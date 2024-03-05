package io.requery.android.database.sqlite.internal

import co.touchlab.kermit.Logger
import io.requery.android.database.sqlite.base.AbstractWindowedCursor
import io.requery.android.database.sqlite.base.CursorWindow
import io.requery.android.database.sqlite.internal.interop.Sqlite3ConnectionPtr
import io.requery.android.database.sqlite.internal.interop.Sqlite3StatementPtr
import io.requery.android.database.sqlite.internal.interop.Sqlite3WindowPtr
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.max

/**
 * A Cursor implementation that exposes results from a query on a [SQLiteDatabase].
 *
 * SQLiteCursor is not internally synchronized so code using a SQLiteCursor from multiple
 * threads should perform its own synchronization when using the SQLiteCursor.
 */
internal class SQLiteCursor<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr>(
    /** The compiled query this cursor came from  */
    private val driver: SQLiteCursorDriver<CP, SP, WP>,
    /** The query object for the cursor  */
    private val query: SQLiteQuery<WP>,
    private val windowCtor: (name: String?) -> CursorWindow<WP>,
    logger: Logger = Logger
) : AbstractWindowedCursor<WP>(windowCtor) {
    private val logger = logger.withTag(TAG)

    /** The names of the columns in the rows  */
    private val columns: List<String> = query.columnNames

    /** The number of rows in the cursor  */
    private var count = NO_COUNT

    /** The number of rows that can fit in the cursor window, 0 if unknown  */
    private var cursorWindowCapacity = 0

    /** A mapping of column names to column indices, to speed up lookups  */
    private val columnNameMap: Map<String, Int> by lazy(NONE) {
        columns.mapIndexed { columnNo, name -> name to columnNo }.toMap()
    }

    /** Used to find out where a cursor was allocated in case it never got released.  */
    private val closeGuard: CloseGuard = CloseGuard.get()

    /**
     * Get the database that this cursor is associated with.
     */
    val database: SQLiteDatabase<*, *, WP>
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

                logger.d { "received count(*) from native_fill_window: $count" }
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
        // Hack according to bug 903852
        val cleanColumnName = columnName.substringAfterLast(".")
        return columnNameMap.getOrDefault(cleanColumnName, -1)
    }

    override fun getColumnNames(): Array<String> = columns.toTypedArray<String>()

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
            logger.w(e) { "requery() failed ${e.message}" }
            return false
        }
    }

    override var window: CursorWindow<WP>?
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