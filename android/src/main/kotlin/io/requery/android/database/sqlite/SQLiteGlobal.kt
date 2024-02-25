package io.requery.android.database.sqlite

/**
 * Provides access to SQLite functions that affect all database connection,
 * such as memory management.
 *
 *
 * The native code associated with SQLiteGlobal is also sets global configuration options
 * using sqlite3_config() then calls sqlite3_initialize() to ensure that the SQLite
 * library is properly initialized exactly once before any other framework or application
 * code has a chance to run.
 *
 *
 * Verbose SQLite logging is enabled if the "log.tag.SQLiteLog" property is set to "V".
 * (per [SQLiteDebug.DEBUG_SQL_LOG]).
 *
 * @hide
 */
internal object SQLiteGlobal {
    // private external fun nativeReleaseMemory(): Int

    /**
     * Attempts to release memory by pruning the SQLite page cache and other
     * internal data structures.
     *
     * @return The number of bytes that were freed.
     */
    fun releaseMemory(): Int {
        TODO("nativeReleaseMemory()")
    }

    // values derived from:
    // https://android.googlesource.com/platform/frameworks/base.git/+/master/core/res/res/values/config.xml
    @JvmStatic
    val defaultPageSize: Int = 1024

    /**
     * Gets the default journal mode when WAL is not in use.
     */
    @JvmStatic
    val defaultJournalMode: String = "TRUNCATE"

    /**
     * Gets the journal size limit in bytes.
     */
    @JvmStatic
    val journalSizeLimit: Int = 524288

    /**
     * Gets the default database synchronization mode when WAL is not in use.
     */
    @JvmStatic
    val defaultSyncMode: String = "FULL"

    /**
     * Gets the database synchronization mode when in WAL mode.
     */
    @JvmStatic
    val wALSyncMode: String = "normal"

    /**
     * Gets the WAL auto-checkpoint integer in database pages.
     */
    @JvmStatic
    val wALAutoCheckpoint: Int = 1000

    /**
     * Gets the connection pool size when in WAL mode.
     */
    @JvmStatic
    val wALConnectionPoolSize: Int = 10
}