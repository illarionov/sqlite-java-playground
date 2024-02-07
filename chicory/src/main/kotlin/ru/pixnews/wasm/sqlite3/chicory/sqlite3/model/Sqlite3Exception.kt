package ru.pixnews.wasm.sqlite3.chicory.sqlite3.model

class Sqlite3Exception(
    val sqliteErrorCode: Int,
    val sqliteExetendedErrorCode: Int,
    private val prefix: String? = null,
    private val sqliteMsg: String? = null
) : RuntimeException(
    buildString {
        append("Sqlite error ")
        append(sqlite3ErrNoName(sqliteErrorCode))
        append("/")
        append(sqlite3ErrNoName(sqliteExetendedErrorCode))
        if (prefix?.isNotEmpty() == true) {
            append(": ")
            append(prefix)
        }
        if (sqliteMsg != null) {
            append("; ")
            append(sqliteMsg)
        }
    }
) {
    public companion object {
        fun sqlite3ErrNoName(errNo: Int): String = Sqlite3Errno.fromErrNoCode(errNo)?.toString() ?: errNo.toString()
    }
}