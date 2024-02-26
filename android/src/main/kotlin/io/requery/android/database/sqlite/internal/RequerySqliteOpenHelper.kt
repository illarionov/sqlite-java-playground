package io.requery.android.database.sqlite.internal

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.sqlite.db.SupportSQLiteOpenHelper
import io.requery.android.database.DatabaseErrorHandler
import io.requery.android.database.sqlite.SQLiteDatabaseConfiguration

/**
 * A helper class to manage database creation and version management.
 *
 *
 * You create a subclass implementing [.onCreate], [.onUpgrade] and
 * optionally [.onOpen], and this class takes care of opening the database
 * if it exists, creating it if it does not, and upgrading it as necessary.
 * Transactions are used to make sure the database is always in a sensible state.
 *
 *
 * This class makes it easy for [android.content.ContentProvider]
 * implementations to defer opening and upgrading the database until first use,
 * to avoid blocking application startup with long-running database upgrades.
 *
 *
 * For an example, see the NotePadProvider class in the NotePad sample application,
 * in the *samples/ * directory of the SDK.
 *
 *
 * **Note:** this class assumes
 * monotonically increasing version numbers for upgrades.
 *
 *
 */
@Suppress("unused")
internal abstract class RequerySqliteOpenHelper(
    private val context: Context,
    override val databaseName: String?,
    private val factory: SQLiteDatabase.CursorFactory?,
    private val version: Int,
    private val errorHandler: DatabaseErrorHandler? = null
) : SupportSQLiteOpenHelper {
    private var database: SQLiteDatabase? = null
    private var isInitializing = false
    private var enableWriteAheadLogging = false

    /**
     * Create a helper object to create, open, and/or manage a database.
     * The database is not actually created or opened until one of
     * [.getWritableDatabase] or [.getReadableDatabase] is called.
     *
     *
     * Accepts input param: a concrete instance of [DatabaseErrorHandler] to be
     * used to handle corruption when sqlite reports database corruption.
     *
     * @param context to use to open or create the database
     * @param databaseName of the database file, or null for an in-memory database
     * @param factory to use for creating cursor objects, or null for the default
     * @param version number of the database (starting at 1); if the database is older,
     * [.onUpgrade] will be used to upgrade the database; if the database is
     * newer, [.onDowngrade] will be used to downgrade the database
     * @param errorHandler the [DatabaseErrorHandler] to be used when sqlite reports database
     * corruption, or null to use the default error handler.
     */
    /**
     * Create a helper object to create, open, and/or manage a database.
     * This method always returns very quickly.  The database is not actually
     * created or opened until one of [.getWritableDatabase] or
     * [.getReadableDatabase] is called.
     *
     * @param context to use to open or create the database
     * @param name of the database file, or null for an in-memory database
     * @param factory to use for creating cursor objects, or null for the default
     * @param version number of the database (starting at 1); if the database is older,
     * [.onUpgrade] will be used to upgrade the database; if the database is
     * newer, [.onDowngrade] will be used to downgrade the database
     */
    init {
        require(version >= 1) { "Version must be >= 1, was $version" }
    }

    /**
     * Enables or disables the use of write-ahead logging for the database.
     *
     * Write-ahead logging cannot be used with read-only databases so the value of
     * this flag is ignored if the database is opened read-only.
     *
     * @param enabled True if write-ahead logging should be enabled, false if it
     * should be disabled.
     *
     * @see SQLiteDatabase.enableWriteAheadLogging
     */
    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        synchronized(this) {
            if (enableWriteAheadLogging != enabled) {
                if (database != null && database!!.isOpen && !database!!.isReadOnly) {
                    if (enabled) {
                        database!!.enableWriteAheadLogging()
                    } else {
                        database!!.disableWriteAheadLogging()
                    }
                }
                enableWriteAheadLogging = enabled
            }
        }
    }

    override val writableDatabase: SQLiteDatabase
        /**
         * Create and/or open a database that will be used for reading and writing.
         * The first time this is called, the database will be opened and
         * [.onCreate], [.onUpgrade] and/or [.onOpen] will be
         * called.
         *
         *
         * Once opened successfully, the database is cached, so you can
         * call this method every time you need to write to the database.
         * (Make sure to call [.close] when you no longer need the database.)
         * Errors such as bad permissions or a full disk may cause this method
         * to fail, but future attempts may succeed if the problem is fixed.
         *
         *
         * Database upgrade may take a long time, you
         * should not call this method from the application main thread, including
         * from [ContentProvider.onCreate()][android.content.ContentProvider.onCreate].
         *
         * @throws SQLiteException if the database cannot be opened for writing
         * @return a read/write database object valid until [.close] is called
         */
        get() = synchronized(this) {
            getDatabaseLocked(true)
        }

    override val readableDatabase: SQLiteDatabase
        /**
         * Create and/or open a database.  This will be the same object returned by
         * [.getWritableDatabase] unless some problem, such as a full disk,
         * requires the database to be opened read-only.  In that case, a read-only
         * database object will be returned.  If the problem is fixed, a future call
         * to [.getWritableDatabase] may succeed, in which case the read-only
         * database object will be closed and the read/write object will be returned
         * in the future.
         *
         *
         * Like [.getWritableDatabase], this method may
         * take a long time to return, so you should not call it from the
         * application main thread, including from
         * [ContentProvider.onCreate()][android.content.ContentProvider.onCreate].
         *
         * @throws SQLiteException if the database cannot be opened
         * @return a database object valid until [.getWritableDatabase]
         * or [.close] is called.
         */
        get() = synchronized(this) {
            getDatabaseLocked(false)
        }

    private fun getDatabaseLocked(writable: Boolean): SQLiteDatabase {
        database?.let { db ->
            if (!db.isOpen) {
                // Darn!  The user closed the database by calling mDatabase.close().
                database = null
            } else if (!writable || !db.isReadOnly) {
                // The database is already open for business.
                return db
            }
        }


        check(!isInitializing) { "getDatabase called recursively" }

        var db = database
        try {
            isInitializing = true

            if (db != null) {
                if (db.isReadOnly) {
                    db.reopenReadWrite()
                }
            } else if (databaseName == null) {
                db = SQLiteDatabase.create(null)
            } else {
                try {
                    val path = context.getDatabasePath(databaseName).path
                    if (DEBUG_STRICT_READONLY && !writable) {
                        val configuration = createConfiguration(path, SQLiteDatabase.OPEN_READONLY)
                        db = SQLiteDatabase.openDatabase(configuration, factory, errorHandler)
                    } else {
                        var flags = if (enableWriteAheadLogging) SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING else 0
                        flags = flags or SQLiteDatabase.CREATE_IF_NECESSARY
                        val configuration = createConfiguration(path, flags)
                        db = SQLiteDatabase.openDatabase(configuration, factory, errorHandler)
                    }
                } catch (ex: SQLiteException) {
                    if (writable) {
                        throw ex
                    }
                    Log.e(TAG, "Couldn't open $databaseName for writing (will try read-only):", ex)
                    val path = context.getDatabasePath(databaseName).path
                    val configuration = createConfiguration(path, SQLiteDatabase.OPEN_READONLY)
                    db = SQLiteDatabase.openDatabase(configuration, factory, errorHandler)
                }
            }

            onConfigure(db!!)

            val version = db.version
            if (version != this.version) {
                if (db.isReadOnly) {
                    throw SQLiteException(
                        "Can't upgrade read-only database from version ${db.version} to ${this.version}: $databaseName"
                    )
                }

                db.beginTransaction()
                try {
                    if (version == 0) {
                        onCreate(db)
                    } else {
                        if (version > this.version) {
                            onDowngrade(db, version, this.version)
                        } else {
                            onUpgrade(db, version, this.version)
                        }
                    }
                    db.version = this.version
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }

            onOpen(db)

            if (db.isReadOnly) {
                Log.w(TAG, "Opened $databaseName in read-only mode")
            }

            database = db
            return db
        } finally {
            isInitializing = false
            if (db != null && db != database) {
                db.close()
            }
        }
    }

    /**
     * Close any open database object.
     */
    @Synchronized
    override fun close() {
        check(!isInitializing) { "Closed during initialization" }
        database?.let { db ->
            if (db.isOpen) {
                db.close()
                database = null
            }
        }
    }

    /**
     * Called when the database connection is being configured, to enable features
     * such as write-ahead logging or foreign key support.
     *
     *
     * This method is called before [.onCreate], [.onUpgrade],
     * [.onDowngrade], or [.onOpen] are called.  It should not modify
     * the database except to configure the database connection as required.
     *
     *
     * This method should only call methods that configure the parameters of the
     * database connection, such as [SQLiteDatabase.enableWriteAheadLogging]
     * [SQLiteDatabase.setForeignKeyConstraintsEnabled],
     * [SQLiteDatabase.setLocale], [SQLiteDatabase.setMaximumSize],
     * or executing PRAGMA statements.
     *
     *
     * @param db The database.
     */
    open fun onConfigure(db: SQLiteDatabase) {}

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    abstract fun onCreate(db: SQLiteDatabase)

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     *
     *
     *
     * The SQLite ALTER TABLE documentation can be found
     * [here](http://sqlite.org/lang_altertable.html). If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     *
     *
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     *
     *
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    abstract fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)

    /**
     * Called when the database needs to be downgraded. This is strictly similar to
     * [.onUpgrade] method, but is called whenever current version is newer than requested one.
     * However, this method is not abstract, so it is not mandatory for a customer to
     * implement it. If not overridden, default implementation will reject downgrade and
     * throws SQLiteException
     *
     *
     *
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     *
     *
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    open fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw SQLiteException("Can't downgrade database from version $oldVersion to $newVersion")
    }

    /**
     * Called when the database has been opened.  The implementation
     * should check [SQLiteDatabase.isReadOnly] before updating the
     * database.
     *
     *
     * This method is called after the database connection has been configured
     * and after the database schema has been created, upgraded or downgraded as necessary.
     * If the database connection must be configured in some way before the schema
     * is created, upgraded, or downgraded, do it in [.onConfigure] instead.
     *
     *
     * @param db The database.
     */
    open fun onOpen(db: SQLiteDatabase) {}

    /**
     * Called before the database is opened. Provides the [SQLiteDatabaseConfiguration]
     * instance that is used to initialize the database. Override this to create a configuration
     * that has custom functions or extensions.
     *
     * @param path to database file to open and/or create
     * @param openFlags to control database access mode
     * @return [SQLiteDatabaseConfiguration] instance, cannot be null.
     */
    protected open fun createConfiguration(
        path: String,
        @SQLiteDatabase.OpenFlags openFlags: Int
    ): SQLiteDatabaseConfiguration = SQLiteDatabaseConfiguration(path, openFlags)

    companion object {
        private val TAG: String = RequerySqliteOpenHelper::class.java.simpleName

        // When true, getReadableDatabase returns a read-only database if it is just being opened.
        // The database handle is reopened in read/write mode when getWritableDatabase is called.
        // We leave this behavior disabled in production because it is inefficient and breaks
        // many applications.  For debugging purposes it can be useful to turn on strict
        // read-only semantics to catch applications that call getReadableDatabase when they really
        // wanted getWritableDatabase.
        private const val DEBUG_STRICT_READONLY = false
    }
}