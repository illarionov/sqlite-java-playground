package io.requery.android.database.sqlite.internal

import android.database.Cursor
import androidx.core.os.CancellationSignal
import co.touchlab.kermit.Logger
import io.requery.android.database.sqlite.base.CursorWindow
import io.requery.android.database.sqlite.internal.SQLiteProgram.Companion.bindAllArgsAsStrings
import io.requery.android.database.sqlite.internal.interop.Sqlite3ConnectionPtr
import io.requery.android.database.sqlite.internal.interop.Sqlite3StatementPtr
import io.requery.android.database.sqlite.internal.interop.Sqlite3WindowPtr

/**
 * A cursor driver that uses the given query directly.
 */
internal class SQLiteDirectCursorDriver<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr>(
    private val database: SQLiteDatabase<CP, SP, WP>,
    private val sql: String,
    private val cancellationSignal: CancellationSignal?,
    private val cursorWindowCtor: (name: String?) -> CursorWindow<WP>,
    logger: Logger,
) : SQLiteCursorDriver<CP, SP, WP> {
    private val logger: Logger = Logger.withTag("SQLiteDirectCursorDriver")
    private var query: SQLiteQuery<WP>? = null

    override fun query(
        factory: SQLiteDatabase.CursorFactory<CP, SP, WP>?,
        bindArgs: List<Any?>,
    ): Cursor {
        val query = SQLiteQuery(database, sql, bindArgs, cancellationSignal)
        val cursor: Cursor
        try {
            cursor = factory?.newCursor(database, this, query) ?: SQLiteCursor(
                this,
                query,
                cursorWindowCtor,
                logger,
            )
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