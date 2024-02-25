package io.requery.android.database.sqlite

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteException
import android.os.Looper
import android.util.Log
import android.util.Printer
import androidx.collection.LruCache
import androidx.core.os.CancellationSignal
import io.requery.android.database.CursorWindow
import io.requery.android.database.sqlite.SQLiteDatabase.Companion.ENABLE_WRITE_AHEAD_LOGGING
import io.requery.android.database.sqlite.SQLiteDebug.DEBUG_SQL_STATEMENTS
import io.requery.android.database.sqlite.SQLiteDebug.DEBUG_SQL_TIME
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

/**
 * Represents a SQLite database connection.
 * Each connection wraps an instance of a native `sqlite3` object.
 *
 * When database connection pooling is enabled, there can be multiple active
 * connections to the same database.  Otherwise there is typically only one
 * connection per database.
 *
 * When the SQLite WAL feature is enabled, multiple readers and one writer
 * can concurrently access the database.  Without WAL, readers and writers
 * are mutually exclusive.
 *
 * <h2>Ownership and concurrency guarantees</h2>
 *
 * Connection objects are not thread-safe.  They are acquired as needed to
 * perform a database operation and are then returned to the pool.  At any
 * given time, a connection is either owned and used by a [SQLiteSession]
 * object or the [SQLiteConnectionPool].  Those classes are
 * responsible for serializing operations to guard against concurrent
 * use of a connection.
 *
 * The guarantee of having a single owner allows this class to be implemented
 * without locks and greatly simplifies resource management.
 *
 * <h2>Encapsulation guarantees</h2>
 *
 * The connection object owns *all* of the SQLite related native
 * objects that are associated with the connection.  What's more, there are
 * no other objects in the system that are capable of obtaining handles to
 * those native objects.  Consequently, when the connection is closed, we do
 * not have to worry about what other components might have references to
 * its associated SQLite state -- there are none.
 *
 * Encapsulation is what ensures that the connection object's
 * lifecycle does not become a tortured mess of finalizers and reference
 * queues.
 *
 * <h2>Reentrance</h2>
 *
 * This class must tolerate reentrant execution of SQLite operations because
 * triggers may call custom SQLite functions that perform additional queries.
 *
 */
