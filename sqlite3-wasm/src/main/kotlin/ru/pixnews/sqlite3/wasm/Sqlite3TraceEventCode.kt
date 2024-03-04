package ru.pixnews.sqlite3.wasm

import ru.pixnews.sqlite3.wasm.util.Sqlite3BitMaskExt

@JvmInline
value class Sqlite3TraceEventCode(
    override val mask: Int
) : Sqlite3BitMaskExt<Sqlite3TraceEventCode> {
    override val newInstance: (Int) -> Sqlite3TraceEventCode get() = ::Sqlite3TraceEventCode

    override fun toString(): String = "0x" + mask.toString(16)

    companion object {

        /**
         * An SQLITE_TRACE_STMT callback is invoked when a prepared statement first begins running and possibly
         * at other times during the execution of the prepared statement, such as at the start of each trigger
         * subprogram
         */
        val SQLITE_TRACE_STMT = Sqlite3TraceEventCode(0x01)

        /**
         * An SQLITE_TRACE_PROFILE callback provides approximately the same information as is provided by the
         * sqlite3_profile() callback.
         * The P argument is a pointer to the prepared statement and the X argument points to
         * a 64-bit integer which is approximately the number of nanoseconds that the prepared statement
         * took to run. The SQLITE_TRACE_PROFILE callback is invoked when the statement finishes.
         *
         */
        val SQLITE_TRACE_PROFILE = Sqlite3TraceEventCode(0x02)

        /**
         * An SQLITE_TRACE_ROW callback is invoked whenever a prepared statement generates a single row of result.
         * The P argument is a pointer to the prepared statement and the X argument is unused.
         */
        val SQLITE_TRACE_ROW = Sqlite3TraceEventCode(0x04)

        /**
         * An SQLITE_TRACE_CLOSE callback is invoked when a database connection closes.
         * The P argument is a pointer to the database connection object and the X argument is unused.
         */
        val SQLITE_TRACE_CLOSE = Sqlite3TraceEventCode(0x08)
    }
}