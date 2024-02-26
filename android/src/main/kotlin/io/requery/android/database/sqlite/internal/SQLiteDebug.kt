package io.requery.android.database.sqlite.internal

import android.util.Log

/**
 * Provides debugging info about all SQLite databases running in the current process.
 *
 * {@hide}
 */
object SQLiteDebug {
    private external fun nativeGetPagerStats(stats: PagerStats)

    /**
     * Controls the printing of informational SQL log messages.
     *
     * Enable using "adb shell setprop log.tag.SQLiteLog VERBOSE".
     */
    val DEBUG_SQL_LOG: Boolean = Log.isLoggable("SQLiteLog", Log.VERBOSE)

    /**
     * Controls the printing of SQL statements as they are executed.
     *
     * Enable using "adb shell setprop log.tag.SQLiteStatements VERBOSE".
     */
    val DEBUG_SQL_STATEMENTS: Boolean = Log.isLoggable("SQLiteStatements", Log.VERBOSE)

    /**
     * Controls the printing of wall-clock time taken to execute SQL statements
     * as they are executed.
     *
     * Enable using "adb shell setprop log.tag.SQLiteTime VERBOSE".
     */
    val DEBUG_SQL_TIME: Boolean = Log.isLoggable("SQLiteTime", Log.VERBOSE)

    /**
     * True to enable database performance testing instrumentation.
     * @hide
     */
    const val DEBUG_LOG_SLOW_QUERIES: Boolean = false

    /**
     * Determines whether a query should be logged.
     *
     * Reads the "db.log.slow_query_threshold" system property, which can be changed
     * by the user at any time.  If the value is zero, then all queries will
     * be considered slow.  If the value does not exist or is negative, then no queries will
     * be considered slow.
     *
     * This value can be changed dynamically while the system is running.
     * For example, "adb shell setprop db.log.slow_query_threshold 200" will
     * log all queries that take 200ms or longer to run.
     * @hide
     */
    fun shouldLogSlowQuery(elapsedTimeMillis: Long): Boolean {
        val slowQueryMillis = System.getProperty("db.log.slow_query_threshold", "-1")!!.toInt()
        return slowQueryMillis in 0..elapsedTimeMillis
    }

    /**
     * Contains statistics about the active pagers in the current process.
     *
     * @see .nativeGetPagerStats
     */
    internal data class PagerStats(
        /** the current amount of memory checked out by sqlite using sqlite3_malloc().
         * documented at http://www.sqlite.org/c3ref/c_status_malloc_size.html
         */
        val memoryUsed: Int = 0,

        /** the number of bytes of page cache allocation which could not be sattisfied by the
         * SQLITE_CONFIG_PAGECACHE buffer and where forced to overflow to sqlite3_malloc().
         * The returned value includes allocations that overflowed because they where too large
         * (they were larger than the "sz" parameter to SQLITE_CONFIG_PAGECACHE) and allocations
         * that overflowed because no space was left in the page cache.
         * documented at http://www.sqlite.org/c3ref/c_status_malloc_size.html
         */
        val pageCacheOverflow: Int = 0,

        /** records the largest memory allocation request handed to sqlite3.
         * documented at http://www.sqlite.org/c3ref/c_status_malloc_size.html
         */
        val largestMemAlloc: Int = 0,

        /** a list of [DbStats] - one for each main database opened by the applications
         * running on the android device
         */
        val dbStats: List<DbStats> = emptyList(),
    )

    /**
     * contains statistics about a database
     */
    internal class DbStats(
        /** name of the database  */
        val dbName: String,
        pageCount: Long,
        pageSize: Long,
        /** documented here http://www.sqlite.org/c3ref/c_dbstatus_lookaside_used.html  */
        val lookaside: Int,
        hits: Int,
        misses: Int,
        cachesize: Int
    ) {
        /** the page size for the database  */
        val pageSize: Long = pageSize / 1024

        /** the database size  */
        val dbSize: Long = pageCount * pageSize / 1024

        /** statement cache stats: hits/misses/cachesize  */
        val cache: String = "$hits/$misses/$cachesize"
    }
}