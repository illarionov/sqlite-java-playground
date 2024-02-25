package io.requery.android.database.sqlite

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteQueryBuilder
import android.database.sqlite.SQLiteTransactionListener
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.EventLog
import android.util.Log
import android.util.Pair
import android.util.Printer
import androidx.annotation.IntDef
import androidx.core.os.CancellationSignal
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import io.requery.android.database.DatabaseErrorHandler
import io.requery.android.database.DefaultDatabaseErrorHandler
import io.requery.android.database.sqlite.SQLiteConnectionPool.CONNECTION_FLAG_INTERACTIVE
import io.requery.android.database.sqlite.SQLiteConnectionPool.CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY
import io.requery.android.database.sqlite.SQLiteConnectionPool.CONNECTION_FLAG_READ_ONLY
import io.requery.android.database.sqlite.SQLiteDatabaseConfiguration.Companion.MEMORY_DB_PATH
import io.requery.android.database.sqlite.SQLiteSession.TRANSACTION_MODE_DEFERRED
import io.requery.android.database.sqlite.SQLiteSession.TRANSACTION_MODE_EXCLUSIVE
import io.requery.android.database.sqlite.SQLiteSession.TRANSACTION_MODE_IMMEDIATE
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.Locale

/**
 * Exposes methods to manage a SQLite database.
 *
 * SQLiteDatabase has methods to create, delete, execute SQL commands, and
 * perform other common database management tasks.
 *
 * Database names must be unique within an application, not across all applications.
 *
 * ### Localized Collation - ORDER BY
 *
 * In addition to SQLite's default `BINARY` collator, Android supplies
 * two more, `LOCALIZED`, which changes with the system's current locale,
 * and `UNICODE`, which is the Unicode Collation Algorithm and not tailored
 * to the current locale.
 *
 */
