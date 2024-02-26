package io.requery.android.database.sqlite.internal

import android.util.Log
import android.util.SparseIntArray
import io.requery.android.database.sqlite.base.AbstractWindowedCursor
import io.requery.android.database.sqlite.base.CursorWindow
import kotlin.math.max

/**
 * A Cursor implementation that exposes results from a query on a [SQLiteDatabase].
 *
 * SQLiteCursor is not internally synchronized so code using a SQLiteCursor from multiple
 * threads should perform its own synchronization when using the SQLiteCursor.
 */
internal class SQLiteCursor(
    /** The compiled query this cursor came from  */
    private val driver: SQLiteCursorDriver,
    /** The query object for the cursor  */
    private val query: SQLiteQuery
) : AbstractWindowedCursor() {
    /** The names of the columns in the rows  */
    private val columns: List<String> = query.columnNames

    /** The number of rows in the cursor  */
    private var count = NO_COUNT

    /** The number of rows that can fit in the cursor window, 0 if unknown  */
    private var cursorWindowCapacity = 0

    /** A mapping of column names to column indices, to speed up lookups  */
    private var columnNameArray: SparseIntArray? = null
    private var columnNameMap: HashMap<String, Int>? = null

    /** Used to find out where a cursor was allocated in case it never got released.  */
    private val closeGuard: CloseGuard = CloseGuard.get()

    /**
     * Get the database that this cursor is associated with.
     */
    val database: SQLiteDatabase
        get() = query.database

    override fun onMove(oldPosition: Int, newPosition: Int): Boolean {
        // Make sure the row at newPosition is present in the window
        _window.let {
            if ((it == null) || newPosition < it.startPosition || newPosition >= (it.startPosition + it.numRows)) {
                fillWindow(newPosition)
            }
        }

        return true
    }

    override fun getCount(): Int {
        if (count == NO_COUNT) {
            fillWindow(0)
        }
        return count
    }

    private fun fillWindow(requiredPos: Int) {
        clearOrCreateWindow(database.path)
        val window = checkNotNull(_window)

        try {
            if (count == NO_COUNT) {
                val startPos = cursorPickFillWindowStartPosition(requiredPos, 0)
                count = query.fillWindow(window, startPos, requiredPos, true)
                cursorWindowCapacity = window.numRows
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "received count(*) from native_fill_window: $count")
                }
            } else {
                val startPos = cursorPickFillWindowStartPosition(requiredPos, cursorWindowCapacity)
                query.fillWindow(window, startPos, requiredPos, false)
            }
        } catch (ex: RuntimeException) {
            // Close the cursor window if the query failed and therefore will
            // not produce any results.  This helps to avoid accidentally leaking
            // the cursor window if the client does not correctly handle exceptions
            // and fails to close the cursor.
            this.window = null
            throw ex
        }
    }

    override fun getColumnIndex(columnName: String): Int {
        // Create mColumnNameMap on demand
        if (columnNameArray == null && columnNameMap == null) {
            val columns = columns
            val columnCount = columns.size
            val map = SparseIntArray(columnCount)
            var collision = false
            for (i in 0 until columnCount) {
                val key = columns[i].hashCode()
                // check for hashCode collision
                if (map[key, -1] != -1) {
                    collision = true
                    break
                }
                map.put(key, i)
            }

            if (collision) {
                columnNameMap = HashMap()
                for (i in 0 until columnCount) {
                    columnNameMap!![columns[i]] = i
                }
            } else {
                columnNameArray = map
            }
        }

        // Hack according to bug 903852
        val periodIndex = columnName.lastIndexOf('.')
        val cleanColumnName = if (periodIndex != -1) {
            val e = Exception()
            Log.e(TAG, "requesting column name with table name -- $columnName", e)
            columnName.substring(periodIndex + 1)
        } else {
            columnName
        }

        if (columnNameMap != null) {
            val i = columnNameMap!![cleanColumnName]
            return i ?: -1
        } else {
            return columnNameArray!![cleanColumnName.hashCode(), -1]
        }
    }

    override fun getColumnNames(): Array<String> {
        return columns.toTypedArray<String>()
    }

    @Deprecated("Deprecated in Java")
    override fun deactivate() {
        super.deactivate()
        driver.cursorDeactivated()
    }

    override fun close() {
        super.close()
        synchronized(this) {
            query.close()
            driver.cursorClosed()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun requery(): Boolean {
        if (isClosed) {
            return false
        }

        synchronized(this) {
            if (!query.database.isOpen) {
                return false
            }
            _window?.clear()
            pos = -1
            count = NO_COUNT
            driver.cursorRequeried(this)
        }

        try {
            return super.requery()
        } catch (e: IllegalStateException) {
            // for backwards compatibility, just return false
            Log.w(TAG, "requery() failed " + e.message, e)
            return false
        }
    }

    override var window: CursorWindow?
        get() = super.window
        set(value) {
            super.window = value
            count = NO_COUNT
        }

    /**
     * Changes the selection arguments. The new values take effect after a call to requery().
     */
    fun setSelectionArguments(selectionArgs: Array<String?>) {
        driver.setBindArguments(selectionArgs.asList())
    }

    /**
     * Release the native resources, if they haven't been released yet.
     */
    override fun finalize() {
        try {
            // if the cursor hasn't been closed yet, close it first
            if (_window != null) {
                closeGuard.warnIfOpen()
                close()
            }
        } finally {
            super.finalize()
        }
    }

    companion object {
        const val TAG: String = "SQLiteCursor"
        const val NO_COUNT: Int = -1

        fun cursorPickFillWindowStartPosition(
            cursorPosition: Int, cursorWindowCapacity: Int
        ): Int {
            return max((cursorPosition - cursorWindowCapacity / 3).toDouble(), 0.0).toInt()
        }
    }
}