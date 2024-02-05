package ru.pixnews.wasm.sqlite3.chicory.sqlite3

import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite3.chicory.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite3.chicory.ext.isNull
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Size

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