class SQLiteDatabase private constructor(
    configuration: SQLiteDatabaseConfiguration,
    // The optional factory to use when creating new Cursors.  May be null.
    private val cursorFactory: CursorFactory? = null,
    errorHandler: DatabaseErrorHandler? = null
) : SQLiteClosable(), SupportSQLiteDatabase {
    // Thread-local for database sessions that belong to this database.
    // Each thread has its own database session.
    // INVARIANT: Immutable.
    private val _threadSession: ThreadLocal<SQLiteSession> = object : ThreadLocal<SQLiteSession>() {
        override fun initialValue(): SQLiteSession = createSession()
    }

    // Error handler to be used when SQLite returns corruption errors.
    // INVARIANT: Immutable.
    private val errorHandler = errorHandler ?: DefaultDatabaseErrorHandler()

    // Shared database state lock.
    // This lock guards all of the shared state of the database, such as its
    // configuration, whether it is open or closed, and so on.  This lock should
    // be held for as little time as possible.
    //
    // The lock MUST NOT be held while attempting to acquire database connections or
    // while executing SQL statements on behalf of the client as it can lead to deadlock.
    //
    // It is ok to hold the lock while reconfiguring the connection pool or dumping
    // statistics because those operations are non-reentrant and do not try to acquire
    // connections that might be held by other threads.
    //
    // Basic rule: grab the lock, access or modify global state, release the lock, then
    // do the required SQL work.
    private val lock = Any()

    // Warns if the database is finalized without being closed properly.
    // INVARIANT: Guarded by mLock.
    private val closeGuardLocked: CloseGuard? = CloseGuard.get()

    // The database configuration.
    // INVARIANT: Guarded by mLock.
    private val configurationLocked = configuration

    // The connection pool for the database, null when closed.
    // The pool itself is thread-safe, but the reference to it can only be acquired
    // when the lock is held.
    // INVARIANT: Guarded by mLock.
    private var connectionPoolLocked: SQLiteConnectionPool? = null

    /**
     * Gets the [SQLiteSession] that belongs to this thread for this database.
     * Once a thread has obtained a session, it will continue to obtain the same
     * session even after the database has been closed (although the session will not
     * be usable).  However, a thread that does not already have a session cannot
     * obtain one after the database has been closed.
     *
     * The idea is that threads that have active connections to the database may still
     * have work to complete even after the call to [.close].  Active database
     * connections are not actually disposed until they are released by the threads
     * that own them.
     *
     * @return The session, never null.
     *
     * @throws IllegalStateException if the thread does not yet have a session and
     * the database is not open.
     */
    val threadSession: SQLiteSession
        get() = _threadSession.get()!! // initialValue() throws if database closed


    /** Conflict options integer enumeration definition  */
    @IntDef(
        CONFLICT_ABORT, CONFLICT_FAIL, CONFLICT_IGNORE, CONFLICT_NONE, CONFLICT_REPLACE, CONFLICT_ROLLBACK
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ConflictAlgorithm

    /** Integer flag definition for the database open options  */
    @IntDef(
        flag = true,
        value = [OPEN_READONLY, OPEN_READWRITE, OPEN_CREATE, OPEN_URI, OPEN_NOMUTEX, OPEN_FULLMUTEX, OPEN_SHAREDCACHE, OPEN_PRIVATECACHE, CREATE_IF_NECESSARY, ENABLE_WRITE_AHEAD_LOGGING]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class OpenFlags

    @Throws(Throwable::class)
    protected fun finalize() = dispose(true)

    override fun onAllReferencesReleased() = dispose(false)

    private fun dispose(finalized: Boolean) {
        val pool: SQLiteConnectionPool?
        synchronized(lock) {
            if (closeGuardLocked != null) {
                if (finalized) {
                    closeGuardLocked.warnIfOpen()
                }
                closeGuardLocked.close()
            }
            pool = connectionPoolLocked
            connectionPoolLocked = null
        }

        if (!finalized) {
            pool?.close()
        }
    }

    /**
     * Gets a label to use when describing the database in log messages.
     * @return The label.
     */
    val label: String
        get() = synchronized(lock, configurationLocked::label)

    /**
     * Sends a corruption message to the database error handler.
     */
    fun onCorruption() {
        EventLog.writeEvent(EVENT_DB_CORRUPT, label)
        errorHandler.onCorruption(this)
    }

    fun createSession(): SQLiteSession {
        val pool: SQLiteConnectionPool?
        synchronized(lock) {
            throwIfNotOpenLocked()
            pool = connectionPoolLocked
        }
        return SQLiteSession(pool)
    }

    /**
     * Gets default connection flags that are appropriate for this thread, taking into
     * account whether the thread is acting on behalf of the UI.
     *
     * @param readOnly True if the connection should be read-only.
     * @return The connection flags.
     */
    fun getThreadDefaultConnectionFlags(readOnly: Boolean): Int {
        var flags = if (readOnly) CONNECTION_FLAG_READ_ONLY else CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY
        if (isMainThread) {
            flags = flags or CONNECTION_FLAG_INTERACTIVE
        }
        return flags
    }

    /**
     * Begins a transaction in EXCLUSIVE mode.
     *
     *
     * Transactions can be nested.
     * When the outer transaction is ended all of
     * the work done in that transaction and all of the nested transactions will be committed or
     * rolled back. The changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransaction();
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     */
    override fun beginTransaction() = beginTransaction(null, TRANSACTION_MODE_EXCLUSIVE)

    /**
     * Begins a transaction in IMMEDIATE mode. Transactions can be nested. When
     * the outer transaction is ended all of the work done in that transaction
     * and all of the nested transactions will be committed or rolled back. The
     * changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they
     * will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransactionNonExclusive();
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     */
    override fun beginTransactionNonExclusive() = beginTransaction(null, TRANSACTION_MODE_IMMEDIATE)

    /**
     * Begins a transaction in DEFERRED mode.
     */
    fun beginTransactionDeferred() = beginTransaction(null, TRANSACTION_MODE_DEFERRED)

    /**
     * Begins a transaction in DEFERRED mode.
     *
     * @param transactionListener listener that should be notified when the transaction begins,
     * commits, or is rolled back, either explicitly or by a call to
     * [.yieldIfContendedSafely].
     */
    fun beginTransactionWithListenerDeferred(
        transactionListener: SQLiteTransactionListener?
    ) = beginTransaction(transactionListener, TRANSACTION_MODE_DEFERRED)

    /**
     * Begins a transaction in EXCLUSIVE mode.
     *
     *
     * Transactions can be nested.
     * When the outer transaction is ended all of
     * the work done in that transaction and all of the nested transactions will be committed or
     * rolled back. The changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransactionWithListener(listener);
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     *
     * @param transactionListener listener that should be notified when the transaction begins,
     * commits, or is rolled back, either explicitly or by a call to
     * [.yieldIfContendedSafely].
     */
    override fun beginTransactionWithListener(
        transactionListener: SQLiteTransactionListener
    ) = beginTransaction(transactionListener, TRANSACTION_MODE_EXCLUSIVE)

    /**
     * Begins a transaction in IMMEDIATE mode. Transactions can be nested. When
     * the outer transaction is ended all of the work done in that transaction
     * and all of the nested transactions will be committed or rolled back. The
     * changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they
     * will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransactionWithListenerNonExclusive(listener);
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     *
     * @param transactionListener listener that should be notified when the
     * transaction begins, commits, or is rolled back, either
     * explicitly or by a call to [.yieldIfContendedSafely].
     */
    override fun beginTransactionWithListenerNonExclusive(
        transactionListener: SQLiteTransactionListener
    ) {
        beginTransaction(transactionListener, TRANSACTION_MODE_IMMEDIATE)
    }

    private fun beginTransaction(transactionListener: SQLiteTransactionListener?, mode: Int) = useReference {
        threadSession.beginTransaction(
            mode, transactionListener,
            getThreadDefaultConnectionFlags(false /*readOnly*/), null
        )
    }

    /**
     * End a transaction. See beginTransaction for notes about how to use this and when transactions
     * are committed and rolled back.
     */
    override fun endTransaction() = useReference {
        threadSession.endTransaction(null)
    }

    /**
     * Marks the current transaction as successful. Do not do any more database work between
     * calling this and calling endTransaction. Do as little non-database work as possible in that
     * situation too. If any errors are encountered between this and endTransaction the transaction
     * will still be committed.
     *
     * @throws IllegalStateException if the current thread is not in a transaction or the
     * transaction is already marked as successful.
     */
    override fun setTransactionSuccessful() = useReference {
        threadSession.setTransactionSuccessful()
    }

    /**
     * Returns true if the current thread has a transaction pending.
     *
     * @return True if the current thread is in a transaction.
     */
    override fun inTransaction(): Boolean = useReference {
        threadSession.hasTransaction()
    }

    /**
     * Returns true if the current thread is holding an active connection to the database.
     *
     *
     * The name of this method comes from a time when having an active connection
     * to the database meant that the thread was holding an actual lock on the
     * database.  Nowadays, there is no longer a true "database lock" although threads
     * may block if they cannot acquire a database connection to perform a
     * particular operation.
     *
     *
     * @return True if the current thread is holding an active connection to the database.
     */
    override val isDbLockedByCurrentThread: Boolean
        get() = useReference {
            threadSession.hasConnection()
        }

    /**
     * Temporarily end the transaction to let other threads run. The transaction is assumed to be
     * successful so far. Do not call setTransactionSuccessful before calling this. When this
     * returns a new transaction will have been created but not marked as successful. This assumes
     * that there are no nested transactions (beginTransaction has only been called once) and will
     * throw an exception if that is not the case.
     * @return true if the transaction was yielded
     */
    override fun yieldIfContendedSafely(): Boolean = yieldIfContendedHelper(
        true,
        /* check yielding */-1 /* sleepAfterYieldDelay*/
    )

    /**
     * Temporarily end the transaction to let other threads run. The transaction is assumed to be
     * successful so far. Do not call setTransactionSuccessful before calling this. When this
     * returns a new transaction will have been created but not marked as successful. This assumes
     * that there are no nested transactions (beginTransaction has only been called once) and will
     * throw an exception if that is not the case.
     * @param sleepAfterYieldDelayMillis if > 0, sleep this long before starting a new transaction if
     * the lock was actually yielded. This will allow other background threads to make some
     * more progress than they would if we started the transaction immediately.
     * @return true if the transaction was yielded
     */
    override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean {
        return yieldIfContendedHelper(true,  /* check yielding */sleepAfterYieldDelayMillis)
    }

    private fun yieldIfContendedHelper(throwIfUnsafe: Boolean, sleepAfterYieldDelay: Long): Boolean = useReference {
        threadSession.yieldTransaction(sleepAfterYieldDelay, throwIfUnsafe, null)
    }

    /**
     * Reopens the database in read-write mode.
     * If the database is already read-write, does nothing.
     *
     * @throws SQLiteException if the database could not be reopened as requested, in which
     * case it remains open in read only mode.
     * @throws IllegalStateException if the database is not open.
     *
     * @see .isReadOnly
     * @hide
     */
    fun reopenReadWrite() = synchronized(lock) {
        throwIfNotOpenLocked()
        if (!isReadOnlyLocked) {
            return  // nothing to do
        }

        // Reopen the database in read-write mode.
        val oldOpenFlags = configurationLocked.openFlags
        configurationLocked.openFlags = (configurationLocked.openFlags and OPEN_READONLY.inv())
        try {
            connectionPoolLocked!!.reconfigure(configurationLocked)
        } catch (ex: RuntimeException) {
            configurationLocked.openFlags = oldOpenFlags
            throw ex
        }
    }

    private fun open() = try {
        if (!configurationLocked.isInMemoryDb
            && (configurationLocked.openFlags and OPEN_CREATE) != 0
        ) {
            ensureFile(configurationLocked.path)
        }
        try {
            openInner()
        } catch (ex: SQLiteDatabaseCorruptException) {
            onCorruption()
            openInner()
        }
    } catch (ex: SQLiteException) {
        Log.e(TAG, "Failed to open database '$label'.", ex)
        close()
        throw ex
    }

    private fun openInner() = synchronized(lock) {
        check(connectionPoolLocked == null)
        connectionPoolLocked = SQLiteConnectionPool.open(configurationLocked)
        closeGuardLocked!!.open("close")
    }

    override var version: Int
        /**
         * Gets the database version.
         *
         * @return the database version
         */
        get() = longForQuery("PRAGMA user_version;", null).toInt()
        /**
         * Sets the database version.
         *
         * @param version the new database version
         */
        set(version) {
            execSQL("PRAGMA user_version = $version")
        }

    override val maximumSize: Long
        /**
         * Returns the maximum size the database may grow to.
         *
         * @return the new maximum database size
         */
        get() {
            val pageCount = longForQuery("PRAGMA max_page_count;", null)
            return pageCount * pageSize
        }

    /**
     * Sets the maximum size the database will grow to. The maximum size cannot
     * be set below the current size.
     *
     * @param numBytes the maximum database size, in bytes
     * @return the new maximum database size
     */
    override fun setMaximumSize(numBytes: Long): Long {
        val pageSize = pageSize
        var numPages = numBytes / pageSize
        // If numBytes isn't a multiple of pageSize, bump up a page
        if ((numBytes % pageSize) != 0L) {
            numPages++
        }
        val newPageCount = longForQuery("PRAGMA max_page_count = $numPages", null)
        return newPageCount * pageSize
    }

    override var pageSize: Long
        /**
         * Returns the current database page size, in bytes.
         *
         * @return the database page size, in bytes
         */
        get() = longForQuery("PRAGMA page_size;", null)
        /**
         * Sets the database page size. The page size must be a power of two. This
         * method does not work if any data has been written to the database file,
         * and must be called right after the database has been created.
         *
         * @param numBytes the database page size, in bytes
         */
        set(numBytes) = execSQL("PRAGMA page_size = $numBytes")

    /**
     * Compiles an SQL statement into a reusable pre-compiled statement object.
     * The parameters are identical to [.execSQL]. You may put ?s in the
     * statement and fill in those values with [SQLiteProgram.bindString]
     * and [SQLiteProgram.bindLong] each time you want to run the
     * statement. Statements may not return result sets larger than 1x1.
     *
     *
     * No two threads should be using the same [SQLiteStatement] at the same time.
     *
     * @param sql The raw SQL statement, may contain ? for unknown values to be
     * bound later.
     * @return A pre-compiled [SQLiteStatement] object. Note that
     * [SQLiteStatement]s are not synchronized, see the documentation for more details.
     */
    @Throws(SQLException::class)
    override fun compileStatement(sql: String): SQLiteStatement = useReference {
        SQLiteStatement(this, sql, null)
    }

    /**
     * Query the given URL, returning a [Cursor] over the result set.
     *
     * @param cursorFactory the cursor factory to use, or null for the default factory
     * @param distinct true if you want each row to be unique, false otherwise.
     * @param table The table name to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     * return all columns, which is discouraged to prevent reading
     * data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an
     * SQL WHERE clause (excluding the WHERE itself). Passing null
     * will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be
     * replaced by the values from selectionArgs, in order that they
     * appear in the selection.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     * GROUP BY clause (excluding the GROUP BY itself). Passing null
     * will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     * if row grouping is being used, formatted as an SQL HAVING
     * clause (excluding the HAVING itself). Passing null will cause
     * all row groups to be included, and is required when row
     * grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     * (excluding the ORDER BY itself). Passing null will use the
     * default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     * formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then [OperationCanceledException] will be thrown
     * when the query is executed.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     * @see Cursor
     */
    /**
     * Query the given URL, returning a [Cursor] over the result set.
     *
     * @param cursorFactory the cursor factory to use, or null for the default factory
     * @param distinct true if you want each row to be unique, false otherwise.
     * @param table The table name to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     * return all columns, which is discouraged to prevent reading
     * data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an
     * SQL WHERE clause (excluding the WHERE itself). Passing null
     * will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be
     * replaced by the values from selectionArgs, in order that they
     * appear in the selection.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     * GROUP BY clause (excluding the GROUP BY itself). Passing null
     * will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     * if row grouping is being used, formatted as an SQL HAVING
     * clause (excluding the HAVING itself). Passing null will cause
     * all row groups to be included, and is required when row
     * grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     * (excluding the ORDER BY itself). Passing null will use the
     * default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     * formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     * @see Cursor
     */
    @JvmOverloads
    fun queryWithFactory(
        cursorFactory: CursorFactory?,
        distinct: Boolean,
        table: String,
        columns: Array<String?>?,
        selection: String?,
        selectionArgs: Array<Any?>?,
        groupBy: String?,
        having: String?,
        orderBy: String?,
        limit: String?,
        cancellationSignal: CancellationSignal? = null
    ): Cursor = useReference {
        val sql = SQLiteQueryBuilder.buildQueryString(distinct, table, columns, selection, groupBy, having, orderBy, limit)

        rawQueryWithFactory(
            cursorFactory, sql, selectionArgs,
            findEditTable(table), cancellationSignal
        )
    }

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param query the SQL query. The SQL string must not be ; terminated
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    override fun query(
        query: String,
    ): Cursor = rawQueryWithFactory(
        cursorFactory = null,
        sql = query,
        selectionArgs = null,
        editTable = null,
        cancellationSignal = null
    )

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param query the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     * which will be replaced by the values from selectionArgs.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    override fun query(
        query: String,
        bindArgs: Array<out Any?>
    ): Cursor = rawQueryWithFactory(
        cursorFactory = null,
        sql = query,
        selectionArgs = bindArgs,
        editTable = null,
        cancellationSignal = null
    )

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param query the SQL query.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    override fun query(query: SupportSQLiteQuery): Cursor = query(query, signal = null)

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param query the SQL query. The SQL string must not be ; terminated
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then [OperationCanceledException] will be thrown
     * when the query is executed.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    override fun query(
        query: SupportSQLiteQuery,
        cancellationSignal: android.os.CancellationSignal?
    ): Cursor {
        if (cancellationSignal != null) {
            val supportCancellationSignal = CancellationSignal()
            cancellationSignal.setOnCancelListener(supportCancellationSignal::cancel)
            return query(query, supportCancellationSignal)
        } else {
            return query(query, signal = null)
        }
    }

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param supportQuery the SQL query. The SQL string must not be ; terminated
     * @param signal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then [OperationCanceledException] will be thrown
     * when the query is executed.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    fun query(
        supportQuery: SupportSQLiteQuery,
        signal: CancellationSignal?
    ): Cursor = rawQueryWithFactory(
        { db, masterQuery, editTable, query ->
            supportQuery.bindTo(query)
            cursorFactory?.newCursor(
                db = db,
                masterQuery = masterQuery,
                editTable = editTable,
                query = query
            ) ?: SQLiteCursor(masterQuery, editTable, query)
        },
        supportQuery.sql, arrayOf(), null, signal
    )

    /**
     * Runs the provided SQL and returns a cursor over the result set.
     *
     * @param cursorFactory the cursor factory to use, or null for the default factory
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     * which will be replaced by the values from selectionArgs.
     * @param editTable the name of the first table, which is editable
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then [OperationCanceledException] will be thrown
     * when the query is executed.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    /**
     * Runs the provided SQL and returns a cursor over the result set.
     *
     * @param cursorFactory the cursor factory to use, or null for the default factory
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     * which will be replaced by the values from selectionArgs.
     * @param editTable the name of the first table, which is editable
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    @JvmOverloads
    fun rawQueryWithFactory(
        cursorFactory: CursorFactory?,
        sql: String?,
        selectionArgs: Array<out Any?>?,
        editTable: String?,
        cancellationSignal: CancellationSignal? = null
    ): Cursor = useReference {
        val driver: SQLiteCursorDriver = SQLiteDirectCursorDriver(this, sql, editTable, cancellationSignal)
        return driver.query(cursorFactory ?: this.cursorFactory, selectionArgs)
    }

    /**
     * General method for inserting a row into the database.
     *
     * @param table the table to insert the row into
     * @param conflictAlgorithm for insert conflict resolver
     * @param values this map contains the initial column values for the
     * row. The keys should be the column names and the values the
     * column values
     * @return the row ID of the newly inserted row
     * OR the primary key of the existing row if the input param 'conflictAlgorithm' =
     * [.CONFLICT_IGNORE]
     * OR -1 if any error
     */
    @Throws(SQLException::class)
    override fun insert(
        table: String,
        @ConflictAlgorithm conflictAlgorithm: Int,
        values: ContentValues
    ): Long {
        return insertWithOnConflict(table, null, values, conflictAlgorithm)
    }

    /**
     * General method for inserting a row into the database.
     *
     * @param table the table to insert the row into
     * @param nullColumnHack optional; may be `null`.
     * SQL doesn't allow inserting a completely empty row without
     * naming at least one column name.  If your provided `initialValues` is
     * empty, no column names are known and an empty row can't be inserted.
     * If not set to null, the `nullColumnHack` parameter
     * provides the name of nullable column name to explicitly insert a NULL into
     * in the case where your `initialValues` is empty.
     * @param initialValues this map contains the initial column values for the
     * row. The keys should be the column names and the values the
     * column values
     * @param conflictAlgorithm for insert conflict resolver
     * @return the row ID of the newly inserted row
     * OR the primary key of the existing row if the input param 'conflictAlgorithm' =
     * [.CONFLICT_IGNORE]
     * OR -1 if any error
     */
    fun insertWithOnConflict(
        table: String?,
        nullColumnHack: String?,
        initialValues: ContentValues?,
        @ConflictAlgorithm conflictAlgorithm: Int
    ): Long = useReference {
        val sql = StringBuilder()
        sql.append("INSERT")
        sql.append(CONFLICT_VALUES[conflictAlgorithm])
        sql.append(" INTO ")
        sql.append(table)
        sql.append('(')

        var bindArgs: Array<Any?>? = null
        val size = if ((initialValues != null && initialValues.size() > 0)
        ) initialValues.size() else 0
        if (size > 0) {
            bindArgs = arrayOfNulls(size)
            var i = 0
            for ((key, value) in initialValues!!.valueSet()) {
                sql.append(if ((i > 0)) "," else "")
                sql.append(key)
                bindArgs[i++] = value
            }
            sql.append(')')
            sql.append(" VALUES (")
            i = 0
            while (i < size) {
                sql.append(if ((i > 0)) ",?" else "?")
                i++
            }
        } else {
            sql.append("$nullColumnHack) VALUES (NULL")
        }
        sql.append(')')

        return SQLiteStatement(this, sql.toString(), bindArgs).use { it.executeInsert() }
    }

    /**
     * Convenience method for deleting rows in the database.
     *
     * @param table the table to delete from
     * @param whereClause the optional WHERE clause to apply when deleting.
     * Passing null will delete all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @return the number of rows affected if a whereClause is passed in, 0
     * otherwise. To remove all rows and get a count pass "1" as the
     * whereClause.
     */
    fun delete(table: String, whereClause: String, whereArgs: Array<String?>?): Int = useReference {
        SQLiteStatement(
            this,
            "DELETE FROM $table" + (if (whereClause.isNotEmpty()) " WHERE $whereClause" else ""),
            whereArgs
        ).use {
            it.executeUpdateDelete()
        }
    }

    /**
     * Convenience method for deleting rows in the database.
     *
     * @param table the table to delete from
     * @param whereClause the optional WHERE clause to apply when deleting.
     * Passing null will delete all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @return the number of rows affected if a whereClause is passed in, 0
     * otherwise. To remove all rows and get a count pass "1" as the
     * whereClause.
     */
    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int = useReference {
        SQLiteStatement(
            this,
            "DELETE FROM " + table + (if (whereClause?.isNotEmpty() == true) " WHERE $whereClause" else ""),
            whereArgs
        ).use {
            it.executeUpdateDelete()
        }
    }

    /**
     * Convenience method for updating rows in the database.
     *
     * @param table the table to update in
     * @param values a map from column names to new column values. null is a
     * valid value that will be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating.
     * Passing null will update all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @return the number of rows affected
     */
    fun update(table: String?, values: ContentValues?, whereClause: String?, whereArgs: Array<String?>?): Int {
        return updateWithOnConflict(table, values, whereClause, whereArgs, CONFLICT_NONE)
    }

    /**
     * Convenience method for updating rows in the database.
     *
     * @param table the table to update in
     * @param values a map from column names to new column values. null is a
     * valid value that will be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating.
     * Passing null will update all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @param conflictAlgorithm for update conflict resolver
     * @return the number of rows affected
     */
    override fun update(
        table: String,
        @ConflictAlgorithm conflictAlgorithm: Int,
        values: ContentValues,
        whereClause: String?,
        whereArgs: Array<out Any?>?
    ): Int = useReference {
        require(values.size() != 0) { "Empty values" }

        val sql = StringBuilder(120)
        sql.append("UPDATE ")
        sql.append(CONFLICT_VALUES[conflictAlgorithm])
        sql.append(table)
        sql.append(" SET ")

        // move all bind args to one array
        val setValuesSize = values.size()
        val bindArgsSize = if ((whereArgs == null)) setValuesSize else (setValuesSize + whereArgs.size)
        val bindArgs = arrayOfNulls<Any>(bindArgsSize)
        var i = 0
        for ((key, value) in values.valueSet()) {
            sql.append(if ((i > 0)) "," else "")
            sql.append(key)
            bindArgs[i++] = value
            sql.append("=?")
        }
        if (whereArgs != null) {
            i = setValuesSize
            while (i < bindArgsSize) {
                bindArgs[i] = whereArgs[i - setValuesSize]
                i++
            }
        }
        if (whereClause?.isNotEmpty() == true) {
            sql.append(" WHERE ")
            sql.append(whereClause)
        }

        return SQLiteStatement(this, sql.toString(), bindArgs).use(SQLiteStatement::executeUpdateDelete)
    }

    /**
     * Convenience method for updating rows in the database.
     *
     * @param table the table to update in
     * @param values a map from column names to new column values. null is a
     * valid value that will be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating.
     * Passing null will update all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @param conflictAlgorithm for update conflict resolver
     * @return the number of rows affected
     */
    fun updateWithOnConflict(
        table: String?,
        values: ContentValues?,
        whereClause: String?,
        whereArgs: Array<String?>?,
        @ConflictAlgorithm conflictAlgorithm: Int,
    ): Int = useReference {
        require(!(values == null || values.size() == 0)) { "Empty values" }

        val sql = StringBuilder(120)
        sql.append("UPDATE ")
        sql.append(CONFLICT_VALUES[conflictAlgorithm])
        sql.append(table)
        sql.append(" SET ")

        // move all bind args to one array
        val setValuesSize = values.size()
        val bindArgsSize = if ((whereArgs == null)) setValuesSize else (setValuesSize + whereArgs.size)
        val bindArgs = arrayOfNulls<Any>(bindArgsSize)
        var i = 0
        for ((key, value) in values.valueSet()) {
            sql.append(if ((i > 0)) "," else "")
            sql.append(key)
            bindArgs[i++] = value
            sql.append("=?")
        }
        if (whereArgs != null) {
            i = setValuesSize
            while (i < bindArgsSize) {
                bindArgs[i] = whereArgs[i - setValuesSize]
                i++
            }
        }
        if (whereClause?.isNotEmpty() == true) {
            sql.append(" WHERE ")
            sql.append(whereClause)
        }

        return SQLiteStatement(this, sql.toString(), bindArgs).use(SQLiteStatement::executeUpdateDelete)
    }

    /**
     * Execute a single SQL statement that is NOT a SELECT
     * or any other SQL statement that returns data.
     *
     *
     * It has no means to return any data (such as the number of affected rows).
     * Instead, you're encouraged to use [.insert],
     * [.update], et al, when possible.
     *
     *
     *
     * When using [.enableWriteAheadLogging], journal_mode is
     * automatically managed by this class. So, do not set journal_mode
     * using "PRAGMA journal_mode'<value>" statement if your app is using
     * [.enableWriteAheadLogging]
    </value> *
     *
     * @param sql the SQL statement to be executed. Multiple statements separated by semicolons are
     * not supported.
     * @throws SQLException if the SQL string is invalid
     */
    @Throws(SQLException::class)
    override fun execSQL(sql: String) {
        executeSql(sql, null)
    }

    /**
     * Execute a single SQL statement that is NOT a SELECT/INSERT/UPDATE/DELETE.
     *
     *
     * For INSERT statements, use any of the following instead.
     *
     *  * [.insert]
     *  * [.insertOrThrow]
     *  * [.insertWithOnConflict]
     *
     *
     *
     * For UPDATE statements, use any of the following instead.
     *
     *  * [.update]
     *  * [.updateWithOnConflict]
     *
     *
     *
     * For DELETE statements, use any of the following instead.
     *
     *  * [.delete]
     *
     *
     *
     * For example, the following are good candidates for using this method:
     *
     *  * ALTER TABLE
     *  * CREATE or DROP table / trigger / view / index / virtual table
     *  * REINDEX
     *  * RELEASE
     *  * SAVEPOINT
     *  * PRAGMA that returns no data
     *
     * When using [.enableWriteAheadLogging], journal_mode is
     * automatically managed by this class. So, do not set journal_mode
     * using "PRAGMA journal_mode'<value>" statement if your app is using
     * [.enableWriteAheadLogging]
    </value> *
     *
     * @param sql the SQL statement to be executed. Multiple statements separated by semicolons are
     * not supported.
     * @param bindArgs only byte[], String, Long and Double are supported in bindArgs.
     * @throws SQLException if the SQL string is invalid
     */
    @Throws(SQLException::class)
    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        executeSql(sql, bindArgs)
    }

    @Throws(SQLException::class)
    private fun executeSql(sql: String, bindArgs: Array<out Any?>?): Int = useReference {
        SQLiteStatement(this, sql, bindArgs).use(SQLiteStatement::executeUpdateDelete)
    }

    /**
     * Verifies that a SQL SELECT statement is valid by compiling it.
     * If the SQL statement is not valid, this method will throw a [SQLiteException].
     *
     * @param sql SQL to be validated
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then [OperationCanceledException] will be thrown
     * when the query is executed.
     * @throws SQLiteException if `sql` is invalid
     */
    fun validateSql(sql: String, cancellationSignal: CancellationSignal?) {
        threadSession.prepare(
            sql,
            getThreadDefaultConnectionFlags(true), cancellationSignal, null
        )
    }

    /**
     * Returns true if the database is opened as read only.
     *
     * @return True if database is opened as read only.
     */
    override val isReadOnly: Boolean
        get() = synchronized(lock) { isReadOnlyLocked }

    private val isReadOnlyLocked: Boolean
        get() = (configurationLocked.openFlags and OPEN_READONLY) == OPEN_READONLY

    /**
     * Returns true if the database is in-memory db.
     *
     * @return True if the database is in-memory.
     * @hide
     */
    val isInMemoryDatabase: Boolean
        get() = synchronized(lock) { configurationLocked.isInMemoryDb }

    /**
     * Returns true if the database is currently open.
     *
     * @return True if the database is currently open (has not been closed).
     */
    override val isOpen: Boolean
        get() = synchronized(lock) {
            connectionPoolLocked != null
        }

    /**
     * Returns true if the new version code is greater than the current database version.
     *
     * @param newVersion The new version code.
     * @return True if the new version code is greater than the current database version.
     */
    override fun needUpgrade(newVersion: Int): Boolean = newVersion > version

    /**
     * Gets the path to the database file.
     *
     * @return The path to the database file.
     */
    override val path: String?
        get() = synchronized(lock) {
            configurationLocked.path.takeIf { it != MEMORY_DB_PATH }
        }

    /**
     * Sets the locale for this database.
     *
     * @param locale The new locale.
     *
     * @throws SQLException if the locale could not be set.  The most common reason
     * for this is that there is no collator available for the locale you requested.
     * In this case the database remains unchanged.
     */
    override fun setLocale(locale: Locale) = synchronized(lock) {
        throwIfNotOpenLocked()
        val oldLocale = configurationLocked.locale
        configurationLocked.locale = locale
        try {
            connectionPoolLocked!!.reconfigure(configurationLocked)
        } catch (ex: RuntimeException) {
            configurationLocked.locale = oldLocale
            throw ex
        }
    }

    /**
     * Sets the maximum size of the prepared-statement cache for this database.
     * (size of the cache = number of compiled-sql-statements stored in the cache).
     *
     *
     * Maximum cache size can ONLY be increased from its current size (default = 10).
     * If this method is called with smaller size than the current maximum value,
     * then IllegalStateException is thrown.
     *
     *
     * This method is thread-safe.
     *
     * @param cacheSize the size of the cache. can be (0 to [.MAX_SQL_CACHE_SIZE])
     * @throws IllegalStateException if input cacheSize > [.MAX_SQL_CACHE_SIZE].
     */
    override fun setMaxSqlCacheSize(cacheSize: Int) {
        check(!(cacheSize > MAX_SQL_CACHE_SIZE || cacheSize < 0)) { "expected value between 0 and $MAX_SQL_CACHE_SIZE" }

        synchronized(lock) {
            throwIfNotOpenLocked()
            val oldMaxSqlCacheSize = configurationLocked.maxSqlCacheSize
            configurationLocked.maxSqlCacheSize = cacheSize
            try {
                connectionPoolLocked!!.reconfigure(configurationLocked)
            } catch (ex: RuntimeException) {
                configurationLocked.maxSqlCacheSize = oldMaxSqlCacheSize
                throw ex
            }
        }
    }

    /**
     * Sets whether foreign key constraints are enabled for the database.
     *
     *
     * By default, foreign key constraints are not enforced by the database.
     * This method allows an application to enable foreign key constraints.
     * It must be called each time the database is opened to ensure that foreign
     * key constraints are enabled for the session.
     *
     *
     * A good time to call this method is right after calling [.openOrCreateDatabase]
     * or in the [SQLiteOpenHelper.onConfigure] callback.
     *
     *
     * When foreign key constraints are disabled, the database does not check whether
     * changes to the database will violate foreign key constraints.  Likewise, when
     * foreign key constraints are disabled, the database will not execute cascade
     * delete or update triggers.  As a result, it is possible for the database
     * state to become inconsistent.  To perform a database integrity check,
     * call [.isDatabaseIntegrityOk].
     *
     *
     * This method must not be called while a transaction is in progress.
     *
     *
     * See also [SQLite Foreign Key Constraints](http://sqlite.org/foreignkeys.html)
     * for more details about foreign key constraint support.
     *
     *
     * @param enabled True to enable foreign key constraints, false to disable them.
     *
     * @throws IllegalStateException if the are transactions is in progress
     * when this method is called.
     */
    override fun setForeignKeyConstraintsEnabled(enabled: Boolean) = synchronized(lock) {
        throwIfNotOpenLocked()
        if (configurationLocked.foreignKeyConstraintsEnabled == enabled) {
            return
        }

        configurationLocked.foreignKeyConstraintsEnabled = enabled
        try {
            connectionPoolLocked!!.reconfigure(configurationLocked)
        } catch (ex: RuntimeException) {
            configurationLocked.foreignKeyConstraintsEnabled = !enabled
            throw ex
        }
    }

    /**
     * This method enables parallel execution of queries from multiple threads on the
     * same database.  It does this by opening multiple connections to the database
     * and using a different database connection for each query.  The database
     * journal mode is also changed to enable writes to proceed concurrently with reads.
     *
     *
     * When write-ahead logging is not enabled (the default), it is not possible for
     * reads and writes to occur on the database at the same time.  Before modifying the
     * database, the writer implicitly acquires an exclusive lock on the database which
     * prevents readers from accessing the database until the write is completed.
     *
     *
     * In contrast, when write-ahead logging is enabled (by calling this method), write
     * operations occur in a separate log file which allows reads to proceed concurrently.
     * While a write is in progress, readers on other threads will perceive the state
     * of the database as it was before the write began.  When the write completes, readers
     * on other threads will then perceive the new state of the database.
     *
     *
     * It is a good idea to enable write-ahead logging whenever a database will be
     * concurrently accessed and modified by multiple threads at the same time.
     * However, write-ahead logging uses significantly more memory than ordinary
     * journaling because there are multiple connections to the same database.
     * So if a database will only be used by a single thread, or if optimizing
     * concurrency is not very important, then write-ahead logging should be disabled.
     *
     *
     * After calling this method, execution of queries in parallel is enabled as long as
     * the database remains open.  To disable execution of queries in parallel, either
     * call [.disableWriteAheadLogging] or close the database and reopen it.
     *
     *
     * The maximum number of connections used to execute queries in parallel is
     * dependent upon the device memory and possibly other properties.
     *
     *
     * If a query is part of a transaction, then it is executed on the same database handle the
     * transaction was begun.
     *
     *
     * Writers should use [.beginTransactionNonExclusive] or
     * [.beginTransactionWithListenerNonExclusive]
     * to start a transaction.  Non-exclusive mode allows database file to be in readable
     * by other threads executing queries.
     *
     *
     * If the database has any attached databases, then execution of queries in parallel is NOT
     * possible.  Likewise, write-ahead logging is not supported for read-only databases
     * or memory databases.  In such cases, [.enableWriteAheadLogging] returns false.
     *
     *
     * The best way to enable write-ahead logging is to pass the
     * [.ENABLE_WRITE_AHEAD_LOGGING] flag to [.openDatabase].  This is
     * more efficient than calling [.enableWriteAheadLogging].
     * `<pre>
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
     * myDatabaseErrorHandler);
     * db.enableWriteAheadLogging();
    </pre>` *
     *
     *
     * Another way to enable write-ahead logging is to call [.enableWriteAheadLogging]
     * after opening the database.
     * `<pre>
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY, myDatabaseErrorHandler);
     * db.enableWriteAheadLogging();
    </pre>` *
     *
     *
     * See also [SQLite Write-Ahead Logging](http://sqlite.org/wal.html) for
     * more details about how write-ahead logging works.
     *
     * @return True if write-ahead logging is enabled.
     *
     * @throws IllegalStateException if there are transactions in progress at the
     * time this method is called.  WAL mode can only be changed when there are no
     * transactions in progress.
     *
     * @see .ENABLE_WRITE_AHEAD_LOGGING
     * @see .disableWriteAheadLogging
     */
    override fun enableWriteAheadLogging(): Boolean = synchronized(lock) {
        throwIfNotOpenLocked()
        if ((configurationLocked.openFlags and ENABLE_WRITE_AHEAD_LOGGING) != 0) {
            return true
        }

        if (isReadOnlyLocked) {
            // WAL doesn't make sense for readonly-databases.
            // TODO: True, but connection pooling does still make sense...
            return false
        }

        if (configurationLocked.isInMemoryDb) {
            Log.i(TAG, "can't enable WAL for memory databases.")
            return false
        }

        configurationLocked.openFlags = configurationLocked.openFlags or ENABLE_WRITE_AHEAD_LOGGING
        try {
            connectionPoolLocked!!.reconfigure(configurationLocked)
        } catch (ex: RuntimeException) {
            configurationLocked.openFlags = configurationLocked.openFlags and ENABLE_WRITE_AHEAD_LOGGING.inv()
            throw ex
        }
        return true
    }

    /**
     * This method disables the features enabled by [.enableWriteAheadLogging].
     *
     * @throws IllegalStateException if there are transactions in progress at the
     * time this method is called.  WAL mode can only be changed when there are no
     * transactions in progress.
     *
     * @see .enableWriteAheadLogging
     */
    override fun disableWriteAheadLogging() = synchronized(lock) {
        throwIfNotOpenLocked()
        if ((configurationLocked.openFlags and ENABLE_WRITE_AHEAD_LOGGING) == 0) {
            return
        }

        configurationLocked.openFlags = configurationLocked.openFlags and ENABLE_WRITE_AHEAD_LOGGING.inv()
        try {
            connectionPoolLocked!!.reconfigure(configurationLocked)
        } catch (ex: RuntimeException) {
            configurationLocked.openFlags = configurationLocked.openFlags or ENABLE_WRITE_AHEAD_LOGGING
            throw ex
        }
    }

    /**
     * Returns true if write-ahead logging has been enabled for this database.
     *
     * @see .enableWriteAheadLogging
     * @see .ENABLE_WRITE_AHEAD_LOGGING
     */
    override val isWriteAheadLoggingEnabled: Boolean
        get() {
            synchronized(lock) {
                throwIfNotOpenLocked()
                return (configurationLocked.openFlags and ENABLE_WRITE_AHEAD_LOGGING) != 0
            }
        }

    private fun collectDbStats(dbStatsList: ArrayList<SQLiteDebug.DbStats>) {
        synchronized(lock) {
            connectionPoolLocked?.collectDbStats(dbStatsList)
        }
    }

    private fun dump(printer: Printer, verbose: Boolean) = synchronized(lock) {
        connectionPoolLocked?.let {
            printer.println("")
            it.dump(printer, verbose)
        }
    }

    override val attachedDbs: List<Pair<String, String>>?
        /**
         * Returns list of full pathnames of all attached databases including the main database
         * by executing 'pragma database_list' on the database.
         *
         * @return ArrayList of pairs of (database name, database file path) or null if the database
         * is not open.
         */
        get() {
            val attachedDbs = ArrayList<Pair<String, String>>()
            synchronized(lock) {
                if (connectionPoolLocked == null) {
                    return null // not open
                }
                acquireReference()
            }

            try {
                // has attached databases. query sqlite to get the list of attached databases.
                rawQuery("pragma database_list;", null).use { c ->
                    while (c.moveToNext()) {
                        // sqlite returns a row for each database in the returned list of databases.
                        //   in each row,
                        //       1st column is the database name such as main, or the database
                        //                              name specified on the "ATTACH" command
                        //       2nd column is the database file path.
                        attachedDbs.add(Pair(c.getString(1), c.getString(2)))
                    }
                }
                return attachedDbs
            } finally {
                releaseReference()
            }
        }

    /**
     * Runs 'pragma integrity_check' on the given database (and all the attached databases)
     * and returns true if the given database (and all its attached databases) pass integrity_check,
     * false otherwise.
     *
     * If the result is false, then this method logs the errors reported by the integrity_check
     * command execution.
     *
     * Note that 'pragma integrity_check' on a database can take a long time.
     *
     * @return true if the given database (and all its attached databases) pass integrity_check,
     * false otherwise.
     */
    override val isDatabaseIntegrityOk: Boolean
        get() = useReference {
            val attachedDbs = try {
                checkNotNull(this.attachedDbs) {
                    "databaselist for: $path couldn't be retrieved. probably because the database is closed"
                }
            } catch (e: SQLiteException) {
                // can't get attachedDb list. do integrity check on the main database
                listOf(Pair("main", path))
            }

            attachedDbs.forEach { p ->
                compileStatement("PRAGMA ${p.first}.integrity_check(1);").use { prog ->
                    val rslt = prog.simpleQueryForString()
                    if (!rslt.equals("ok", ignoreCase = true)) {
                        // integrity_checker failed on main or attached databases
                        Log.e(TAG, "PRAGMA integrity_check on " + p.second + " returned: " + rslt)
                        return false
                    }
                }
            }
            return true
        }

    override fun toString(): String = "SQLiteDatabase: $path"

    private fun throwIfNotOpenLocked() {
        checkNotNull(connectionPoolLocked) { "The database '${configurationLocked.label}' is not open." }
    }

    /**
     * Used to allow returning sub-classes of [Cursor] when calling query.
     */
    fun interface CursorFactory {
        /**
         * See [SQLiteCursor.SQLiteCursor].
         */
        fun newCursor(
            db: SQLiteDatabase,
            masterQuery: SQLiteCursorDriver?,
            editTable: String?,
            query: SQLiteQuery
        ): Cursor
    }

    /**
     * A callback interface for a custom sqlite3 function. This can be used to create a function
     * that can be called from sqlite3 database triggers, or used in queries.
     */
    interface Function {
        interface Args {
            fun getBlob(arg: Int): ByteArray?
            fun getString(arg: Int): String?
            fun getDouble(arg: Int): Double
            fun getInt(arg: Int): Int
            fun getLong(arg: Int): Long
        }

        interface Result {
            fun set(value: ByteArray?)
            fun set(value: Double)
            fun set(value: Int)
            fun set(value: Long)
            fun set(value: String?)
            fun setError(error: String?)
            fun setNull()
        }

        /**
         * Invoked whenever the function is called.
         * @param args function arguments
         * @return String value of the result or null
         */
        fun callback(args: Args?, result: Result?)

        companion object {
            /**
             * Flag that declares this function to be "deterministic,"
             * which means it may be used with Indexes on Expressions.
             */
            const val FLAG_DETERMINISTIC: Int = 0x800
        }
    }

    fun enableLocalizedCollators() = connectionPoolLocked!!.enableLocalizedCollators()

    /**
     * Query the table for the number of rows in the table.
     * @param table the name of the table to query
     * @param selection A filter declaring which rows to return,
     * formatted as an SQL WHERE clause (excluding the WHERE itself).
     * Passing null will count all rows for the given table
     * @param selectionArgs You may include ?s in selection,
     * which will be replaced by the values from selectionArgs,
     * in order that they appear in the selection.
     * The values will be bound as Strings.
     * @return the number of rows in the table filtered by the selection
     */
    /**
     * Query the table for the number of rows in the table.
     * @param table the name of the table to query
     * @return the number of rows in the table
     */
    /**
     * Query the table for the number of rows in the table.
     * @param table the name of the table to query
     * @param selection A filter declaring which rows to return,
     * formatted as an SQL WHERE clause (excluding the WHERE itself).
     * Passing null will count all rows for the given table
     * @return the number of rows in the table filtered by the selection
     */
    @JvmOverloads
    fun queryNumEntries(table: String, selection: String? = null, selectionArgs: Array<String?>? = null): Long {
        val s = if (selection?.isNotEmpty() == true) " where $selection" else ""
        return longForQuery("select count(*) from $table$s", selectionArgs)
    }

    /**
     * Utility method to run the query on the db and return the value in the
     * first column of the first row.
     */
    fun longForQuery(query: String, selectionArgs: Array<String?>?): Long = compileStatement(query).use { prog ->
        longForQuery(prog, selectionArgs)
    }

    /**
     * Utility method to run the query on the db and return the value in the
     * first column of the first row.
     */
    fun stringForQuery(query: String, selectionArgs: Array<String?>?): String? = compileStatement(query).use { prog ->
        stringForQuery(prog, selectionArgs)
    }

    /**
     * Utility method to run the query on the db and return the blob value in the
     * first column of the first row.
     *
     * @return A read-only file descriptor for a copy of the blob value.
     */
    fun blobFileDescriptorForQuery(
        query: String,
        selectionArgs: Array<String?>?
    ): ParcelFileDescriptor {
        val prog = compileStatement(query)
        try {
            return blobFileDescriptorForQuery(prog, selectionArgs)
        } finally {
            prog.close()
        }
    }

    companion object {
        private const val TAG = "SQLiteDatabase"

        private const val EVENT_DB_CORRUPT = 75004

        /**
         * When a constraint violation occurs, an immediate ROLLBACK occurs,
         * thus ending the current transaction, and the command aborts with a
         * return code of SQLITE_CONSTRAINT. If no transaction is active
         * (other than the implied transaction that is created on every command)
         * then this algorithm works the same as ABORT.
         */
        const val CONFLICT_ROLLBACK: Int = 1

        /**
         * When a constraint violation occurs,no ROLLBACK is executed
         * so changes from prior commands within the same transaction
         * are preserved. This is the default behavior.
         */
        const val CONFLICT_ABORT: Int = 2

        /**
         * When a constraint violation occurs, the command aborts with a return
         * code SQLITE_CONSTRAINT. But any changes to the database that
         * the command made prior to encountering the constraint violation
         * are preserved and are not backed out.
         */
        const val CONFLICT_FAIL: Int = 3

        /**
         * When a constraint violation occurs, the one row that contains
         * the constraint violation is not inserted or changed.
         * But the command continues executing normally. Other rows before and
         * after the row that contained the constraint violation continue to be
         * inserted or updated normally. No error is returned.
         */
        const val CONFLICT_IGNORE: Int = 4

        /**
         * When a UNIQUE constraint violation occurs, the pre-existing rows that
         * are causing the constraint violation are removed prior to inserting
         * or updating the current row. Thus the insert or update always occurs.
         * The command continues executing normally. No error is returned.
         * If a NOT NULL constraint violation occurs, the NULL value is replaced
         * by the default value for that column. If the column has no default
         * value, then the ABORT algorithm is used. If a CHECK constraint
         * violation occurs then the IGNORE algorithm is used. When this conflict
         * resolution strategy deletes rows in order to satisfy a constraint,
         * it does not invoke delete triggers on those rows.
         * This behavior might change in a future release.
         */
        const val CONFLICT_REPLACE: Int = 5

        /**
         * Use the following when no conflict action is specified.
         */
        const val CONFLICT_NONE: Int = 0

        private val CONFLICT_VALUES =
            arrayOf("", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE ")

        /** Open flag to open in the database in read only mode  */
        const val OPEN_READONLY: Int = 0x00000001

        /** Open flag to open in the database in read/write mode  */
        const val OPEN_READWRITE: Int = 0x00000002

        /** Open flag to create the database if it does not exist  */
        const val OPEN_CREATE: Int = 0x00000004

        /** Open flag to support URI filenames  */
        const val OPEN_URI: Int = 0x00000040

        /** Open flag opens the database in multi-thread threading mode  */
        const val OPEN_NOMUTEX: Int = 0x00008000

        /** Open flag opens the database in serialized threading mode  */
        const val OPEN_FULLMUTEX: Int = 0x00010000

        /** Open flag opens the database in shared cache mode  */
        const val OPEN_SHAREDCACHE: Int = 0x00020000

        /** Open flag opens the database in private cache mode  */
        const val OPEN_PRIVATECACHE: Int = 0x00040000

        /** Open flag equivalent to [.OPEN_READWRITE] | [.OPEN_CREATE]  */
        const val CREATE_IF_NECESSARY: Int = OPEN_READWRITE or OPEN_CREATE

        /** Open flag to enable write-ahead logging  */ // custom flag remove for sqlite3_open_v2
        const val ENABLE_WRITE_AHEAD_LOGGING: Int = 0x20000000

        /**
         * Absolute max value that can be set by [.setMaxSqlCacheSize].
         *
         * Each prepared-statement is between 1K - 6K, depending on the complexity of the
         * SQL statement & schema.  A large SQL cache may use a significant amount of memory.
         */
        const val MAX_SQL_CACHE_SIZE: Int = 100

        /**
         * Attempts to release memory that SQLite holds but does not require to
         * operate properly. Typically this memory will come from the page cache.
         *
         * @return the number of bytes actually released
         */
        fun releaseMemory(): Int {
            return SQLiteGlobal.releaseMemory()
        }

        private val isMainThread: Boolean
            get() = Looper.getMainLooper().isCurrentThread

        /**
         * Open the database according to the flags [OpenFlags]
         *
         *
         * Sets the locale of the database to the  the system's current locale.
         * Call [.setLocale] if you would like something else.
         *
         *
         * Accepts input param: a concrete instance of [DatabaseErrorHandler] to be
         * used to handle corruption when sqlite reports database corruption.
         *
         * @param path to database file to open and/or create
         * @param factory an optional factory class that is called to instantiate a
         * cursor when query is called, or null for default
         * @param flags to control database access mode
         * @param errorHandler the [DatabaseErrorHandler] obj to be used to handle corruption
         * when sqlite reports database corruption
         * @return the newly opened database
         * @throws SQLiteException if the database cannot be opened
         */
        /**
         * Open the database according to the flags [OpenFlags]
         *
         *
         * Sets the locale of the database to the  the system's current locale.
         * Call [.setLocale] if you would like something else.
         *
         * @param path to database file to open and/or create
         * @param factory an optional factory class that is called to instantiate a
         * cursor when query is called, or null for default
         * @param flags to control database access mode
         * @return the newly opened database
         * @throws SQLiteException if the database cannot be opened
         */
        @JvmStatic
        @JvmOverloads
        fun openDatabase(
            path: String,
            factory: CursorFactory?,
            @OpenFlags flags: Int,
            errorHandler: DatabaseErrorHandler? = null
        ): SQLiteDatabase {
            val configuration = SQLiteDatabaseConfiguration(path, flags)
            val db = SQLiteDatabase(configuration, factory, errorHandler)
            db.open()
            return db
        }

        /**
         * Open the database according to the given configuration.
         *
         *
         * Sets the locale of the database to the  the system's current locale.
         * Call [.setLocale] if you would like something else.
         *
         *
         * Accepts input param: a concrete instance of [DatabaseErrorHandler] to be
         * used to handle corruption when sqlite reports database corruption.
         *
         * @param configuration to database configuration to use
         * @param factory an optional factory class that is called to instantiate a
         * cursor when query is called, or null for default
         * @param errorHandler the [DatabaseErrorHandler] obj to be used to handle corruption
         * when sqlite reports database corruption
         * @return the newly opened database
         * @throws SQLiteException if the database cannot be opened
         */
        fun openDatabase(
            configuration: SQLiteDatabaseConfiguration,
            factory: CursorFactory?,
            errorHandler: DatabaseErrorHandler?
        ): SQLiteDatabase {
            val db = SQLiteDatabase(configuration, factory, errorHandler)
            db.open()
            return db
        }

        /**
         * Equivalent to openDatabase(file.getPath(), factory, CREATE_IF_NECESSARY).
         */
        fun openOrCreateDatabase(
            file: File,
            factory: CursorFactory
        ): SQLiteDatabase = openOrCreateDatabase(file.path, factory)

        /**
         * Equivalent to openDatabase(path, factory, CREATE_IF_NECESSARY).
         */
        fun openOrCreateDatabase(
            path: String,
            factory: CursorFactory
        ): SQLiteDatabase = openDatabase(
            path = path,
            factory = factory,
            flags = CREATE_IF_NECESSARY,
            errorHandler = null
        )

        /**
         * Equivalent to openDatabase(path, factory, CREATE_IF_NECESSARY, errorHandler).
         */
        @JvmStatic
        fun openOrCreateDatabase(
            path: String,
            factory: CursorFactory,
            errorHandler: DatabaseErrorHandler?
        ): SQLiteDatabase = openDatabase(path, factory, CREATE_IF_NECESSARY, errorHandler)

        /**
         * Deletes a database including its journal file and other auxiliary files
         * that may have been created by the database engine.
         *
         * @param file The database file path.
         * @return True if the database was successfully deleted.
         */
        @JvmStatic
        fun deleteDatabase(file: File?): Boolean {
            requireNotNull(file) { "file must not be null" }

            var deleted: Boolean
            deleted = file.delete()
            deleted = deleted or File(file.path + "-journal").delete()
            deleted = deleted or File(file.path + "-shm").delete()
            deleted = deleted or File(file.path + "-wal").delete()

            val dir = file.parentFile
            if (dir != null) {
                val prefix = file.name + "-mj"
                val filter = FileFilter { candidate -> candidate.name.startsWith(prefix) }
                dir.listFiles(filter)?.forEach { masterJournal ->
                    deleted = deleted or masterJournal.delete()
                }
            }
            return deleted
        }

        private fun ensureFile(path: String) {
            val file = File(path)
            if (!file.exists()) {
                try {
                    val created = file.parentFile?.let { dir ->
                        if (!dir.exists()) {
                            dir.mkdirs()
                        } else {
                            true
                        }
                    } ?: false
                    if (!created) {
                        // Fixes #103: Check parent directory's existence before
                        // attempting to create.
                        Log.e(TAG, "Couldn't mkdirs $file")
                    }
                    if (!file.createNewFile()) {
                        Log.e(TAG, "Couldn't create $file")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Couldn't ensure file $file", e)
                }
            }
        }

        /**
         * Create a memory backed SQLite database.  Its contents will be destroyed
         * when the database is closed.
         *
         * Sets the locale of the database to the the system's current locale.
         * Call [.setLocale] if you would like something else.
         *
         * @param factory an optional factory class that is called to instantiate a
         * cursor when query is called
         * @return a SQLiteDatabase object, or null if the database can't be created
         */
        fun create(factory: CursorFactory?): SQLiteDatabase = openDatabase(MEMORY_DB_PATH, factory, CREATE_IF_NECESSARY)

        /**
         * Finds the name of the first table, which is editable.
         *
         * @param tables a list of tables
         * @return the first table listed
         */
        @JvmStatic
        fun findEditTable(tables: String): String {
            if (tables.isNotEmpty()) {
                // find the first word terminated by either a space or a comma
                val spacepos = tables.indexOf(' ')
                val commapos = tables.indexOf(',')

                if (spacepos > 0 && (spacepos < commapos || commapos < 0)) {
                    return tables.substring(0, spacepos)
                } else if (commapos > 0 && (commapos < spacepos || spacepos < 0)) {
                    return tables.substring(0, commapos)
                }
                return tables
            } else {
                throw IllegalStateException("Invalid tables")
            }
        }

        @JvmStatic
        fun hasCodec(): Boolean = SQLiteConnection.hasCodec()

        /**
         * Utility method to run the pre-compiled query and return the value in the
         * first column of the first row.
         */
        private fun longForQuery(prog: SQLiteStatement, selectionArgs: Array<String?>?): Long {
            prog.bindAllArgsAsStrings(selectionArgs)
            return prog.simpleQueryForLong()
        }

        /**
         * Utility method to run the pre-compiled query and return the value in the
         * first column of the first row.
         */
        fun stringForQuery(prog: SQLiteStatement, selectionArgs: Array<String?>?): String? {
            prog.bindAllArgsAsStrings(selectionArgs)
            return prog.simpleQueryForString()
        }

        /**
         * Utility method to run the pre-compiled query and return the blob value in the
         * first column of the first row.
         *
         * @return A read-only file descriptor for a copy of the blob value.
         */
        fun blobFileDescriptorForQuery(
            prog: SQLiteStatement,
            selectionArgs: Array<String?>?
        ): ParcelFileDescriptor {
            prog.bindAllArgsAsStrings(selectionArgs)
            return prog.simpleQueryForBlobFileDescriptor()
        }

        /**
         * Query the given table, returning a [Cursor] over the result set.
         *
         * @param table The table name to compile the query against.
         * @param columns A list of which columns to return. Passing null will
         * return all columns, which is discouraged to prevent reading
         * data from storage that isn't going to be used.
         * @param selection A filter declaring which rows to return, formatted as an
         * SQL WHERE clause (excluding the WHERE itself). Passing null
         * will return all rows for the given table.
         * @param selectionArgs You may include ?s in selection, which will be
         * replaced by the values from selectionArgs, in order that they
         * appear in the selection.
         * @param groupBy A filter declaring how to group rows, formatted as an SQL
         * GROUP BY clause (excluding the GROUP BY itself). Passing null
         * will cause the rows to not be grouped.
         * @param having A filter declare which row groups to include in the cursor,
         * if row grouping is being used, formatted as an SQL HAVING
         * clause (excluding the HAVING itself). Passing null will cause
         * all row groups to be included, and is required when row
         * grouping is not being used.
         * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
         * (excluding the ORDER BY itself). Passing null will use the
         * default sort order, which may be unordered.
         * @param limit Limits the number of rows returned by the query,
         * formatted as LIMIT clause. Passing null denotes no LIMIT clause.
         * @return A [Cursor] object, which is positioned before the first entry. Note that
         * [Cursor]s are not synchronized, see the documentation for more details.
         * @see Cursor
         */
        fun SQLiteDatabase.query(
            table: String,
            columns: Array<String?>?,
            selection: String?,
            selectionArgs: Array<Any?>?,
            groupBy: String?,
            having: String?,
            orderBy: String?,
            limit: String? = null,
        ): Cursor = query(
            distinct = false,
            table = table,
            columns = columns,
            selection = selection,
            selectionArgs = selectionArgs,
            groupBy = groupBy,
            having = having,
            orderBy = orderBy,
            limit = limit
        )

        /**
         * Query the given URL, returning a [Cursor] over the result set.
         *
         * @param distinct true if you want each row to be unique, false otherwise.
         * @param table The table name to compile the query against.
         * @param columns A list of which columns to return. Passing null will
         * return all columns, which is discouraged to prevent reading
         * data from storage that isn't going to be used.
         * @param selection A filter declaring which rows to return, formatted as an
         * SQL WHERE clause (excluding the WHERE itself). Passing null
         * will return all rows for the given table.
         * @param selectionArgs You may include ?s in selection, which will be
         * replaced by the values from selectionArgs, in order that they
         * appear in the selection.
         * @param groupBy A filter declaring how to group rows, formatted as an SQL
         * GROUP BY clause (excluding the GROUP BY itself). Passing null
         * will cause the rows to not be grouped.
         * @param having A filter declare which row groups to include in the cursor,
         * if row grouping is being used, formatted as an SQL HAVING
         * clause (excluding the HAVING itself). Passing null will cause
         * all row groups to be included, and is required when row
         * grouping is not being used.
         * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
         * (excluding the ORDER BY itself). Passing null will use the
         * default sort order, which may be unordered.
         * @param limit Limits the number of rows returned by the query,
         * formatted as LIMIT clause. Passing null denotes no LIMIT clause.
         * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
         * If the operation is canceled, then [OperationCanceledException] will be thrown
         * when the query is executed.
         * @return A [Cursor] object, which is positioned before the first entry. Note that
         * [Cursor]s are not synchronized, see the documentation for more details.
         * @see Cursor
         */
        fun SQLiteDatabase.query(
            distinct: Boolean,
            table: String,
            columns: Array<String?>?,
            selection: String?,
            selectionArgs: Array<Any?>?,
            groupBy: String?,
            having: String?,
            orderBy: String?,
            limit: String?,
            cancellationSignal: CancellationSignal? = null,
        ): Cursor = queryWithFactory(
            cursorFactory = null,
            distinct = distinct,
            table = table,
            columns = columns,
            selection = selection,
            selectionArgs = selectionArgs,
            groupBy = groupBy,
            having = having,
            orderBy = orderBy,
            limit = limit,
            cancellationSignal = cancellationSignal
        )

        /**
         * Runs the provided SQL and returns a [Cursor] over the result set.
         *
         * @param sql the SQL query. The SQL string must not be ; terminated
         * @param selectionArgs You may include ?s in where clause in the query,
         * which will be replaced by the values from selectionArgs.
         * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
         * If the operation is canceled, then [OperationCanceledException] will be thrown
         * when the query is executed.
         * @return A [Cursor] object, which is positioned before the first entry. Note that
         * [Cursor]s are not synchronized, see the documentation for more details.
         */
        fun SQLiteDatabase.rawQuery(
            sql: String?,
            selectionArgs: Array<Any?>?,
            cancellationSignal: CancellationSignal? = null
        ): Cursor = rawQueryWithFactory(null, sql, selectionArgs, null, cancellationSignal)

        /**
         * Convenience method for inserting a row into the database.
         *
         * @param table the table to insert the row into
         * @param nullColumnHack optional; may be `null`.
         * SQL doesn't allow inserting a completely empty row without
         * naming at least one column name.  If your provided `values` is
         * empty, no column names are known and an empty row can't be inserted.
         * If not set to null, the `nullColumnHack` parameter
         * provides the name of nullable column name to explicitly insert a NULL into
         * in the case where your `values` is empty.
         * @param values this map contains the initial column values for the
         * row. The keys should be the column names and the values the
         * column values
         * @return the row ID of the newly inserted row, or -1 if an error occurred
         */
        fun SQLiteDatabase.insert(table: String?, nullColumnHack: String?, values: ContentValues): Long = try {
            insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE)
        } catch (e: SQLException) {
            Log.e(TAG, "Error inserting $values", e)
            -1
        }

        /**
         * Convenience method for inserting a row into the database.
         *
         * @param table the table to insert the row into
         * @param nullColumnHack optional; may be `null`.
         * SQL doesn't allow inserting a completely empty row without
         * naming at least one column name.  If your provided `values` is
         * empty, no column names are known and an empty row can't be inserted.
         * If not set to null, the `nullColumnHack` parameter
         * provides the name of nullable column name to explicitly insert a NULL into
         * in the case where your `values` is empty.
         * @param values this map contains the initial column values for the
         * row. The keys should be the column names and the values the
         * column values
         * @throws SQLException
         * @return the row ID of the newly inserted row, or -1 if an error occurred
         */
        @Throws(SQLException::class)
        fun SQLiteDatabase.insertOrThrow(table: String?, nullColumnHack: String?, values: ContentValues?): Long {
            return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE)
        }

        /**
         * Convenience method for replacing a row in the database.
         *
         * @param table the table in which to replace the row
         * @param nullColumnHack optional; may be `null`.
         * SQL doesn't allow inserting a completely empty row without
         * naming at least one column name.  If your provided `initialValues` is
         * empty, no column names are known and an empty row can't be inserted.
         * If not set to null, the `nullColumnHack` parameter
         * provides the name of nullable column name to explicitly insert a NULL into
         * in the case where your `initialValues` is empty.
         * @param initialValues this map contains the initial column values for
         * the row.
         * @return the row ID of the newly inserted row, or -1 if an error occurred
         */
        fun SQLiteDatabase.replace(table: String?, nullColumnHack: String?, initialValues: ContentValues): Long {
            try {
                return insertWithOnConflict(table, nullColumnHack, initialValues, CONFLICT_REPLACE)
            } catch (e: SQLException) {
                Log.e(TAG, "Error inserting $initialValues", e)
                return -1
            }
        }

        /**
         * Convenience method for replacing a row in the database.
         *
         * @param table the table in which to replace the row
         * @param nullColumnHack optional; may be `null`.
         * SQL doesn't allow inserting a completely empty row without
         * naming at least one column name.  If your provided `initialValues` is
         * empty, no column names are known and an empty row can't be inserted.
         * If not set to null, the `nullColumnHack` parameter
         * provides the name of nullable column name to explicitly insert a NULL into
         * in the case where your `initialValues` is empty.
         * @param initialValues this map contains the initial column values for
         * the row. The key
         * @throws SQLException
         * @return the row ID of the newly inserted row, or -1 if an error occurred
         */
        @Throws(SQLException::class)
        fun SQLiteDatabase.replaceOrThrow(
            table: String?,
            nullColumnHack: String?,
            initialValues: ContentValues?,
        ): Long = insertWithOnConflict(table, nullColumnHack, initialValues, CONFLICT_REPLACE)

    }
}