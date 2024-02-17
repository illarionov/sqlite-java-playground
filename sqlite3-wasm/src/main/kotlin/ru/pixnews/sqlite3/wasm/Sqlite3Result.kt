package ru.pixnews.sqlite3.wasm

public sealed class Sqlite3Result<out T : Any> {
    public data class Success<out T : Any>(
        val value: T
    ) : Sqlite3Result<T>()

    public data class Error(
        val sqliteErrorCode: Int,
        val sqliteExtendedErrorCode: Int,
        val sqliteMsg: String? = null
    ) : Sqlite3Result<Nothing>()
}