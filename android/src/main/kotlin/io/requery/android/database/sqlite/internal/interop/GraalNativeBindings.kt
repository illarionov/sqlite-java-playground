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
import io.requery.android.database.sqlite.internal.interop.GraalNativeBindings.CopyRowResult.CPR_FULL
import io.requery.android.database.sqlite.internal.interop.GraalNativeBindings.CopyRowResult.CPR_OK
import org.example.app.sqlite3.Sqlite3CApi
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType.Companion.SQLITE3_BLOB
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType.Companion.SQLITE3_FLOAT
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType.Companion.SQLITE3_INTEGER
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType.Companion.SQLITE3_NULL
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType.Companion.SQLITE3_TEXT
import ru.pixnews.sqlite3.wasm.Sqlite3DbStatusParameter.Companion.SQLITE_DBSTATUS_LOOKASIDE_USED
import ru.pixnews.sqlite3.wasm.Sqlite3Errno
import ru.pixnews.sqlite3.wasm.Sqlite3Errno.SQLITE_DONE
import ru.pixnews.sqlite3.wasm.Sqlite3Errno.SQLITE_OK
import ru.pixnews.sqlite3.wasm.Sqlite3Errno.SQLITE_ROW
import ru.pixnews.sqlite3.wasm.Sqlite3ErrorInfo
import ru.pixnews.sqlite3.wasm.Sqlite3Exception
import ru.pixnews.sqlite3.wasm.Sqlite3Exception.Companion.sqlite3ErrNoName
import ru.pixnews.sqlite3.wasm.Sqlite3OpenFlags
import ru.pixnews.sqlite3.wasm.Sqlite3OpenFlags.Companion.SQLITE_OPEN_READWRITE
import ru.pixnews.sqlite3.wasm.Sqlite3TextEncoding.SQLITE_UTF8
import ru.pixnews.sqlite3.wasm.Sqlite3TraceEventCode
import ru.pixnews.sqlite3.wasm.Sqlite3TraceEventCode.Companion.SQLITE_TRACE_CLOSE
import ru.pixnews.sqlite3.wasm.Sqlite3TraceEventCode.Companion.SQLITE_TRACE_PROFILE
import ru.pixnews.sqlite3.wasm.Sqlite3TraceEventCode.Companion.SQLITE_TRACE_ROW
import ru.pixnews.sqlite3.wasm.Sqlite3TraceEventCode.Companion.SQLITE_TRACE_STMT
import ru.pixnews.sqlite3.wasm.util.contains
import ru.pixnews.sqlite3.wasm.util.or
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.WasmPtr.Companion.sqlite3Null
import ru.pixnews.wasm.host.isSqlite3Null
import ru.pixnews.wasm.host.memory.encodedNullTerminatedStringLength
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.sqlite3.Sqlite3Statement
import ru.pixnews.wasm.host.sqlite3.Sqlite3Trace

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
    val ptr: NativeCursorWindow
) : Sqlite3WindowPtr {
    override fun isNull(): Boolean = false
}

