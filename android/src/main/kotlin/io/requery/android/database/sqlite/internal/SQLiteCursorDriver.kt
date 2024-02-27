package io.requery.android.database.sqlite.internal

import android.database.Cursor
import io.requery.android.database.sqlite.internal.interop.Sqlite3ConnectionPtr
import io.requery.android.database.sqlite.internal.interop.Sqlite3StatementPtr
import io.requery.android.database.sqlite.internal.interop.Sqlite3WindowPtr

/**
 * A driver for SQLiteCursors that is used to create them and gets notified
 * by the cursors it creates on significant events in their lifetimes.
 */
internal interface SQLiteCursorDriver<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr> {
    /**
     * Executes the query returning a Cursor over the result set.
     *
     * @param factory The CursorFactory to use when creating the Cursors, or
     * null if standard SQLiteCursors should be returned.
     * @return a Cursor over the result set
     */
    fun query(factory: SQLiteDatabase.CursorFactory<CP, SP, WP>?, bindArgs: List<Any?>): Cursor

    /**
     * Called by a SQLiteCursor when it is released.
     */
    fun cursorDeactivated()

    /**
     * Called by a SQLiteCursor when it is requeried.
     */
    fun cursorRequeried(cursor: Cursor)

    /**
     * Called by a SQLiteCursor when it it closed to destroy this object as well.
     */
    fun cursorClosed()

    /**
     * Set new bind arguments. These will take effect in cursorRequeried().
     * @param bindArgs the new arguments
     */
    fun setBindArguments(bindArgs: List<String?>)
}