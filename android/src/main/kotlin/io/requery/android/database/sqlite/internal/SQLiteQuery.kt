package io.requery.android.database.sqlite.internal

import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.core.os.CancellationSignal
import io.requery.android.database.sqlite.base.CursorWindow

/**
 * Represents a query that reads the resulting rows into a [SQLiteQuery].
 * This class is used by [SQLiteCursor] and isn't useful itself.
 *
 *
 * This class is not thread-safe.
 *
 */
internal class SQLiteQuery(
    db: SQLiteDatabase,
    query: String,
    bindArgs: List<Any?>,
    private val mCancellationSignal: CancellationSignal?
) : SQLiteProgram(db, query, bindArgs, mCancellationSignal) {
    /**
     * Reads rows into a buffer.
     *
     * @param window The window to fill into
     * @param startPos The start position for filling the window.
     * @param requiredPos The position of a row that MUST be in the window.
     * If it won't fit, then the query should discard part of what it filled.
     * @param countAllRows True to count all rows that the query would
     * return regardless of whether they fit in the window.
     * @return Number of rows that were enumerated.  Might not be all rows
     * unless countAllRows is true.
     *
     * @throws SQLiteException if an error occurs.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun fillWindow(
        window: CursorWindow,
        startPos: Int,
        requiredPos: Int,
        countAllRows: Boolean
    ): Int = useReference {
        window.useReference {
            try {
                session.executeForCursorWindow(
                    sql, bindArgs,
                    window, startPos, requiredPos, countAllRows, connectionFlags,
                    mCancellationSignal
                )
            } catch (ex: SQLiteDatabaseCorruptException) {
                onCorruption()
                throw ex
            } catch (ex: SQLiteException) {
                Log.e(TAG, "exception: " + ex.message + "; query: " + sql)
                throw ex
            }
        }
    }

    override fun toString(): String = "SQLiteQuery: $sql"

    private companion object {
        private const val TAG = "SQLiteQuery"
    }
}