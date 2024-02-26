package io.requery.android.database.sqlite.internal

import android.database.Cursor
import androidx.core.os.CancellationSignal
import io.requery.android.database.sqlite.internal.SQLiteProgram.Companion.bindAllArgsAsStrings

/**
 * A cursor driver that uses the given query directly.
 */
internal class SQLiteDirectCursorDriver(
    private val database: SQLiteDatabase,
    private val sql: String,
    private val cancellationSignal: CancellationSignal?
) : SQLiteCursorDriver {
    private var query: SQLiteQuery? = null

    override fun query(
        factory: SQLiteDatabase.CursorFactory?,
        bindArgs: List<Any?>,
    ): Cursor {
        val query = SQLiteQuery(database, sql, bindArgs, cancellationSignal)
        val cursor: Cursor
        try {
            cursor = factory?.newCursor(database, this, query) ?: SQLiteCursor(this, query)
        } catch (ex: RuntimeException) {
            query.close()
            throw ex
        }

        this.query = query
        return cursor
    }

    override fun cursorClosed() {
        // Do nothing
    }

    override fun setBindArguments(bindArgs: List<String?>) {
        requireNotNull(query) { "query() not called" }.bindAllArgsAsStrings(bindArgs)
    }

    override fun cursorDeactivated() {
        // Do nothing
    }

    override fun cursorRequeried(cursor: Cursor) {
        // Do nothing
    }

    override fun toString(): String = "SQLiteDirectCursorDriver: $sql"
}