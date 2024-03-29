package io.requery.android.database.sqlite.internal

import android.database.sqlite.SQLiteDatabaseCorruptException
import androidx.sqlite.db.SupportSQLiteStatement
import io.requery.android.database.sqlite.internal.interop.Sqlite3WindowPtr

/**
 * Represents a statement that can be executed against a database.  The statement
 * cannot return multiple rows or columns, but single value (1 x 1) result sets
 * are supported.
 *
 *
 * This class is not thread-safe.
 *
 */
internal class SQLiteStatement<WP: Sqlite3WindowPtr>(
    db: SQLiteDatabase<*, *, WP>,
    sql: String,
    bindArgs: List<Any?> = emptyList<Unit>(),
) : SQLiteProgram<WP>(db, sql, bindArgs, null), SupportSQLiteStatement {
    /**
     * Execute this SQL statement, if it is not a SELECT / INSERT / DELETE / UPDATE, for example
     * CREATE / DROP table, view, trigger, index etc.
     *
     * @throws SQLException If the SQL string is invalid for some reason
     */
    override fun execute() = useReference{
        try {
            session.execute(sql, bindArgs, connectionFlags, null)
        } catch (ex: SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        }
    }

    /**
     * Execute this SQL statement, if the the number of rows affected by execution of this SQL
     * statement is of any importance to the caller - for example, UPDATE / DELETE SQL statements.
     *
     * @return the number of rows affected by this SQL statement execution.
     * @throws SQLException If the SQL string is invalid for some reason
     */
    override fun executeUpdateDelete(): Int = useReference {
        try {
            return session.executeForChangedRowCount(sql, bindArgs, connectionFlags, null)
        } catch (ex: SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        }
    }

    /**
     * Execute this SQL statement and return the ID of the row inserted due to this call.
     * The SQL statement should be an INSERT for this to be a useful call.
     *
     * @return the row ID of the last row inserted, if this insert is successful. -1 otherwise.
     *
     * @throws SQLException If the SQL string is invalid for some reason
     */
    override fun executeInsert(): Long = useReference {
        try {
            return session.executeForLastInsertedRowId(sql, bindArgs, connectionFlags, null)
        } catch (ex: SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        }
    }

    /**
     * Execute a statement that returns a 1 by 1 table with a numeric value.
     * For example, SELECT COUNT(*) FROM table;
     *
     * @return The result of the query.
     *
     * @throws SQLiteDoneException if the query returns zero rows
     */
    override fun simpleQueryForLong(): Long = useReference {
        try {
            return session.executeForLong(
                sql, bindArgs, connectionFlags, null
            )
        } catch (ex: SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        }
    }

    /**
     * Execute a statement that returns a 1 by 1 table with a text value.
     * For example, SELECT COUNT(*) FROM table;
     *
     * @return The result of the query.
     *
     * @throws SQLiteDoneException if the query returns zero rows
     */
    override fun simpleQueryForString(): String? = useReference {
        try {
            return session.executeForString(
                sql, bindArgs, connectionFlags, null
            )
        } catch (ex: SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        }
    }

    override fun toString(): String = "SQLiteProgram: $sql"
}