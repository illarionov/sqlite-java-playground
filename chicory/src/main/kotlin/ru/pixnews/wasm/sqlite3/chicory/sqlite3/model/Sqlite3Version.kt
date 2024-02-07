package ru.pixnews.wasm.sqlite3.chicory.sqlite3.model

data class Sqlite3Version(
    val version: String,
    val versionNumber: Int,
    val sourceId: String,
)