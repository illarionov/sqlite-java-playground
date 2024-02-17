package ru.pixnews.sqlite3.wasm

data class Sqlite3Version(
    val version: String,
    val versionNumber: Int,
    val sourceId: String,
)