package org.example.app

import org.example.app.bindings.SqliteBindings

class Sqlite3CApi(
    private val bindings: SqliteBindings
) {
    val version: Sqlite3Version
        get() = Sqlite3Version(
            bindings.version,
            bindings.versionNumber,
            bindings.sourceId,
        )

    data class Sqlite3Version(
        val version: String,
        val versionNumber: Int,
        val sourceId: String,
    )
}