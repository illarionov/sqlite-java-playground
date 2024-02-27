package io.requery.android.database.sqlite.base

import io.requery.android.database.sqlite.internal.SQLiteDatabase

/**
 * An interface to let apps define an action to take when database corruption is detected.
 */
internal interface DatabaseErrorHandler {
    /**
     * The method invoked when database corruption is detected.
     * @param dbObj the [SQLiteDatabase] object representing the database on which corruption
     * is detected.
     */
    fun onCorruption(dbObj: SQLiteDatabase<*, *, *>)
}