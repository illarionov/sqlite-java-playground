package io.requery.android.database.sqlite.internal

/**
 * Describes a SQLite statement.
 */
internal data class SQLiteStatementInfo(
    /**
     * The number of parameters that the statement has.
     */
    val numParameters: Int,

    /**
     * The names of all columns in the result set of the statement.
     */
    val columnNames: List<String>,

    /**
     * True if the statement is read-only.
     */
    val readOnly: Boolean,
)