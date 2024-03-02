package io.requery.android.database.sqlite.internal.interop

import android.database.sqlite.SQLiteAbortException
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException
import android.database.sqlite.SQLiteBlobTooBigException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteDatatypeMismatchException
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteDoneException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.database.sqlite.SQLiteMisuseException
import android.database.sqlite.SQLiteOutOfMemoryException
import android.database.sqlite.SQLiteReadOnlyDatabaseException
import android.database.sqlite.SQLiteTableLockedException
import androidx.core.os.OperationCanceledException
import co.touchlab.kermit.Logger
import io.requery.android.database.sqlite.base.CursorWindow
import org.example.app.sqlite3.Sqlite3CApi
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType.Companion.SQLITE3_BLOB
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType.Companion.SQLITE3_FLOAT
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType.Companion.SQLITE3_INTEGER
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType.Companion.SQLITE3_NULL
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType.Companion.SQLITE3_TEXT
import ru.pixnews.sqlite3.wasm.Sqlite3DbStatusParameter.Companion.SQLITE_DBSTATUS_LOOKASIDE_USED
import ru.pixnews.sqlite3.wasm.Sqlite3Errno
import ru.pixnews.sqlite3.wasm.Sqlite3ErrorInfo
import ru.pixnews.sqlite3.wasm.Sqlite3Exception
import ru.pixnews.sqlite3.wasm.Sqlite3Exception.Companion.sqlite3ErrNoName
import ru.pixnews.sqlite3.wasm.Sqlite3OpenFlags
import ru.pixnews.sqlite3.wasm.Sqlite3OpenFlags.Companion.SQLITE_OPEN_READWRITE
import ru.pixnews.sqlite3.wasm.Sqlite3TextEncoding.SQLITE_UTF8
import ru.pixnews.sqlite3.wasm.util.contains
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.WasmPtr.Companion.sqlite3Null
import ru.pixnews.wasm.host.isSqlite3Null
import ru.pixnews.wasm.host.memory.encodedNullTerminatedStringLength
import ru.pixnews.wasm.host.memory.encodedStringLength
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.sqlite3.Sqlite3Statement

@JvmInline
value class GraalSqlite3ConnectionPtr(
    val ptr: WasmPtr<Sqlite3Db>
) : Sqlite3ConnectionPtr {
    override fun isNull(): Boolean = ptr.isSqlite3Null()
}

@JvmInline
value class GraalSqlite3StatementPtr(
    val ptr: WasmPtr<Sqlite3Statement>
) : Sqlite3StatementPtr {
    override fun isNull(): Boolean = ptr.isSqlite3Null()
}

@JvmInline
value class GraalSqlite3WindowPtr(
    val ptr: NativeCursorWindow?
) : Sqlite3WindowPtr {
    override fun isNull(): Boolean = ptr == null
}

