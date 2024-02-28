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
import io.requery.android.database.sqlite.SQLiteFunction
import org.example.app.sqlite3.Sqlite3CApi
import ru.pixnews.sqlite3.wasm.Sqlite3Errno
import ru.pixnews.sqlite3.wasm.Sqlite3Exception
import ru.pixnews.sqlite3.wasm.Sqlite3OpenFlags
import ru.pixnews.sqlite3.wasm.Sqlite3TextEncodings
import ru.pixnews.wasm.host.WasmPtr
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
) : SqlOpenHelperNativeBindings<GraalSqlite3ConnectionPtr, GraalSqlite3StatementPtr, Graallite3WindowPtr> {
    override fun <T : Sqlite3Ptr> nullPtr(): T {
        TODO("Not yet implemented")
    }

    override fun nativeOpen(
        path: String,
        openFlags: Int,
        label: String,
        enableTrace: Boolean,
        enableProfile: Boolean
    ): GraalSqlite3ConnectionPtr {
        try {
            val db = sqlite3Api.sqlite3OpenV2(
                filename = path,
                flags = Sqlite3OpenFlags(openFlags),
                vfsName = null
            )
            sqlite3Api.createCollation(db, "localized", Sqlite3TextEncodings.SQLITE_UTF8, localizedCOmparator)


        } catch (e: Sqlite3Exception) {
            rethrowAndroidSqliteException(e)
            // TODO
        }

/*        err = sqlite3_create_collation(db, "localized", SQLITE_UTF8, 0, coll_localized);
        if (err != SQLITE_OK) {
                env->ReleaseStringUTFChars(pathStr, pathChars);
            env->ReleaseStringUTFChars(labelStr, labelChars);
            throw_sqlite3_exception_errcode(env, err, "Could not register collation");
            sqlite3_close(db);
            return 0;
        }

        // Check that the database is really read/write when that is what we asked for.
        if ((openFlags & SQLITE_OPEN_READWRITE) && sqlite3_db_readonly(db, NULL)) {
                env->ReleaseStringUTFChars(pathStr, pathChars);
            env->ReleaseStringUTFChars(labelStr, labelChars);
            throw_sqlite3_exception(env, db, "Could not open the database in read/write mode.");
            sqlite3_close(db);
            return 0;
        }

        // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
        err = sqlite3_busy_timeout(db, BUSY_TIMEOUT_MS);
        if (err != SQLITE_OK) {
                env->ReleaseStringUTFChars(pathStr, pathChars);
            env->ReleaseStringUTFChars(labelStr, labelChars);
            throw_sqlite3_exception(env, db, "Could not set busy timeout");
            sqlite3_close(db);
            return 0;
        }

        // Register custom Android functions.
        #if 0
        err = register_android_functions(db, UTF16_STORAGE);
        if (err) {
                env->ReleaseStringUTFChars(pathStr, pathChars);
            env->ReleaseStringUTFChars(labelStr, labelChars);
            throw_sqlite3_exception(env, db, "Could not register Android SQL functions.");
            sqlite3_close(db);
            return 0;
        }
        #endif

        // Create wrapper object.
        SQLiteConnection* connection = new SQLiteConnection(db, openFlags, pathChars, labelChars);
        ALOGV("Opened connection %p with label '%s'", db, labelChars);
        env->ReleaseStringUTFChars(pathStr, pathChars);
        env->ReleaseStringUTFChars(labelStr, labelChars);

        // Enable tracing and profiling if requested.
        if (enableTrace) {
            sqlite3_trace(db, &sqliteTraceCallback, connection);
        }
        if (enableProfile) {
            sqlite3_profile(db, &sqliteProfileCallback, connection);
        }

        return reinterpret_cast<jlong>(connection);*/
    }

    override fun nativeRegisterLocalizedCollators(connectionPtr: GraalSqlite3ConnectionPtr, locale: String) {
        TODO("Not yet implemented")
    }

    override fun nativeRegisterFunction(connectionPtr: GraalSqlite3ConnectionPtr, function: SQLiteFunction) {
        TODO("Not yet implemented")
    }

    override fun nativeClose(connectionPtr: GraalSqlite3ConnectionPtr) {
        TODO("Not yet implemented")
    }

    override fun nativeHasCodec(): Boolean {

        TODO("Not yet implemented")
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

    companion object {
        private fun rethrowAndroidSqliteException(sqliteException: Sqlite3Exception, msg: String? = null) {
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