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
import io.requery.android.database.sqlite.SQLiteFunction
import org.example.app.sqlite3.Sqlite3CApi
import ru.pixnews.sqlite3.wasm.Sqlite3Errno
import ru.pixnews.sqlite3.wasm.Sqlite3Exception
import ru.pixnews.sqlite3.wasm.Sqlite3OpenFlags
import ru.pixnews.sqlite3.wasm.Sqlite3OpenFlags.Companion.SQLITE_OPEN_READWRITE
import ru.pixnews.sqlite3.wasm.Sqlite3TextEncoding.SQLITE_UTF8
import ru.pixnews.sqlite3.wasm.util.contains
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.WasmPtr.Companion.sqlite3Null
import ru.pixnews.wasm.host.isSqlite3Null
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
value class Graallite3WindowPtr(
    val ptr: WasmPtr<Void>
) : Sqlite3WindowPtr {
    override fun isNull(): Boolean = ptr.isSqlite3Null()
}

class GraalNativeBindings(
    private val sqlite3Api: Sqlite3CApi,
    logger: Logger = Logger
) : SqlOpenHelperNativeBindings<GraalSqlite3ConnectionPtr, GraalSqlite3StatementPtr, Graallite3WindowPtr> {
    private val logger = Logger.withTag("GraalNativeBindings")

    private val localizedComparator = LocalizedComparator()
    private val connections = Sqlite3ConnectionRegistry()

    override fun connectionNullPtr(): GraalSqlite3ConnectionPtr = GraalSqlite3ConnectionPtr(sqlite3Null())

    override fun connectionStatementPtr(): GraalSqlite3StatementPtr = GraalSqlite3StatementPtr(sqlite3Null())

    override fun connectionWindowPtr(): Graallite3WindowPtr = Graallite3WindowPtr(sqlite3Null())

    override fun nativeOpen(
        path: String,
        openFlags: Int,
        label: String,
        enableTrace: Boolean,
        enableProfile: Boolean
    ): GraalSqlite3ConnectionPtr {
        var db: WasmPtr<Sqlite3Db>? = null
        val flags = Sqlite3OpenFlags(openFlags)
        try {
            db = sqlite3Api.sqlite3OpenV2(
                filename = path,
                flags = flags,
                vfsName = null
            )

            // TODO: Why not in nativeRegisterLocalizedCollators()?
            sqlite3Api.sqlite3CreateCollation(db, "localized", SQLITE_UTF8, localizedComparator)

            // Check that the database is really read/write when that is what we asked for.
            if (flags.contains(SQLITE_OPEN_READWRITE)
                && sqlite3Api.sqlite3DbReadonly(db, null) == Sqlite3CApi.Sqlite3DbReadonlyResult.READ_WRITE
            ) {
                throw SQLiteCantOpenDatabaseException("Could not open the database in read/write mode.")
            }

            // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
            sqlite3Api.sqlite3BusyTimeout(db, BUSY_TIMEOUT_MS)

            // Register wrapper object
            connections.add(db, flags, path, label)

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
            rethrowAndroidSqliteException(e)
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

    override fun nativeRegisterFunction(connectionPtr: GraalSqlite3ConnectionPtr, function: SQLiteFunction) {
        TODO("Not yet implemented sqlite3_create_function_v2")
    }

    override fun nativeClose(connectionPtr: GraalSqlite3ConnectionPtr) {
        connections.remove(connectionPtr.ptr)
        try {
            sqlite3Api.sqlite3Close(connectionPtr.ptr)
        } catch (e: Sqlite3Exception) {
            // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
            logger.i { "sqlite3_close(${connectionPtr}) failed: %d" }
            rethrowAndroidSqliteException(e, "Count not close db.")
        }
    }

    override fun nativeLoadExtension(connectionPtr: GraalSqlite3ConnectionPtr, file: String, proc: String) {
        TODO("Not yet implemented")
    }

    override fun nativeResetCancel(connectionPtr: GraalSqlite3ConnectionPtr, cancelable: Boolean) {
        TODO("Not yet implemented")
    }

    override fun nativeCancel(connectionPtr: GraalSqlite3ConnectionPtr) {
        TODO("Not yet implemented")
    }

    override fun nativeGetDbLookaside(connectionPtr: GraalSqlite3ConnectionPtr): Int {
        TODO("Not yet implemented")
    }

    override fun nativeExecuteForCursorWindow(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        winPtr: Graallite3WindowPtr,
        startPos: Int,
        requiredPos: Int,
        countAllRows: Boolean
    ): Long {
        TODO("Not yet implemented")
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

        private fun rethrowAndroidSqliteException(sqliteException: Sqlite3Exception, msg: String? = null): Nothing {
            val errno = Sqlite3Errno.fromErrNoCode(sqliteException.sqliteExtendedErrorCode)
            val fullErMsg = buildString {
                append(errno?.name ?: "UNKNOWN")
                append(" (code ", sqliteException.sqliteExtendedErrorCode, ")")
                val msgs = listOfNotNull(msg, sqliteException.sqliteMsg)
                if (msgs.isNotEmpty()) {
                    msgs.joinTo(
                        buffer = this,
                        separator = ", ",
                        prefix = ": "
                    )
                }
            }

            val androidException = when (Sqlite3Errno.fromErrNoCode(sqliteException.sqliteErrorCode)) {
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