class GraalNativeBindings(
    private val sqlite3Api: Sqlite3CApi,
    logger: Logger = Logger
) : SqlOpenHelperNativeBindings<GraalSqlite3ConnectionPtr, GraalSqlite3StatementPtr, GraalSqlite3WindowPtr> {
    private val logger = Logger.withTag("GraalNativeBindings")

    private val localizedComparator = LocalizedComparator()
    private val connections = Sqlite3ConnectionRegistry()

    override fun connectionNullPtr(): GraalSqlite3ConnectionPtr = GraalSqlite3ConnectionPtr(sqlite3Null())

    override fun connectionStatementPtr(): GraalSqlite3StatementPtr = GraalSqlite3StatementPtr(sqlite3Null())

    override fun connectionWindowPtr(): GraalSqlite3WindowPtr = GraalSqlite3WindowPtr(null)

    override fun nativeOpen(
        path: String,
        openFlags: Sqlite3OpenFlags,
        label: String,
        enableTrace: Boolean,
        enableProfile: Boolean
    ): GraalSqlite3ConnectionPtr {
        var db: WasmPtr<Sqlite3Db>? = null
        try {
            db = sqlite3Api.sqlite3OpenV2(
                filename = path,
                flags = openFlags,
                vfsName = null
            )

            // TODO: Why not in nativeRegisterLocalizedCollators()?
            sqlite3Api.sqlite3CreateCollation(db, "localized", SQLITE_UTF8, localizedComparator)

            // Check that the database is really read/write when that is what we asked for.
            if (openFlags.contains(SQLITE_OPEN_READWRITE)
                && sqlite3Api.sqlite3DbReadonly(db, null) == Sqlite3CApi.Sqlite3DbReadonlyResult.READ_WRITE
            ) {
                throw SQLiteCantOpenDatabaseException("Could not open the database in read/write mode.")
            }

            // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
            sqlite3Api.sqlite3BusyTimeout(db, BUSY_TIMEOUT_MS)

            // Register wrapper object
            connections.add(db, openFlags, path, label)

            // Enable tracing and profiling if requested.
            if (enableTrace) {
                sqlite3Api.sqlite3Trace(db, ::sqliteTraceCallback)
            }
            if (enableProfile) {
                sqlite3Api.sqlite3Profile(db, ::sqliteProfileCallback)
            }
            return GraalSqlite3ConnectionPtr(db)
        } catch (e: Sqlite3Exception) {
            // TODO: unregister collation / trace callback / profile callback on close?
            db?.let {
                sqlite3Api.sqlite3Close(it)
            }
            e.rethrowAndroidSqliteException()
        } catch (otherException: RuntimeException) {
            db?.let {
                sqlite3Api.sqlite3Close(it)
            }
            throw otherException
        }
    }

    override fun nativeRegisterLocalizedCollators(connectionPtr: GraalSqlite3ConnectionPtr, locale: String) {
        // empty
    }

    override fun nativeClose(connectionPtr: GraalSqlite3ConnectionPtr) {
        connections.remove(connectionPtr.ptr)
        try {
            sqlite3Api.sqlite3Close(connectionPtr.ptr)
        } catch (e: Sqlite3Exception) {
            // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
            logger.i { "sqlite3_close(${connectionPtr.ptr}) failed: ${e.sqlite3ErrNoName}" }
            e.rethrowAndroidSqliteException("Count not close db.")
        }
    }

    override fun nativeResetCancel(connectionPtr: GraalSqlite3ConnectionPtr, cancelable: Boolean) {
        val connection = connections.get(connectionPtr.ptr) ?: run {
            logger.i { "nativeResetCancel(${connectionPtr.ptr}): connection not open" }
            return
        }
        connection.isCancelled = false
        try {
            if (cancelable) {
                sqlite3Api.sqlite3ProgressHandler(connectionPtr.ptr, 4, ::sqliteProgressHandlerCallback)
            } else {
                sqlite3Api.sqlite3ProgressHandler(connectionPtr.ptr, 0, null)
            }
        } catch (e: Sqlite3Exception) {
            logger.i { "nativeResetCancel(${connectionPtr.ptr}) failed: ${e.sqlite3ErrNoName}" }
            e.rethrowAndroidSqliteException("Count not close db.")
        }
    }

    override fun nativeCancel(connectionPtr: GraalSqlite3ConnectionPtr) {
        val connection = connections.get(connectionPtr.ptr) ?: run {
            logger.i { "nativeResetCancel(${connectionPtr.ptr}): connection not open" }
            return
        }
        connection.isCancelled = true
    }

    override fun nativeGetDbLookaside(connectionPtr: GraalSqlite3ConnectionPtr): Int {
        val connection = connections.get(connectionPtr.ptr) ?: run {
            logger.i { "nativeGetDbLookaside(${connectionPtr.ptr}): connection not open" }
            return -1
        }
        try {
            val lookasideUsed = sqlite3Api.sqlite3DbStatus(connection.dbPtr,
                SQLITE_DBSTATUS_LOOKASIDE_USED,
                false
            )
            return lookasideUsed.current
        } catch (e: Sqlite3Exception) {
            logger.i { "nativeGetDbLookaside(${connectionPtr.ptr}) failed: ${e.sqlite3ErrNoName}" }
            return -1
        }
    }

    override fun nativeExecuteForCursorWindow(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        winPtr: GraalSqlite3WindowPtr,
        startPos: Int,
        requiredPos: Int,
        countAllRows: Boolean
    ): Long {
        val connection = connections.get(connectionPtr.ptr) ?: run {
            logger.i { "nativeExecuteForCursorWindow(${connectionPtr.ptr}): connection not open" }
            return -1
        }
        val window: NativeCursorWindow = winPtr.ptr ?: throw NullPointerException("NativeCursorWindow is null")
        val statement = statementPtr.ptr

        val status = window.clear()
        if (status != 0) {
            throwSqliteException("Failed to clear the cursor window")
        }

        val numColumns = sqlite3Api.sqlite3ColumnCount(statement)
        if (window.setNumColumns(numColumns) != 0) {
            throwSqliteException("Failed to set the cursor window column count")
        }

        var retryCount = 0
        var totalRows = 0
        var addedRows = 0
        var windowFull = false
        try {
            while (!windowFull || countAllRows) {
                val err = sqlite3Api.sqlite3Step(statement)
                when (err) {
                    Sqlite3Errno.SQLITE_DONE -> {
                        logger.v { "Processed all rows" }
                        break
                    }
                    Sqlite3Errno.SQLITE_ROW -> {
                        logger.v { "Stepped statement $statement to row $totalRows" }
                        retryCount = 0;
                        totalRows += 1;

                        // Skip the row if the window is full or we haven't reached the start position yet.
                        if (startPos >= totalRows || windowFull) {
                            continue;
                        }
                        val cpr = copyRow(window, statement, numColumns, startPos, addedRows)

                    }
                    Sqlite3Errno.SQLITE_LOCKED, Sqlite3Errno.SQLITE_BUSY -> {
                        // The table is locked, retry
                        logger.v { "Database locked, retrying" }
                        if (retryCount > 50) {
                            logger.e { "Bailing on database busy retry" }
                            throwSqliteException(connection.dbPtr, "retrycount exceeded")
                        } else {
                            // Sleep to give the thread holding the lock a chance to finish
                            Thread.sleep(1)
                            retryCount++;
                        }
                    }
                    else -> throwSqliteException(connection.dbPtr, "sqlite3Step() failed")
                }
            }
        } catch (exception: Sqlite3Exception) {
            exception.rethrowAndroidSqliteException("nativeExecuteForCursorWindow() failed")
        } finally {
            logger.v {
                "Resetting statement ${statement} after fetching $totalRows rows and adding $addedRows rows" +
                "to the window in ${window.size - window.freeSpace} bytes"
            }

            val errCode = sqlite3Api.sqlite3Reset(statement) // TODO: check error code, may be SQLITE_BUSY
        }

        // Report the total number of rows on request.
        if (startPos > totalRows) {
            logger.e { "startPos $startPos > actual rows $totalRows" }
        }

        return (startPos.toLong().shr(32)).or(totalRows.toLong());

/*

        while (!gotException && (!windowFull || countAllRows)) {
            if (err == SQLITE_ROW) {

                CopyRowResult cpr = copyRow(env, window, statement, numColumns, startPos, addedRows);
                if (cpr == CPR_FULL && addedRows && startPos + addedRows <= requiredPos) {
                    // We filled the window before we got to the one row that we really wanted.
                    // Clear the window and start filling it again from here.
                    // TODO: Would be nicer if we could progressively replace earlier rows.
                        window->clear();
                    window->setNumColumns(numColumns);
                    startPos += addedRows;
                    addedRows = 0;
                    cpr = copyRow(env, window, statement, numColumns, startPos, addedRows);
                }

                if (cpr == CPR_OK) {
                    addedRows += 1;
                } else if (cpr == CPR_FULL) {
                    windowFull = true;
                } else {
                    gotException = true;
                }
                }
        }

        LOG_WINDOW("Resetting statement %p after fetching %d rows and adding %d rows"
            "to the window in %d bytes",
            statement, totalRows, addedRows, window->size() - window->freeSpace());
        sqlite3_reset(statement);

        // Report the total number of rows on request.
        if (startPos > totalRows) {
            ALOGE("startPos %d > actual rows %d", startPos, totalRows);
        }
        jlong result = jlong(startPos) << 32 | jlong(totalRows);
        return result;*/
    }



    override fun nativeExecuteForLastInsertedRowId(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Long {
        TODO("Not yet implemented")
    }

    override fun nativeExecuteForChangedRowCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Int {
        TODO("Not yet implemented")
    }

    override fun nativeExecuteForBlobFileDescriptor(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Int {
        TODO("Not yet implemented")
    }

    override fun nativeExecuteForString(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): String? {
        TODO("Not yet implemented")
    }

    override fun nativeExecuteForLong(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Long {
        TODO("Not yet implemented")
    }

    override fun nativeExecute(connectionPtr: GraalSqlite3ConnectionPtr, statementPtr: GraalSqlite3StatementPtr) {
        TODO("Not yet implemented")
    }

    override fun nativeResetStatementAndClearBindings(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeBindBlob(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: ByteArray
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeBindString(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: String
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeBindDouble(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: Double
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeBindLong(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: Long
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeBindNull(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeGetColumnName(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int
    ): String? {
        TODO("Not yet implemented")
    }

    override fun nativeGetColumnCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Int {
        TODO("Not yet implemented")
    }

    override fun nativeIsReadOnly(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativeGetParameterCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Int {
        TODO("Not yet implemented")
    }

    override fun nativeFinalizeStatement(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ) {
        TODO("Not yet implemented")
    }

    override fun nativePrepareStatement(
        connectionPtr: GraalSqlite3ConnectionPtr,
        sql: String
    ): GraalSqlite3StatementPtr {
        TODO("Not yet implemented")
    }

    private fun sqliteTraceCallback(db: WasmPtr<Sqlite3Db>, statement: String) {
        val connection = connections.get(db)
        logger.v { """${connection?.label ?: db.toString()}: "$statement"""" }
    }

    private fun sqliteProfileCallback(db: WasmPtr<Sqlite3Db>, statement: String, time: Long): Unit {
        val connection = connections.get(db)
        logger.v {
            String.format(
                """%s: "%s" took %0.3f ms """,
                connection?.label ?: db.toString(),
                statement,
                time * 0.000001f,
            )
        }
    }

    private fun sqliteProgressHandlerCallback(db: WasmPtr<Sqlite3Db>): Int {
        val connection = connections.get(db) ?: run {
            logger.i { "sqliteProgressHandlerCallback(${db.addr}): database not open" }
            return -1
        }
        return if (connection.isCancelled) 1 else 0
    }

    private fun throwSqliteException(
        db: WasmPtr<Sqlite3Db>,
        message: String?
    ): Nothing {
        val errInfo = sqlite3Api.readSqliteErrorInfo(db)
        throwSqliteException(errInfo, message)
    }

    private fun copyRow(
        window: NativeCursorWindow,
        statement: WasmPtr<Sqlite3Statement>,
        numColumns: Int,
        startPos: Int,
        addedRows: Int,
    ) : CopyRowResult {
        val status = window.allocRow()
        if (status != 0) {
            logger.i { "Failed allocating fieldDir at startPos $status row $addedRows" }
            return CopyRowResult.CPR_FULL
        }
        var result = CopyRowResult.CPR_OK
        for (columnNo in 0 until numColumns) {
            val type = sqlite3Api.sqlite3ColumnType(statement, columnNo)
            when (type) {
                SQLITE3_TEXT -> {
                    val text = sqlite3Api.sqlite3ColumnText(statement, columnNo)
                    val putStatus = window.putString(addedRows, columnNo, text)
                    if (putStatus != 0) {
                        logger.v {
                            "Failed allocating ${text.encodedNullTerminatedStringLength()} bytes for text " +
                                "at ${startPos + addedRows},${columnNo}, error=$putStatus"
                        }
                        result = CopyRowResult.CPR_FULL;
                        break;
                    }
                    logger.v { "${startPos + addedRows},${columnNo} is TEXT with " +
                            "${text.encodedNullTerminatedStringLength()} bytes" }
                }
                SQLITE3_INTEGER -> {
                    val value = sqlite3Api.sqlite3ColumnInt64(statement, columnNo)
                    val putStatus = window.putLong(addedRows, columnNo, value)
                    if (putStatus != 0) {
                        logger.v { "Failed allocating space for a long in column $columnNo, error=$putStatus" }
                        result = CopyRowResult.CPR_FULL;
                        break;
                    }
                    logger.v { "${startPos + addedRows},${columnNo} is INTEGER $value" }
                }
                SQLITE3_FLOAT -> {
                    val value = sqlite3Api.sqlite3ColumnDouble(statement, columnNo)
                    val putStatus = window.putDouble(addedRows, columnNo, value)
                    if (putStatus != 0) {
                        logger.v { "Failed allocating space for a double in column $columnNo, error=$putStatus" }
                        result = CopyRowResult.CPR_FULL;
                        break;
                    }
                    logger.v { "${startPos + addedRows},${columnNo} is FLOAT $value" }
                }
                SQLITE3_BLOB -> {
                    val value = sqlite3Api.sqlite3ColumnBlob(statement, columnNo)
                    val putStatus = window.putBlob(addedRows, columnNo, value)
                    if (putStatus != 0) {
                        logger.v { "Failed allocating ${value.size} bytes for blob at " +
                                "${startPos + addedRows},$columnNo, error=${putStatus}" }
                        result = CopyRowResult.CPR_FULL;
                        break;
                    }
                    logger.v { "${startPos + addedRows},$columnNo is Blob with ${value.size} bytes" }
                }
                SQLITE3_NULL -> {
                    val putStatus = window.putNull(addedRows, columnNo)
                    if (putStatus != 0) {
                        logger.v { "Failed allocating space for a null in column ${columnNo}, error=${putStatus}" +
                                "${startPos + addedRows},$columnNo, error=${putStatus}" }
                        result = CopyRowResult.CPR_FULL;
                        break;
                    }
                }
                else -> {
                    logger.e { "Unknown column type when filling database window" }
                    throwSqliteException("Unknown column type when filling window")
                }
            }
        }

        if (result != CopyRowResult.CPR_OK) {
            window.freeLastRow()
        }
        return result
    }

//    static CopyRowResult copyRow(JNIEnv* env, CursorWindow* window,
//    sqlite3_stmt* statement, int numColumns, int startPos, int addedRows) {
//        // Allocate a new field directory for the row.
//        status_t status = window->allocRow();
//        if (status) {
//            LOG_WINDOW("Failed allocating fieldDir at startPos %d row %d, error=%d",
//                startPos, addedRows, status);
//            return CPR_FULL;
//        }
//
//        // Pack the row into the window.
//        CopyRowResult result = CPR_OK;
//        for (int i = 0; i < numColumns; i++) {
//            int type = sqlite3_column_type(statement, i);
//            if (type == SQLITE_TEXT) {
//            } else if (type == SQLITE_INTEGER) {
//                // INTEGER data

//            } else if (type == SQLITE_FLOAT) {
//                // FLOAT data
//                double value = sqlite3_column_double(statement, i);
//                status = window->putDouble(addedRows, i, value);
//                if (status) {
//                    LOG_WINDOW("Failed allocating space for a double in column %d, error=%d",
//                        i, status);
//                    result = CPR_FULL;
//                    break;
//                }
//                LOG_WINDOW("%d,%d is FLOAT %lf", startPos + addedRows, i, value);
//            } else if (type == SQLITE_BLOB) {
//                // BLOB data
//                const void* blob = sqlite3_column_blob(statement, i);
//                size_t size = sqlite3_column_bytes(statement, i);
//                status = window->putBlob(addedRows, i, blob, size);
//                if (status) {
//                    LOG_WINDOW("Failed allocating %u bytes for blob at %d,%d, error=%d",
//                        size, startPos + addedRows, i, status);
//                    result = CPR_FULL;
//                    break;
//                }
//                LOG_WINDOW("%d,%d is Blob with %u bytes",
//                    startPos + addedRows, i, size);
//            } else if (type == SQLITE_NULL) {
//                // NULL field
//                status = window->putNull(addedRows, i);
//                if (status) {
//                    LOG_WINDOW("Failed allocating space for a null in column %d, error=%d",
//                        i, status);
//                    result = CPR_FULL;
//                    break;
//                }
//
//                LOG_WINDOW("%d,%d is NULL", startPos + addedRows, i);
//            } else {
//                // Unknown data
//                ALOGE("Unknown column type when filling database window");
//                throw_sqlite3_exception(env, "Unknown column type when filling window");
//                result = CPR_ERROR;
//                break;
//            }
//        }
//
//        // Free the last row if if was not successfully copied.
//        if (result != CPR_OK) {
//                window->freeLastRow();
//        }
//        return result;
//    }


    class Sqlite3Connection(
        val dbPtr: WasmPtr<Sqlite3Db>,
        val openFlags: Sqlite3OpenFlags,
        val path: String,
        val label: String,
        var isCancelled: Boolean = false
    )

    private class Sqlite3ConnectionRegistry {
        private val map: MutableMap<WasmPtr<Sqlite3Db>, Sqlite3Connection> = mutableMapOf()

        fun add(
            dbPtr: WasmPtr<Sqlite3Db>,
            openFlags: Sqlite3OpenFlags,
            path: String,
            label: String,
        ): Sqlite3Connection {
            val connection = Sqlite3Connection(dbPtr, openFlags, path, label, false)
            val old = map.put(dbPtr, connection)
            check(old == null) { "Connection $dbPtr already registered" }
            return connection
        }

        fun get(ptr: WasmPtr<Sqlite3Db>): Sqlite3Connection? = map[ptr]

        fun remove(ptr: WasmPtr<Sqlite3Db>): Sqlite3Connection? = map.remove(ptr)

    }
    private enum class CopyRowResult {
        CPR_OK,
        CPR_FULL,
        CPR_ERROR,
    }

    companion object {
        /* Busy timeout in milliseconds.
        If another connection (possibly in another process) has the database locked for
        longer than this amount of time then SQLite will generate a SQLITE_BUSY error.
        The SQLITE_BUSY error is then raised as a SQLiteDatabaseLockedException.

        In ordinary usage, busy timeouts are quite rare.  Most databases only ever
        have a single open connection at a time unless they are using WAL.  When using
        WAL, a timeout could occur if one connection is busy performing an auto-checkpoint
        operation.  The busy timeout needs to be long enough to tolerate slow I/O write
        operations but not so long as to cause the application to hang indefinitely if
        there is a problem acquiring a database lock. */
        const val BUSY_TIMEOUT_MS = 2500;

        private fun Sqlite3Exception.rethrowAndroidSqliteException(msg: String? = null): Nothing {
            throwSqliteException(this.errorInfo, msg)
        }

        private fun throwSqliteException(message: String?): Nothing = throwSqliteException(
            Sqlite3ErrorInfo(0, 0, null),
            message,
        )

        private fun throwSqliteException(
            errorInfo: Sqlite3ErrorInfo,
            message: String?
        ) : Nothing {
            val extendedErrNo = Sqlite3Errno.fromErrNoCode(errorInfo.sqliteErrorCode)
            val fullErMsg = if (errorInfo.sqliteMsg != null) {
                buildString {
                    append(extendedErrNo?.name ?: "UNKNOWN")
                    append(" (code ", errorInfo.sqliteExtendedErrorCode, ")")
                    val msgs = listOfNotNull(message, errorInfo.sqliteMsg)
                    if (msgs.isNotEmpty()) {
                        msgs.joinTo(
                            buffer = this,
                            separator = ", ",
                            prefix = ": "
                        )
                    }
                }
            } else {
                message
            }

            val androidException = when (Sqlite3Errno.fromErrNoCode(errorInfo.sqliteErrorCode)) {
                Sqlite3Errno.SQLITE_IOERR -> SQLiteDiskIOException(fullErMsg)
                Sqlite3Errno.SQLITE_CORRUPT, Sqlite3Errno.SQLITE_NOTADB -> SQLiteDatabaseCorruptException(fullErMsg)
                Sqlite3Errno.SQLITE_CONSTRAINT -> SQLiteConstraintException(fullErMsg)
                Sqlite3Errno.SQLITE_ABORT -> SQLiteAbortException(fullErMsg)
                Sqlite3Errno.SQLITE_DONE -> SQLiteDoneException(fullErMsg)
                Sqlite3Errno.SQLITE_FULL -> SQLiteFullException(fullErMsg)
                Sqlite3Errno.SQLITE_MISUSE -> SQLiteMisuseException(fullErMsg)
                Sqlite3Errno.SQLITE_PERM -> SQLiteAccessPermException(fullErMsg)
                Sqlite3Errno.SQLITE_BUSY -> SQLiteDatabaseLockedException(fullErMsg)
                Sqlite3Errno.SQLITE_LOCKED -> SQLiteTableLockedException(fullErMsg)
                Sqlite3Errno.SQLITE_READONLY -> SQLiteReadOnlyDatabaseException(fullErMsg)
                Sqlite3Errno.SQLITE_CANTOPEN -> SQLiteCantOpenDatabaseException(fullErMsg)
                Sqlite3Errno.SQLITE_TOOBIG -> SQLiteBlobTooBigException(fullErMsg)
                Sqlite3Errno.SQLITE_RANGE -> SQLiteBindOrColumnIndexOutOfRangeException(fullErMsg)
                Sqlite3Errno.SQLITE_NOMEM -> SQLiteOutOfMemoryException(fullErMsg)
                Sqlite3Errno.SQLITE_MISMATCH -> SQLiteDatatypeMismatchException(fullErMsg)
                Sqlite3Errno.SQLITE_INTERRUPT -> OperationCanceledException(fullErMsg)
                else -> SQLiteException(fullErMsg)
            }
            throw androidException
        }
    }
}