class SQLiteConnection private constructor(
    private val pool: SQLiteConnectionPool,
    configuration: SQLiteDatabaseConfiguration,
    private val connectionId: Int,
    internal val isPrimaryConnection: Boolean,
) : CancellationSignal.OnCancelListener {
    private val closeGuard: CloseGuard = CloseGuard.get()

    private val configuration = SQLiteDatabaseConfiguration(configuration)

    private val isReadOnlyConnection = configuration.openFlags and SQLiteDatabase.OPEN_READONLY != 0
    private val preparedStatementCache = PreparedStatementCache(this.configuration.maxSqlCacheSize)
    private var preparedStatementPool: PreparedStatement? = null

    // The recent operations log.
    private val recentOperations = OperationLog()

    // The native SQLiteConnection pointer.  (FOR INTERNAL USE ONLY)
    private var connectionPtr: Long = 0

    private var onlyAllowReadOnlyOperations = false

    // The number of times attachCancellationSignal has been called.
    // Because SQLite statement execution can be reentrant, we keep track of how many
    // times we have attempted to attach a cancellation signal to the connection so that
    // we can ensure that we detach the signal at the right time.
    private var cancellationSignalAttachCount = 0

    init {
        closeGuard.open("close")
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        if (connectionPtr != 0L) {
            pool.onConnectionLeaked()
        }

        dispose(true)
    }

    // Called by SQLiteConnectionPool only.
    // Closes the database closes and releases all of its associated resources.
    // Do not call methods on the connection after it is closed.  It will probably crash.
    fun close() {
        dispose(false)
    }

    private fun open() {
        connectionPtr = nativeOpen(
            configuration.path,  // remove the wal flag as its a custom flag not supported by sqlite3_open_v2
            configuration.openFlags and ENABLE_WRITE_AHEAD_LOGGING.inv(),
            configuration.label,
            DEBUG_SQL_STATEMENTS, DEBUG_SQL_TIME
        )

        setPageSize()
        setForeignKeyModeFromConfiguration()
        setJournalSizeLimit()
        setAutoCheckpointInterval()
        if (!nativeHasCodec()) {
            setWalModeFromConfiguration()
            setLocaleFromConfiguration()
        }

        // Register functions
        val functionCount = configuration.functions.size
        for (i in 0 until functionCount) {
            val function = configuration.functions[i]
            nativeRegisterFunction(connectionPtr, function)
        }

        // Register custom extensions
        for (extension in configuration.customExtensions) {
            nativeLoadExtension(connectionPtr, extension.path, extension.entryPoint)
        }
    }

    private fun dispose(finalized: Boolean) {
        if (finalized) {
            closeGuard.warnIfOpen()
        }
        closeGuard.close()

        if (connectionPtr != 0L) {
            val cookie = recentOperations.beginOperation("close", null, null)
            try {
                preparedStatementCache.evictAll()
                nativeClose(connectionPtr)
                connectionPtr = 0
            } finally {
                recentOperations.endOperation(cookie)
            }
        }
    }

    private fun setPageSize() {
        if (!configuration.isInMemoryDb && !isReadOnlyConnection) {
            val newValue = SQLiteGlobal.defaultPageSize.toLong()
            val value = executeForLong("PRAGMA page_size")
            if (value != newValue) {
                execute("PRAGMA page_size=$newValue")
            }
        }
    }

    private fun setAutoCheckpointInterval() {
        if (!configuration.isInMemoryDb && !isReadOnlyConnection) {
            val newValue = SQLiteGlobal.wALAutoCheckpoint.toLong()
            val value = executeForLong("PRAGMA wal_autocheckpoint")
            if (value != newValue) {
                executeForLong("PRAGMA wal_autocheckpoint=$newValue")
            }
        }
    }

    private fun setJournalSizeLimit() {
        if (!configuration.isInMemoryDb && !isReadOnlyConnection) {
            val newValue = SQLiteGlobal.journalSizeLimit.toLong()
            val value = executeForLong("PRAGMA journal_size_limit",)
            if (value != newValue) {
                executeForLong("PRAGMA journal_size_limit=$newValue",)
            }
        }
    }

    private fun setForeignKeyModeFromConfiguration() {
        if (!isReadOnlyConnection) {
            val newValue = (if (configuration.foreignKeyConstraintsEnabled) 1 else 0).toLong()
            val value = executeForLong("PRAGMA foreign_keys",)
            if (value != newValue) {
                execute("PRAGMA foreign_keys=$newValue",)
            }
        }
    }

    private fun setWalModeFromConfiguration() {
        if (!configuration.isInMemoryDb && !isReadOnlyConnection) {
            if ((configuration.openFlags and ENABLE_WRITE_AHEAD_LOGGING) != 0) {
                setJournalMode("WAL")
                setSyncMode(SQLiteGlobal.wALSyncMode)
            } else {
                setJournalMode(SQLiteGlobal.defaultJournalMode)
                setSyncMode(SQLiteGlobal.defaultSyncMode)
            }
        }
    }

    private fun setSyncMode(newValue: String) {
        val value = executeForString("PRAGMA synchronous")
        if (!canonicalizeSyncMode(value).equals(
                canonicalizeSyncMode(newValue), ignoreCase = true
            )
        ) {
            execute("PRAGMA synchronous=$newValue")
        }
    }

    private fun setJournalMode(newValue: String) {
        val value = executeForString("PRAGMA journal_mode")
        if (!value.equals(newValue, ignoreCase = true)) {
            try {
                val result = executeForString("PRAGMA journal_mode=$newValue")
                if (result.equals(newValue, ignoreCase = true)) {
                    return
                }
                // PRAGMA journal_mode silently fails and returns the original journal
                // mode in some cases if the journal mode could not be changed.
            } catch (ex: SQLiteException) {
                // This error (SQLITE_BUSY) occurs if one connection has the database
                // open in WAL mode and another tries to change it to non-WAL.
                if (ex !is SQLiteDatabaseLockedException) {
                    throw ex
                }
            }

            // Because we always disable WAL mode when a database is first opened
            // (even if we intend to re-enable it), we can encounter problems if
            // there is another open connection to the database somewhere.
            // This can happen for a variety of reasons such as an application opening
            // the same database in multiple processes at the same time or if there is a
            // crashing content provider service that the ActivityManager has
            // removed from its registry but whose process hasn't quite died yet
            // by the time it is restarted in a new process.
            //
            // If we don't change the journal mode, nothing really bad happens.
            // In the worst case, an application that enables WAL might not actually
            // get it, although it can still use connection pooling.
            Log.w(
                TAG, "Could not change the database journal mode of '"
                        + configuration.label + "' from '" + value + "' to '" + newValue
                        + "' because the database is locked.  This usually means that "
                        + "there are other open connections to the database which prevents "
                        + "the database from enabling or disabling write-ahead logging mode.  "
                        + "Proceeding without changing the journal mode."
            )
        }
    }

    private fun setLocaleFromConfiguration() {
        // Register the localized collators.
        val newLocale = configuration.locale.toString()
        nativeRegisterLocalizedCollators(connectionPtr, newLocale)

        // If the database is read-only, we cannot modify the android metadata table
        // or existing indexes.
        if (isReadOnlyConnection) {
            return
        }

        try {
            // Ensure the android metadata table exists.
            execute("CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT)")

            // Check whether the locale was actually changed.
            val oldLocale = executeForString(
                "SELECT locale FROM android_metadata UNION SELECT NULL ORDER BY locale DESC LIMIT 1"
            )
            if (oldLocale == newLocale) {
                return
            }

            // Go ahead and update the indexes using the new locale.
            execute("BEGIN")
            var success = false
            try {
                execute("DELETE FROM android_metadata")
                execute("INSERT INTO android_metadata (locale) VALUES(?)", arrayOf(newLocale))
                execute("REINDEX LOCALIZED")
                success = true
            } finally {
                execute(if (success) "COMMIT" else "ROLLBACK")
            }
        } catch (ex: RuntimeException) {
            throw SQLiteException("Failed to change locale for db '${configuration.label}' to '$newLocale'.")
        }
    }

    fun enableLocalizedCollators() {
        if (nativeHasCodec()) {
            setLocaleFromConfiguration()
        }
    }

    // Called by SQLiteConnectionPool only.
    fun reconfigure(newConfiguration: SQLiteDatabaseConfiguration) {
        onlyAllowReadOnlyOperations = false

        // Register Functions
        newConfiguration.functions.filter { it !in this.configuration.functions }.forEach {
            nativeRegisterFunction(connectionPtr, it)
        }

        // Remember what changed.
        val foreignKeyModeChanged = (newConfiguration.foreignKeyConstraintsEnabled
                != this.configuration.foreignKeyConstraintsEnabled)
        val walModeChanged = ((newConfiguration.openFlags xor this.configuration.openFlags)
                and ENABLE_WRITE_AHEAD_LOGGING) != 0
        val localeChanged = newConfiguration.locale != this.configuration.locale

        // Update configuration parameters.
        this.configuration.updateParametersFrom(newConfiguration)

        // Update prepared statement cache size.
        /* mPreparedStatementCache.resize(configuration.maxSqlCacheSize); */

        // Update foreign key mode.
        if (foreignKeyModeChanged) {
            setForeignKeyModeFromConfiguration()
        }

        // Update WAL.
        if (walModeChanged) {
            setWalModeFromConfiguration()
        }

        // Update locale.
        if (localeChanged) {
            setLocaleFromConfiguration()
        }
    }

    // Called by SQLiteConnectionPool only.
    // When set to true, executing write operations will throw SQLiteException.
    // Preparing statements that might write is ok, just don't execute them.
    internal fun setOnlyAllowReadOnlyOperations(readOnly: Boolean) {
        onlyAllowReadOnlyOperations = readOnly
    }

    // Called by SQLiteConnectionPool only.
    // Returns true if the prepared statement cache contains the specified SQL.
    internal fun isPreparedStatementInCache(sql: String): Boolean = preparedStatementCache[sql] != null

    /**
     * Prepares a statement for execution but does not bind its parameters or execute it.
     *
     *
     * This method can be used to check for syntax errors during compilation
     * prior to execution of the statement.  If the `outStatementInfo` argument
     * is not null, the provided [SQLiteStatementInfo] object is populated
     * with information about the statement.
     *
     *
     * A prepared statement makes no reference to the arguments that may eventually
     * be bound to it, consequently it it possible to cache certain prepared statements
     * such as SELECT or INSERT/UPDATE statements.  If the statement is cacheable,
     * then it will be stored in the cache for later.
     *
     *
     * To take advantage of this behavior as an optimization, the connection pool
     * provides a method to acquire a connection that already has a given SQL statement
     * in its prepared statement cache so that it is ready for execution.
     *
     *s
     * @param sql The SQL statement to prepare.
     * @param outStatementInfo The [SQLiteStatementInfo] object to populate
     * with information about the statement, or null if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error.
     */
    fun prepare(sql: String, outStatementInfo: SQLiteStatementInfo?) {
        val cookie = recentOperations.beginOperation("prepare", sql, null)
        try {
            val statement = acquirePreparedStatement(sql)
            try {
                if (outStatementInfo != null) {
                    outStatementInfo.numParameters = statement.mNumParameters
                    outStatementInfo.readOnly = statement.mReadOnly

                    val columnCount = nativeGetColumnCount(connectionPtr, statement.mStatementPtr)
                    outStatementInfo.columnNames = Array(columnCount) {
                        nativeGetColumnName(connectionPtr, statement.mStatementPtr, it)
                    }
                }
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            recentOperations.endOperation(cookie)
        }
    }

    /**
     * Executes a statement that does not return a result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun execute(
        sql: String,
        bindArgs: Array<out Any?> = arrayOf(),
        cancellationSignal: CancellationSignal? = null
    ) {
        val cookie = recentOperations.beginOperation("execute", sql, bindArgs)
        try {
            val statement = acquirePreparedStatement(sql)
            try {
                throwIfStatementForbidden(statement)
                bindArguments(statement, bindArgs)
                applyBlockGuardPolicy(statement)
                attachCancellationSignal(cancellationSignal)
                try {
                    nativeExecute(connectionPtr, statement.mStatementPtr)
                } finally {
                    detachCancellationSignal(cancellationSignal)
                }
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            recentOperations.endOperation(cookie)
        }
    }

    /**
     * Executes a statement that returns a single `long` result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The value of the first column in the first row of the result set
     * as a `long`, or zero if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForLong(
        sql: String,
        bindArgs: Array<out Any?> = arrayOf(),
        cancellationSignal: CancellationSignal? = null
    ): Long {
        val cookie = recentOperations.beginOperation("executeForLong", sql, bindArgs)
        try {
            val statement = acquirePreparedStatement(sql)
            try {
                throwIfStatementForbidden(statement)
                bindArguments(statement, bindArgs)
                applyBlockGuardPolicy(statement)
                attachCancellationSignal(cancellationSignal)
                try {
                    return nativeExecuteForLong(connectionPtr, statement.mStatementPtr)
                } finally {
                    detachCancellationSignal(cancellationSignal)
                }
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            recentOperations.endOperation(cookie)
        }
    }

    /**
     * Executes a statement that returns a single [String] result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The value of the first column in the first row of the result set
     * as a `String`, or null if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForString(
        sql: String,
        bindArgs: Array<out Any?> = arrayOf(),
        cancellationSignal: CancellationSignal? = null
    ): String {
        val cookie = recentOperations.beginOperation("executeForString", sql, bindArgs)
        try {
            val statement = acquirePreparedStatement(sql)
            try {
                throwIfStatementForbidden(statement)
                bindArguments(statement, bindArgs)
                applyBlockGuardPolicy(statement)
                attachCancellationSignal(cancellationSignal)
                try {
                    return nativeExecuteForString(connectionPtr, statement.mStatementPtr)
                } finally {
                    detachCancellationSignal(cancellationSignal)
                }
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            recentOperations.endOperation(cookie)
        }
    }

    /**
     * Executes a statement that returns a count of the number of rows
     * that were changed.  Use for UPDATE or DELETE SQL statements.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The number of rows that were changed.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForChangedRowCount(
        sql: String,
        bindArgs: Array<out Any?> = arrayOf(),
        cancellationSignal: CancellationSignal? = null
    ): Int {
        var changedRows = 0
        val cookie = recentOperations.beginOperation(
            "executeForChangedRowCount",
            sql, bindArgs
        )
        try {
            val statement = acquirePreparedStatement(sql)
            try {
                throwIfStatementForbidden(statement)
                bindArguments(statement, bindArgs)
                applyBlockGuardPolicy(statement)
                attachCancellationSignal(cancellationSignal)
                try {
                    changedRows = nativeExecuteForChangedRowCount(connectionPtr, statement.mStatementPtr)
                    return changedRows
                } finally {
                    detachCancellationSignal(cancellationSignal)
                }
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            if (recentOperations.endOperationDeferLog(cookie)) {
                recentOperations.logOperation(cookie, "changedRows=$changedRows")
            }
        }
    }

    /**
     * Executes a statement that returns the row id of the last row inserted
     * by the statement.  Use for INSERT SQL statements.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The row id of the last row that was inserted, or 0 if none.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForLastInsertedRowId(
        sql: String,
        bindArgs: Array<out Any?> = arrayOf(),
        cancellationSignal: CancellationSignal? = null
    ): Long {
        val cookie = recentOperations.beginOperation(
            "executeForLastInsertedRowId",
            sql, bindArgs
        )
        try {
            val statement = acquirePreparedStatement(sql)
            try {
                throwIfStatementForbidden(statement)
                bindArguments(statement, bindArgs)
                applyBlockGuardPolicy(statement)
                attachCancellationSignal(cancellationSignal)
                try {
                    return nativeExecuteForLastInsertedRowId(connectionPtr, statement.mStatementPtr)
                } finally {
                    detachCancellationSignal(cancellationSignal)
                }
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            recentOperations.endOperation(cookie)
        }
    }

    /**
     * Executes a statement and populates the specified [CursorWindow]
     * with a range of results.  Returns the number of rows that were counted
     * during query execution.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param window The cursor window to clear and fill.
     * @param startPos The start position for filling the window.
     * @param requiredPos The position of a row that MUST be in the window.
     * If it won't fit, then the query should discard part of what it filled
     * so that it does.  Must be greater than or equal to `startPos`.
     * @param countAllRows True to count all rows that the query would return
     * regagless of whether they fit in the window.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The number of rows that were counted during query execution.  Might
     * not be all rows in the result set unless `countAllRows` is true.
     *
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForCursorWindow(
        sql: String,
        bindArgs: Array<out Any?> = arrayOf(),
        window: CursorWindow,
        startPos: Int = 0,
        requiredPos: Int = 0,
        countAllRows: Boolean = false,
        cancellationSignal: CancellationSignal? = null
    ): Int {
        window.acquireReference()
        try {
            var actualPos = -1
            var countedRows = -1
            var filledRows = -1
            val cookie = recentOperations.beginOperation("executeForCursorWindow", sql, bindArgs)
            try {
                val statement = acquirePreparedStatement(sql)
                try {
                    throwIfStatementForbidden(statement)
                    bindArguments(statement, bindArgs)
                    applyBlockGuardPolicy(statement)
                    attachCancellationSignal(cancellationSignal)
                    try {
                        val result = nativeExecuteForCursorWindow(
                            connectionPtr,
                            statement.mStatementPtr,
                            window.mWindowPtr,
                            startPos,
                            requiredPos,
                            countAllRows
                        )
                        actualPos = (result shr 32).toInt()
                        countedRows = result.toInt()
                        filledRows = window.numRows
                        window.startPosition = actualPos
                        return countedRows
                    } finally {
                        detachCancellationSignal(cancellationSignal)
                    }
                } finally {
                    releasePreparedStatement(statement)
                }
            } catch (ex: RuntimeException) {
                recentOperations.failOperation(cookie, ex)
                throw ex
            } finally {
                if (recentOperations.endOperationDeferLog(cookie)) {
                    recentOperations.logOperation(
                        cookie, "window='" + window
                                + "', startPos=" + startPos
                                + ", actualPos=" + actualPos
                                + ", filledRows=" + filledRows
                                + ", countedRows=" + countedRows
                    )
                }
            }
        } finally {
            window.releaseReference()
        }
    }

    private fun acquirePreparedStatement(sql: String): PreparedStatement {
        var statement = preparedStatementCache[sql]
        var skipCache = false
        if (statement != null) {
            if (!statement.mInUse) {
                return statement
            }
            // The statement is already in the cache but is in use (this statement appears
            // to be not only re-entrant but recursive!).  So prepare a new copy of the
            // statement but do not cache it.
            skipCache = true
        }

        val statementPtr = nativePrepareStatement(connectionPtr, sql)
        try {
            val numParameters = nativeGetParameterCount(connectionPtr, statementPtr)
            val type = SQLiteStatementType.getSqlStatementType(sql)
            val readOnly = nativeIsReadOnly(connectionPtr, statementPtr)
            statement = obtainPreparedStatement(sql, statementPtr, numParameters, type, readOnly)
            if (!skipCache && isCacheable(type)) {
                preparedStatementCache.put(sql, statement)
                statement.mInCache = true
            }
        } catch (ex: RuntimeException) {
            // Finalize the statement if an exception occurred and we did not add
            // it to the cache.  If it is already in the cache, then leave it there.
            if (statement == null || !statement.mInCache) {
                nativeFinalizeStatement(connectionPtr, statementPtr)
            }
            throw ex
        }
        statement.mInUse = true
        return statement
    }

    private fun releasePreparedStatement(statement: PreparedStatement?) {
        statement!!.mInUse = false
        if (statement.mInCache) {
            try {
                nativeResetStatementAndClearBindings(connectionPtr, statement.mStatementPtr)
            } catch (ex: SQLiteException) {
                // The statement could not be reset due to an error.  Remove it from the cache.
                // When remove() is called, the cache will invoke its entryRemoved() callback,
                // which will in turn call finalizePreparedStatement() to finalize and
                // recycle the statement.
                if (DEBUG) {
                    Log.d(
                        TAG, "Could not reset prepared statement due to an exception.  "
                                + "Removing it from the cache.  SQL: "
                                + trimSqlForDisplay(statement.mSql), ex
                    )
                }

                preparedStatementCache.remove(statement.mSql!!)
            }
        } else {
            finalizePreparedStatement(statement)
        }
    }

    private fun finalizePreparedStatement(statement: PreparedStatement?) {
        nativeFinalizeStatement(connectionPtr, statement!!.mStatementPtr)
        recyclePreparedStatement(statement)
    }

    private fun attachCancellationSignal(cancellationSignal: CancellationSignal?) {
        if (cancellationSignal == null) {
            return
        }

        cancellationSignal.throwIfCanceled()

        cancellationSignalAttachCount += 1
        if (cancellationSignalAttachCount == 1) {
            // Reset cancellation flag before executing the statement.
            nativeResetCancel(connectionPtr, true /*cancelable*/)

            // After this point, onCancel() may be called concurrently.
            cancellationSignal.setOnCancelListener(this)
        }
    }

    @SuppressLint("Assert")
    private fun detachCancellationSignal(cancellationSignal: CancellationSignal?) {
        if (cancellationSignal == null) {
            return
        }

        assert(cancellationSignalAttachCount > 0)

        cancellationSignalAttachCount -= 1
        if (cancellationSignalAttachCount == 0) {
            // After this point, onCancel() cannot be called concurrently.
            cancellationSignal.setOnCancelListener(null)

            // Reset cancellation flag after executing the statement.
            nativeResetCancel(connectionPtr, false /*cancelable*/)
        }
    }

    // CancellationSignal.OnCancelListener callback.
    // This method may be called on a different thread than the executing statement.
    // However, it will only be called between calls to attachCancellationSignal and
    // detachCancellationSignal, while a statement is executing.  We can safely assume
    // that the SQLite connection is still alive.
    override fun onCancel() = nativeCancel(connectionPtr)

    private fun bindArguments(statement: PreparedStatement, bindArgs: Array<out Any?>) {
        if (bindArgs.size != statement.mNumParameters) {
            throw SQLiteBindOrColumnIndexOutOfRangeException(
                "Expected ${statement.mNumParameters} bind arguments but $${bindArgs.size} were provided."
            )
        }

        val statementPtr = statement.mStatementPtr
        bindArgs.forEachIndexed { i, arg ->
            when (getTypeOfObject(arg)) {
                Cursor.FIELD_TYPE_NULL -> nativeBindNull(
                    connectionPtr = connectionPtr,
                    statementPtr = statementPtr,
                    index = i + 1
                )

                Cursor.FIELD_TYPE_INTEGER -> nativeBindLong(
                    connectionPtr = connectionPtr,
                    statementPtr = statementPtr,
                    index = i + 1,
                    value = (arg as Number).toLong()
                )

                Cursor.FIELD_TYPE_FLOAT -> nativeBindDouble(
                    connectionPtr = connectionPtr,
                    statementPtr = statementPtr,
                    index = i + 1,
                    value = (arg as Number).toDouble()
                )

                Cursor.FIELD_TYPE_BLOB -> nativeBindBlob(
                    connectionPtr = connectionPtr,
                    statementPtr = statementPtr,
                    index = i + 1,
                    value = arg as ByteArray
                )
                Cursor.FIELD_TYPE_STRING -> if (arg is Boolean) {
                    // Provide compatibility with legacy applications which may pass
                    // Boolean values in bind args.
                    nativeBindLong(
                        connectionPtr = connectionPtr,
                        statementPtr = statementPtr,
                        index = i + 1,
                        value = (if (arg) 1 else 0).toLong()
                    )
                } else {
                    nativeBindString(
                        connectionPtr = connectionPtr,
                        statementPtr = statementPtr,
                        index = i + 1,
                        value = arg.toString()
                    )
                }

                else -> if (arg is Boolean) {
                    nativeBindLong(
                        connectionPtr = connectionPtr,
                        statementPtr = statementPtr,
                        index = i + 1,
                        value = (if (arg) 1 else 0).toLong()
                    )
                } else {
                    nativeBindString(
                        connectionPtr = connectionPtr,
                        statementPtr = statementPtr,
                        index = i + 1,
                        value = arg.toString()
                    )
                }
            }
        }
    }

    private fun throwIfStatementForbidden(statement: PreparedStatement) {
        if (onlyAllowReadOnlyOperations && !statement.mReadOnly) {
            throw SQLiteException(
                "Cannot execute this statement because it might modify the database but the connection is read-only."
            )
        }
    }

    private fun applyBlockGuardPolicy(statement: PreparedStatement) {
        if (!configuration.isInMemoryDb && SQLiteDebug.DEBUG_SQL_LOG) {
            // don't have access to the policy, so just log
            if (Looper.myLooper() == Looper.getMainLooper()) {
                if (statement.mReadOnly) {
                    Log.w(TAG, "Reading from disk on main thread")
                } else {
                    Log.w(TAG, "Writing to disk on main thread")
                }
            }
        }
    }

    /**
     * Dumps debugging information about this connection.
     *
     * @param printer The printer to receive the dump, not null.
     * @param verbose True to dump more verbose information.
     */
    fun dump(printer: Printer, verbose: Boolean) = dumpUnsafe(printer, verbose)

    /**
     * Dumps debugging information about this connection, in the case where the
     * caller might not actually own the connection.
     *
     * This function is written so that it may be called by a thread that does not
     * own the connection.  We need to be very careful because the connection state is
     * not synchronized.
     *
     * At worst, the method may return stale or slightly wrong data, however
     * it should not crash.  This is ok as it is only used for diagnostic purposes.
     *
     * @param printer The printer to receive the dump, not null.
     * @param verbose True to dump more verbose information.
     */
    fun dumpUnsafe(printer: Printer, verbose: Boolean) {
        printer.println("Connection #$connectionId:")
        if (verbose) {
            printer.println("  connectionPtr: 0x" + java.lang.Long.toHexString(connectionPtr))
        }
        printer.println("  isPrimaryConnection: " + isPrimaryConnection)
        printer.println("  onlyAllowReadOnlyOperations: $onlyAllowReadOnlyOperations")

        recentOperations.dump(printer, verbose)

        if (verbose) {
            preparedStatementCache.dump(printer)
        }
    }

    /**
     * Describes the currently executing operation, in the case where the
     * caller might not actually own the connection.
     *
     * This function is written so that it may be called by a thread that does not
     * own the connection.  We need to be very careful because the connection state is
     * not synchronized.
     *
     * At worst, the method may return stale or slightly wrong data, however
     * it should not crash.  This is ok as it is only used for diagnostic purposes.
     *
     * @return A description of the current operation including how long it has been running,
     * or null if none.
     */
    fun describeCurrentOperationUnsafe(): String? = recentOperations.describeCurrentOperation()

    /**
     * Collects statistics about database connection memory usage.
     *
     * @param dbStatsList The list to populate.
     */
    fun collectDbStats(dbStatsList: ArrayList<SQLiteDebug.DbStats>) {
        // Get information about the main database.
        val lookaside = nativeGetDbLookaside(connectionPtr)
        var pageCount: Long = 0
        var pageSize: Long = 0
        try {
            pageCount = executeForLong("PRAGMA page_count;")
            pageSize = executeForLong("PRAGMA page_size;")
        } catch (ex: SQLiteException) {
            // Ignore.
        }
        dbStatsList.add(getMainDbStatsUnsafe(lookaside, pageCount, pageSize))

        // Get information about attached databases.
        // We ignore the first row in the database list because it corresponds to
        // the main database which we have already described.
        val window = CursorWindow("collectDbStats")
        try {
            executeForCursorWindow(
                sql = "PRAGMA database_list;",
                window = window
            )
            for (i in 1 until window.numRows) {
                val name = window.getString(i, 1)
                val path = window.getString(i, 2)
                pageCount = 0
                pageSize = 0
                try {
                    pageCount = executeForLong("PRAGMA $name.page_count;")
                    pageSize = executeForLong("PRAGMA $name.page_size;")
                } catch (ex: SQLiteException) {
                    // Ignore.
                }
                var label = "  (attached) $name"
                if (!path.isEmpty()) {
                    label += ": $path"
                }
                dbStatsList.add(SQLiteDebug.DbStats(label, pageCount, pageSize, 0, 0, 0, 0))
            }
        } catch (ex: SQLiteException) {
            // Ignore.
        } finally {
            window.close()
        }
    }

    /**
     * Collects statistics about database connection memory usage, in the case where the
     * caller might not actually own the connection.
     */
    fun collectDbStatsUnsafe(dbStatsList: MutableList<SQLiteDebug.DbStats>) {
        dbStatsList.add(getMainDbStatsUnsafe(0, 0, 0))
    }

    private fun getMainDbStatsUnsafe(lookaside: Int, pageCount: Long, pageSize: Long): SQLiteDebug.DbStats {
        // The prepared statement cache is thread-safe so we can access its statistics
        // even if we do not own the database connection.
        var label = configuration.path
        if (!isPrimaryConnection) {
            label += " ($connectionId)"
        }
        return SQLiteDebug.DbStats(
            label, pageCount, pageSize, lookaside,
            preparedStatementCache.hitCount(),
            preparedStatementCache.missCount(),
            preparedStatementCache.size()
        )
    }

    override fun toString(): String {
        return "SQLiteConnection: " + configuration.path + " (" + connectionId + ")"
    }

    private fun obtainPreparedStatement(
        sql: String, statementPtr: Long,
        numParameters: Int, type: Int, readOnly: Boolean
    ): PreparedStatement {
        var statement = preparedStatementPool
        if (statement != null) {
            preparedStatementPool = statement.mPoolNext
            statement.mPoolNext = null
            statement.mInCache = false
        } else {
            statement = PreparedStatement()
        }
        statement.mSql = sql
        statement.mStatementPtr = statementPtr
        statement.mNumParameters = numParameters
        statement.mType = type
        statement.mReadOnly = readOnly
        return statement
    }

    private fun recyclePreparedStatement(statement: PreparedStatement?) {
        statement!!.mSql = null
        statement.mPoolNext = preparedStatementPool
        preparedStatementPool = statement
    }

    /**
     * Holder type for a prepared statement.
     *
     * Although this object holds a pointer to a native statement object, it
     * does not have a finalizer.  This is deliberate.  The [SQLiteConnection]
     * owns the statement object and will take care of freeing it when needed.
     * In particular, closing the connection requires a guarantee of deterministic
     * resource disposal because all native statement objects must be freed before
     * the native database object can be closed.  So no finalizers here.
     */
    private class PreparedStatement {
        // Next item in pool.
        var mPoolNext: PreparedStatement? = null

        // The SQL from which the statement was prepared.
        var mSql: String? = null

        // The native sqlite3_stmt object pointer.
        // Lifetime is managed explicitly by the connection.
        var mStatementPtr: Long = 0

        // The number of parameters that the prepared statement has.
        var mNumParameters: Int = 0

        // The statement type.
        var mType: Int = 0

        // True if the statement is read-only.
        var mReadOnly: Boolean = false

        // True if the statement is in the cache.
        var mInCache: Boolean = false

        // True if the statement is in use (currently executing).
        // We need this flag because due to the use of custom functions in triggers, it's
        // possible for SQLite calls to be re-entrant.  Consequently we need to prevent
        // in use statements from being finalized until they are no longer in use.
        var mInUse: Boolean = false
    }

    private inner class PreparedStatementCache(size: Int) : LruCache<String?, PreparedStatement>(size) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: PreparedStatement,
            newValue: PreparedStatement?
        ) {
            oldValue.mInCache = false
            if (!oldValue.mInUse) {
                finalizePreparedStatement(oldValue)
            }
        }

        fun dump(printer: Printer) {
            printer.println("  Prepared statement cache:")
            val cache = snapshot()
            if (!cache.isEmpty()) {
                var i = 0
                for ((sql, statement) in cache) {
                    if (statement!!.mInCache) { // might be false due to a race with entryRemoved
                        printer.println(
                            "    " + i + ": statementPtr=0x"
                                    + java.lang.Long.toHexString(statement.mStatementPtr)
                                    + ", numParameters=" + statement.mNumParameters
                                    + ", type=" + statement.mType
                                    + ", readOnly=" + statement.mReadOnly
                                    + ", sql=\"" + trimSqlForDisplay(sql) + "\""
                        )
                    }
                    i += 1
                }
            } else {
                printer.println("    <none>")
            }
        }
    }

    private class OperationLog {
        private val mOperations = arrayOfNulls<Operation>(MAX_RECENT_OPERATIONS)
        private var mIndex = 0
        private var mGeneration = 0

        fun beginOperation(kind: String?, sql: String?, bindArgs: Array<out Any?>?): Int {
            synchronized(mOperations) {
                val index = (mIndex + 1) % MAX_RECENT_OPERATIONS
                var operation = mOperations[index]
                if (operation == null) {
                    operation = Operation()
                    mOperations[index] = operation
                } else {
                    operation.mFinished = false
                    operation.mException = null
                    if (operation.mBindArgs != null) {
                        operation.mBindArgs!!.clear()
                    }
                }
                operation.mStartTime = System.currentTimeMillis()
                operation.mKind = kind
                operation.mSql = sql
                if (bindArgs != null) {
                    if (operation.mBindArgs == null) {
                        operation.mBindArgs = ArrayList()
                    } else {
                        operation.mBindArgs!!.clear()
                    }
                    for (arg in bindArgs) {
                        if (arg != null && arg is ByteArray) {
                            // Don't hold onto the real byte array longer than necessary.
                            operation.mBindArgs!!.add(arrayOf<Byte>())
                        } else {
                            operation.mBindArgs!!.add(arg)
                        }
                    }
                }
                operation.mCookie = newOperationCookieLocked(index)
                mIndex = index
                return operation.mCookie
            }
        }

        fun failOperation(cookie: Int, ex: Exception?) {
            synchronized(mOperations) {
                val operation = getOperationLocked(cookie)
                if (operation != null) {
                    operation.mException = ex
                }
            }
        }

        fun endOperation(cookie: Int) {
            synchronized(mOperations) {
                if (endOperationDeferLogLocked(cookie)) {
                    logOperationLocked(cookie, null)
                }
            }
        }

        fun endOperationDeferLog(cookie: Int): Boolean {
            synchronized(mOperations) {
                return endOperationDeferLogLocked(cookie)
            }
        }

        fun logOperation(cookie: Int, detail: String?) {
            synchronized(mOperations) {
                logOperationLocked(cookie, detail)
            }
        }

        private fun endOperationDeferLogLocked(cookie: Int): Boolean {
            val operation = getOperationLocked(cookie)
            if (operation != null) {
                operation.mEndTime = System.currentTimeMillis()
                operation.mFinished = true
                return SQLiteDebug.DEBUG_LOG_SLOW_QUERIES && SQLiteDebug.shouldLogSlowQuery(
                    operation.mEndTime - operation.mStartTime
                )
            }
            return false
        }

        private fun logOperationLocked(cookie: Int, detail: String?) {
            val operation = getOperationLocked(cookie) ?: return
            val msg = StringBuilder()
            operation.describe(msg, false)
            if (detail != null) {
                msg.append(", ").append(detail)
            }
            Log.d(TAG, msg.toString())
        }

        private fun newOperationCookieLocked(index: Int): Int {
            val generation = mGeneration++
            return generation shl COOKIE_GENERATION_SHIFT or index
        }

        private fun getOperationLocked(cookie: Int): Operation? {
            val index = cookie and COOKIE_INDEX_MASK
            val operation = mOperations[index]
            return if (operation!!.mCookie == cookie) operation else null
        }

        fun describeCurrentOperation(): String? {
            synchronized(mOperations) {
                val operation = mOperations[mIndex]
                if (operation != null && !operation.mFinished) {
                    val msg = StringBuilder()
                    operation.describe(msg, false)
                    return msg.toString()
                }
                return null
            }
        }

        fun dump(printer: Printer, verbose: Boolean) {
            synchronized(mOperations) {
                printer.println("  Most recently executed operations:")
                var index = mIndex
                var operation: Operation? = mOperations[index]
                if (operation != null) {
                    var n = 0
                    do {
                        val msg = buildString {
                            append("    ")
                            append(n)
                            append(": [")
                            append(operation!!.formattedStartTime)
                            append("] ")
                            operation!!.describe(this, verbose)
                        }
                        printer.println(msg)

                        if (index > 0) {
                            index -= 1
                        } else {
                            index = MAX_RECENT_OPERATIONS - 1
                        }
                        n += 1
                        operation = mOperations[index]
                    } while (operation != null && n < MAX_RECENT_OPERATIONS)
                } else {
                    printer.println("    <none>")
                }
            }
        }

        companion object {
            private const val MAX_RECENT_OPERATIONS = 20
            private const val COOKIE_GENERATION_SHIFT = 8
            private const val COOKIE_INDEX_MASK = 0xff
        }
    }

    private class Operation {
        var mStartTime: Long = 0
        var mEndTime: Long = 0
        var mKind: String? = null
        var mSql: String? = null
        var mBindArgs: ArrayList<Any?>? = null
        var mFinished: Boolean = false
        var mException: Exception? = null
        var mCookie: Int = 0

        fun describe(msg: StringBuilder, verbose: Boolean) {
            msg.append(mKind)
            if (mFinished) {
                msg.append(" took ").append(mEndTime - mStartTime).append("ms")
            } else {
                msg.append(" started ").append(System.currentTimeMillis() - mStartTime)
                    .append("ms ago")
            }
            msg.append(" - ").append(status)
            if (mSql != null) {
                msg.append(", sql=\"").append(trimSqlForDisplay(mSql)).append("\"")
            }
            if (verbose && mBindArgs != null && mBindArgs!!.size != 0) {
                msg.append(", bindArgs=[")
                val count = mBindArgs!!.size
                for (i in 0 until count) {
                    val arg = mBindArgs!![i]
                    if (i != 0) {
                        msg.append(", ")
                    }
                    if (arg == null) {
                        msg.append("null")
                    } else if (arg is ByteArray) {
                        msg.append("<byte[]>")
                    } else if (arg is String) {
                        msg.append("\"").append(arg as String?).append("\"")
                    } else {
                        msg.append(arg)
                    }
                }
                msg.append("]")
            }
            if (mException != null) {
                msg.append(", exception=\"").append(mException!!.message).append("\"")
            }
        }

        private val status: String
            get() {
                if (!mFinished) {
                    return "running"
                }
                return if (mException != null) "failed" else "succeeded"
            }

        internal val formattedStartTime: String
            get() = sDateFormat.format(Date(mStartTime))

        companion object {
            @SuppressLint("SimpleDateFormat")
            private val sDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        }
    }

    companion object {
        private const val TAG = "SQLiteConnection"
        private const val DEBUG = false

        private val TRIM_SQL_PATTERN: Pattern = Pattern.compile("[\\s]*\\n+[\\s]*")

        private external fun nativeOpen(
            path: String, openFlags: Int, label: String,
            enableTrace: Boolean, enableProfile: Boolean
        ): Long

        private external fun nativeClose(connectionPtr: Long)
        private external fun nativeRegisterFunction(
            connectionPtr: Long,
            function: SQLiteFunction
        )

        private external fun nativeRegisterLocalizedCollators(connectionPtr: Long, locale: String)
        private external fun nativePrepareStatement(connectionPtr: Long, sql: String): Long
        private external fun nativeFinalizeStatement(connectionPtr: Long, statementPtr: Long)
        private external fun nativeGetParameterCount(connectionPtr: Long, statementPtr: Long): Int
        private external fun nativeIsReadOnly(connectionPtr: Long, statementPtr: Long): Boolean
        private external fun nativeGetColumnCount(connectionPtr: Long, statementPtr: Long): Int
        private external fun nativeGetColumnName(
            connectionPtr: Long, statementPtr: Long,
            index: Int
        ): String?

        private external fun nativeBindNull(
            connectionPtr: Long, statementPtr: Long,
            index: Int
        )

        private external fun nativeBindLong(
            connectionPtr: Long, statementPtr: Long,
            index: Int, value: Long
        )

        private external fun nativeBindDouble(
            connectionPtr: Long, statementPtr: Long,
            index: Int, value: Double
        )

        private external fun nativeBindString(
            connectionPtr: Long, statementPtr: Long,
            index: Int, value: String
        )

        private external fun nativeBindBlob(
            connectionPtr: Long, statementPtr: Long,
            index: Int, value: ByteArray
        )

        private external fun nativeResetStatementAndClearBindings(
            connectionPtr: Long, statementPtr: Long
        )

        private external fun nativeExecute(connectionPtr: Long, statementPtr: Long)
        private external fun nativeExecuteForLong(connectionPtr: Long, statementPtr: Long): Long
        private external fun nativeExecuteForString(connectionPtr: Long, statementPtr: Long): String
        private external fun nativeExecuteForBlobFileDescriptor(
            connectionPtr: Long, statementPtr: Long
        ): Int

        private external fun nativeExecuteForChangedRowCount(connectionPtr: Long, statementPtr: Long): Int
        private external fun nativeExecuteForLastInsertedRowId(
            connectionPtr: Long, statementPtr: Long
        ): Long

        private external fun nativeExecuteForCursorWindow(
            connectionPtr: Long, statementPtr: Long, winPtr: Long,
            startPos: Int, requiredPos: Int, countAllRows: Boolean
        ): Long

        private external fun nativeGetDbLookaside(connectionPtr: Long): Int
        private external fun nativeCancel(connectionPtr: Long)
        private external fun nativeResetCancel(connectionPtr: Long, cancelable: Boolean)

        private external fun nativeHasCodec(): Boolean
        private external fun nativeLoadExtension(connectionPtr: Long, file: String, proc: String)

        fun hasCodec(): Boolean {
            return nativeHasCodec()
        }

        // Called by SQLiteConnectionPool only.
        fun open(
            pool: SQLiteConnectionPool,
            configuration: SQLiteDatabaseConfiguration,
            connectionId: Int, primaryConnection: Boolean
        ): SQLiteConnection {
            val connection = SQLiteConnection(
                pool, configuration,
                connectionId, primaryConnection
            )
            try {
                connection.open()
                return connection
            } catch (ex: SQLiteException) {
                connection.dispose(false)
                throw ex
            }
        }

        private fun canonicalizeSyncMode(value: String): String {
            when (value) {
                "0" -> return "OFF"
                "1" -> return "NORMAL"
                "2" -> return "FULL"
            }
            return value
        }

        /**
         * Returns data type of the given object's value.
         *
         *
         * Returned values are
         *
         *  * [Cursor.FIELD_TYPE_NULL]
         *  * [Cursor.FIELD_TYPE_INTEGER]
         *  * [Cursor.FIELD_TYPE_FLOAT]
         *  * [Cursor.FIELD_TYPE_STRING]
         *  * [Cursor.FIELD_TYPE_BLOB]
         *
         *
         *
         * @param obj the object whose value type is to be returned
         * @return object value type
         */
        private fun getTypeOfObject(obj: Any?): Int = when (obj) {
            null -> Cursor.FIELD_TYPE_NULL
            is ByteArray -> Cursor.FIELD_TYPE_BLOB
            is Float, is Double -> Cursor.FIELD_TYPE_FLOAT
            is Long, is Int, is Short, is Byte -> Cursor.FIELD_TYPE_INTEGER
            else -> Cursor.FIELD_TYPE_STRING
        }

        private fun isCacheable(statementType: Int): Boolean = statementType == SQLiteStatementType.STATEMENT_UPDATE ||
                statementType == SQLiteStatementType.STATEMENT_SELECT

        private fun trimSqlForDisplay(sql: String?): String {
            return TRIM_SQL_PATTERN.matcher(sql ?: "").replaceAll(" ")
        }
    }
}