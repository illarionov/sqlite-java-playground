package ru.pixnews.wasm.sqlite3.chicory.sqlite3

import ru.pixnews.wasm.sqlite3.chicory.bindings.SqliteBindings

class Sqlite3CApi(
    private val sqlite: SqliteBindings
) {
    val version: Sqlite3Version
    get() = Sqlite3Version(
        sqlite.version,
        sqlite.versionNumber,
        sqlite.sourceId,
    )

    data class Sqlite3Version(
        val version: String,
        val versionNumber: Int,
        val sourceId: String,
    )
}