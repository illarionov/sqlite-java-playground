package io.requery.android.database.sqlite.internal

import android.util.Log

/**
 * Provides debugging info about all SQLite databases running in the current process.
 *
 * {@hide}
 */
// TODO: Set?
class SQLiteDebug(
    /**
     * Controls the printing of informational SQL log messages.
     */
    val sqlLog: Boolean = false,

    /**
     * Controls the printing of SQL statements as they are executed.
     */
    val sqlStatements: Boolean = false,

    /**
     * Controls the printing of wall-clock time taken to execute SQL statements
     * as they are executed.
     */
    val sqlTime: Boolean = false,

    /**
     * True to enable database performance testing instrumentation.
     */
    val logSlowQueries: Boolean = false,

    /**
     * Reads the "db.log.slow_query_threshold" system property, which can be changed
     * by the user at any time.  If the value is zero, then all queries will
     * be considered slow.  If the value does not exist or is negative, then no queries will
     * be considered slow.
     *
     * This value can be changed dynamically while the system is running.
     * For example, "adb shell setprop db.log.slow_query_threshold 200" will
     * log all queries that take 200ms or longer to run.
     */
    val slowQueryThresholdProvider: () -> Int = {
        System.getProperty("db.log.slow_query_threshold", "-1")!!.toInt()
    }
) {
    /**
     * Determines whether a query should be logged.
     */
    fun shouldLogSlowQuery(elapsedTimeMillis: Long): Boolean {
        val slowQueryMillis = slowQueryThresholdProvider()
        return slowQueryMillis in 0..elapsedTimeMillis
    }
}

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