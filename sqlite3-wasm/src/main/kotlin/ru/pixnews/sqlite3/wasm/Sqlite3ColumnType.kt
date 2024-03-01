package ru.pixnews.sqlite3.wasm

@JvmInline
value class Sqlite3ColumnType(
    val id: Int
) {
    companion object {
        val SQLITE3_INTEGER = Sqlite3ColumnType(1)
        val SQLITE3_FLOAT = Sqlite3ColumnType(2)
        val SQLITE3_BLOB = Sqlite3ColumnType(4)
        val SQLITE3_NULL = Sqlite3ColumnType(5)
        val SQLITE3_TEXT = Sqlite3ColumnType(3)
    }
}