class GraalNativeBindings(
    private val sqlite3Api: Sqlite3CApi,
    logger: Logger = Logger
) : SqlOpenHelperNativeBindings<GraalSqlite3ConnectionPtr, GraalSqlite3StatementPtr, GraalSqlite3WindowPtr> {
    private val logger = logger.withTag("GraalNativeBindings")

    private val localizedComparator = LocalizedComparator()
    private val connections = Sqlite3ConnectionRegistry()

    override fun connectionNullPtr(): GraalSqlite3ConnectionPtr = GraalSqlite3ConnectionPtr(sqlite3Null())

    override fun connectionStatementPtr(): GraalSqlite3StatementPtr = GraalSqlite3StatementPtr(sqlite3Null())

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

            sqlite3Api.sqlite3CreateCollation(db, "localized", localizedComparator)

            // Check that the database is really read/write when that is what we asked for.
            if (openFlags.contains(SQLITE_OPEN_READWRITE)
                && sqlite3Api.sqlite3DbReadonly(db, null) != Sqlite3CApi.Sqlite3DbReadonlyResult.READ_WRITE
            ) {
                throw SQLiteCantOpenDatabaseException("Could not open the database in read/write mode.")
            }

            // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
            sqlite3Api.sqlite3BusyTimeout(db, BUSY_TIMEOUT_MS)

            // Register wrapper object
            connections.add(db, path)

            // Enable tracing and profiling if requested.
            if (enableTrace || enableProfile) {
                var mask = Sqlite3TraceEventCode(0)
                if (enableTrace) {
                    mask = mask or SQLITE_TRACE_STMT or SQLITE_TRACE_ROW or SQLITE_TRACE_CLOSE
                }
                if (enableProfile) {
                    mask = mask or SQLITE_TRACE_PROFILE
                }
                sqlite3Api.sqlite3Trace(db, mask, ::sqliteTraceCallback)
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
            val lookasideUsed = sqlite3Api.sqlite3DbStatus(
                connection.dbPtr,
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
        val window: NativeCursorWindow = winPtr.ptr
        val statement = statementPtr.ptr

        val status = window.clear()
        if (status != 0) {
            throwAndroidSqliteException("Failed to clear the cursor window")
        }

        val numColumns = sqlite3Api.sqlite3ColumnCount(statement)
        if (window.setNumColumns(numColumns) != 0) {
            throwAndroidSqliteException("Failed to set the cursor window column count")
        }

        var retryCount = 0
        var totalRows = 0
        var addedRows = 0
        var windowFull = false

        @Suppress("NAME_SHADOWING")
        var startPos = startPos
        try {
            while (!windowFull || countAllRows) {
                val err = sqlite3Api.sqlite3Step(statement)
                when (err) {
                    SQLITE_DONE -> {
                        logger.v { "Processed all rows" }
                        break
                    }

                    SQLITE_ROW -> {
                        logger.v { "Stepped statement $statement to row $totalRows" }
                        retryCount = 0
                        totalRows += 1

                        // Skip the row if the window is full or we haven't reached the start position yet.
                        if (startPos >= totalRows || windowFull) {
                            continue
                        }
                        var cpr = copyRow(window, statement, numColumns, startPos, addedRows)

                        if (cpr == CPR_FULL && addedRows != 0 && startPos + addedRows <= requiredPos) {
                            // We filled the window before we got to the one row that we really wanted.
                            // Clear the window and start filling it again from here.
                            window.clear()
                            window.setNumColumns(numColumns)
                            startPos += addedRows
                            addedRows = 0
                            cpr = copyRow(window, statement, numColumns, startPos, addedRows)
                        }

                        when (cpr) {
                            CPR_OK -> addedRows += 1
                            CPR_FULL -> windowFull = true
                        }
                    }

                    Sqlite3Errno.SQLITE_LOCKED, Sqlite3Errno.SQLITE_BUSY -> {
                        // The table is locked, retry
                        logger.v { "Database locked, retrying" }
                        if (retryCount > 50) {
                            logger.e { "Bailing on database busy retry" }
                            throwAndroidSqliteException(connectionPtr.ptr, "retrycount exceeded")
                        } else {
                            // Sleep to give the thread holding the lock a chance to finish
                            Thread.sleep(1)
                            retryCount++
                        }
                    }

                    else -> throwAndroidSqliteException(connectionPtr.ptr, "sqlite3Step() failed")
                }
            }
        } catch (exception: Sqlite3Exception) {
            exception.rethrowAndroidSqliteException("nativeExecuteForCursorWindow() failed")
        } finally {
            logger.v {
                "Resetting statement $statement after fetching $totalRows rows and adding $addedRows rows" +
                        "to the window in ${window.size - window.freeSpace} bytes"
            }

            val errCode = sqlite3Api.sqlite3Reset(statement) // TODO: check error code, may be SQLITE_BUSY
        }

        // Report the total number of rows on request.
        if (startPos > totalRows) {
            logger.e { "startPos $startPos > actual rows $totalRows" }
        }

        return (startPos.toLong().shr(32)).or(totalRows.toLong())
    }

    override fun nativeExecuteForLastInsertedRowId(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Long {
        executeNonQuery(connectionPtr, statementPtr)

        if (sqlite3Api.sqlite3Changes(connectionPtr.ptr) <= 0) {
            return -1
        }

        return sqlite3Api.sqlite3LastInsertRowId(connectionPtr.ptr)
    }

    override fun nativeExecuteForChangedRowCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Int {
        executeNonQuery(connectionPtr, statementPtr)
        return sqlite3Api.sqlite3Changes(connectionPtr.ptr)
    }

    override fun nativeExecuteForString(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): String? {
        executeOneRowQuery(connectionPtr, statementPtr)

        if (sqlite3Api.sqlite3ColumnCount(statementPtr.ptr) < 1) {
            return null
        }

        return sqlite3Api.sqlite3ColumnText(statementPtr.ptr, 0)
    }

    override fun nativeExecuteForLong(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Long {
        executeOneRowQuery(connectionPtr, statementPtr)
        if (sqlite3Api.sqlite3ColumnCount(statementPtr.ptr) < 1) {
            return -1
        }

        return sqlite3Api.sqlite3ColumnInt64(statementPtr.ptr, 0)
    }

    override fun nativeExecute(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ) {
        executeNonQuery(connectionPtr, statementPtr)
    }

    override fun nativeResetStatementAndClearBindings(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ) {
        val err = sqlite3Api.sqlite3Reset(statementPtr.ptr)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
        if (sqlite3Api.sqlite3ClearBindings(statementPtr.ptr) != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindBlob(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: ByteArray
    ) {
        val err = sqlite3Api.sqlite3BindBlobTransient(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindString(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: String
    ) {
        val err = sqlite3Api.sqlite3BindStringTransient(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindDouble(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: Double
    ) {
        val err = sqlite3Api.sqlite3BindDouble(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindLong(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: Long
    ) {
        val err = sqlite3Api.sqlite3BindLong(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindNull(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int
    ) {
        val err = sqlite3Api.sqlite3BindNull(statementPtr.ptr, index)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeGetColumnName(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int
    ): String? {
        return sqlite3Api.sqlite3ColumnName(statementPtr.ptr, index)
    }

    override fun nativeGetColumnCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Int {
        return sqlite3Api.sqlite3ColumnCount(statementPtr.ptr)
    }

    override fun nativeIsReadOnly(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Boolean {
        return sqlite3Api.sqlite3StmtReadonly(statementPtr.ptr)
    }

    override fun nativeGetParameterCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Int {
        return sqlite3Api.sqlite3BindParameterCount(statementPtr.ptr)
    }

    override fun nativePrepareStatement(
        connectionPtr: GraalSqlite3ConnectionPtr,
        sql: String
    ): GraalSqlite3StatementPtr {
        try {
            val statementPtr = sqlite3Api.sqlite3PrepareV2(connectionPtr.ptr, sql)
            logger.v { "Prepared statement $statementPtr on connection ${connectionPtr.ptr}" }
            return GraalSqlite3StatementPtr(statementPtr)
        } catch (sqliteException: Sqlite3Exception) {
            sqliteException.rethrowAndroidSqliteException(", while compiling: $sql")
        }
    }

    override fun nativeFinalizeStatement(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ) {
        logger.v { "Finalized statement ${statementPtr.ptr} on connection ${connectionPtr.ptr}" }
        // We ignore the result of sqlite3_finalize because it is really telling us about
        // whether any errors occurred while executing the statement.  The statement itself
        // is always finalized regardless.
        try {
            sqlite3Api.sqlite3Finalize(connectionPtr.ptr, statementPtr.ptr)
        } catch (sqliteException: Sqlite3Exception) {
            logger.v(sqliteException) { "sqlite3_finalize(${connectionPtr.ptr}, ${statementPtr.ptr}) failed" }
        }
    }

    private fun executeNonQuery(
        db: GraalSqlite3ConnectionPtr,
        statement: GraalSqlite3StatementPtr,
    ) {
        val err = sqlite3Api.sqlite3Step(statement.ptr)
        when (err) {
            SQLITE_ROW -> throwAndroidSqliteException("Queries can be performed using SQLiteDatabase query or rawQuery methods only.")
            SQLITE_DONE -> {}
            else -> throwAndroidSqliteException(db.ptr)
        }
    }

    private fun executeOneRowQuery(
        database: GraalSqlite3ConnectionPtr,
        statement: GraalSqlite3StatementPtr,
    ) {
        val err = sqlite3Api.sqlite3Step(statement.ptr)
        if (err != SQLITE_ROW) {
            throwAndroidSqliteException(database.ptr)
        }
    }

    private fun sqliteTraceCallback(trace: Sqlite3Trace) {
        when (trace) {
            is Sqlite3Trace.TraceStmt -> logger.v { """${trace.db}: "${trace.unexpandedSql}"""" }
            is Sqlite3Trace.TraceClose -> logger.v { """${trace.db} closed""" }
            is Sqlite3Trace.TraceProfile -> {
                logger.v {
                    String.format(
                        """%s: "%s" took %.3f ms """,
                        trace.db.toString(),
                        sqlite3Api.sqlite3ExpandedSql(trace.statement) ?: trace.statement.toString(),
                        trace.timeMs * 0.000001f,
                    )
                }
            }
            is Sqlite3Trace.TraceRow -> logger.v { """${trace.db} / ${trace.statement}: Received row""" }
        }
    }

    private fun sqliteProgressHandlerCallback(db: WasmPtr<Sqlite3Db>): Int {
        val connection = connections.get(db) ?: run {
            logger.i { "sqliteProgressHandlerCallback(${db.addr}): database not open" }
            return -1
        }
        return if (connection.isCancelled) 1 else 0
    }

    private fun throwAndroidSqliteException(
        db: WasmPtr<Sqlite3Db>,
        message: String? = null
    ): Nothing {
        val errInfo = sqlite3Api.readSqliteErrorInfo(db)
        throwAndroidSqliteException(errInfo, message)
    }

    @Throws(Sqlite3Exception::class)
    private fun copyRow(
        window: NativeCursorWindow,
        statement: WasmPtr<Sqlite3Statement>,
        numColumns: Int,
        startPos: Int,
        addedRows: Int,
    ): CopyRowResult {
        val status = window.allocRow()
        if (status != 0) {
            logger.i { "Failed allocating fieldDir at startPos $status row $addedRows" }
            return CPR_FULL
        }
        var result = CPR_OK
        try {
            for (columnNo in 0 until numColumns) {
                val type = sqlite3Api.sqlite3ColumnType(statement, columnNo)
                when (type) {
                    SQLITE3_TEXT -> {
                        val text = sqlite3Api.sqlite3ColumnText(statement, columnNo) ?: run {
                            throwAndroidSqliteException("Null text at ${startPos + addedRows},${columnNo}")
                        }
                        val putStatus = window.putString(addedRows, columnNo, text)
                        if (putStatus != 0) {
                            logger.v {
                                "Failed allocating ${text.encodedNullTerminatedStringLength()} bytes for text " +
                                        "at ${startPos + addedRows},${columnNo}, error=$putStatus"
                            }
                            result = CPR_FULL
                            break
                        }
                        logger.v {
                            "${startPos + addedRows},${columnNo} is TEXT with " +
                                    "${text.encodedNullTerminatedStringLength()} bytes"
                        }
                    }

                    SQLITE3_INTEGER -> {
                        val value = sqlite3Api.sqlite3ColumnInt64(statement, columnNo)
                        val putStatus = window.putLong(addedRows, columnNo, value)
                        if (putStatus != 0) {
                            logger.v { "Failed allocating space for a long in column $columnNo, error=$putStatus" }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},${columnNo} is INTEGER $value" }
                    }

                    SQLITE3_FLOAT -> {
                        val value = sqlite3Api.sqlite3ColumnDouble(statement, columnNo)
                        val putStatus = window.putDouble(addedRows, columnNo, value)
                        if (putStatus != 0) {
                            logger.v { "Failed allocating space for a double in column $columnNo, error=$putStatus" }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},${columnNo} is FLOAT $value" }
                    }

                    SQLITE3_BLOB -> {
                        val value = sqlite3Api.sqlite3ColumnBlob(statement, columnNo)
                        val putStatus = window.putBlob(addedRows, columnNo, value)
                        if (putStatus != 0) {
                            logger.v {
                                "Failed allocating ${value.size} bytes for blob at " +
                                        "${startPos + addedRows},$columnNo, error=${putStatus}"
                            }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},$columnNo is Blob with ${value.size} bytes" }
                    }

                    SQLITE3_NULL -> {
                        val putStatus = window.putNull(addedRows, columnNo)
                        if (putStatus != 0) {
                            logger.v {
                                "Failed allocating space for a null in column ${columnNo}, error=${putStatus}" +
                                        "${startPos + addedRows},$columnNo, error=${putStatus}"
                            }
                            result = CPR_FULL
                            break
                        }
                    }

                    else -> {
                        logger.e { "Unknown column type when filling database window" }
                        throwAndroidSqliteException("Unknown column type when filling window")
                    }
                }
            }
        } catch (e: Throwable) {
            window.freeLastRow()
            throw e
        }

        if (result != CPR_OK) {
            window.freeLastRow()
        }

        return result
    }

    class Sqlite3Connection(
        val dbPtr: WasmPtr<Sqlite3Db>,
        val path: String,
        var isCancelled: Boolean = false
    )

    private class Sqlite3ConnectionRegistry {
        private val map: MutableMap<WasmPtr<Sqlite3Db>, Sqlite3Connection> = mutableMapOf()

        fun add(
            dbPtr: WasmPtr<Sqlite3Db>,
            path: String,
        ): Sqlite3Connection {
            val connection = Sqlite3Connection(dbPtr, path, false)
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
        const val BUSY_TIMEOUT_MS = 2500

        private fun Sqlite3Exception.rethrowAndroidSqliteException(msg: String? = null): Nothing {
            throwAndroidSqliteException(errorInfo, msg)
        }

        private fun throwAndroidSqliteException(message: String?): Nothing = throwAndroidSqliteException(
            Sqlite3ErrorInfo(0, 0, null),
            message,
        )

        private fun throwAndroidSqliteException(
            errorInfo: Sqlite3ErrorInfo,
            message: String?
        ): Nothing {
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
                SQLITE_DONE -> SQLiteDoneException(fullErMsg)
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