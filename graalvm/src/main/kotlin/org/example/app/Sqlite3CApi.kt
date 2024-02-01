package org.example.app

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