package ru.pixnews.sqlite3.wasm

class Sqlite3Exception(
    val sqliteErrorCode: Int,
    val sqliteExtendedErrorCode: Int,
    private val prefix: String? = null,
    val sqliteMsg: String? = null
) : RuntimeException(
    sqlite3ErrorMsg(sqliteErrorCode, sqliteErrorCode, prefix, sqliteMsg)
) {
    public companion object {
        fun sqlite3ErrNoName(errNo: Int): String = Sqlite3Errno.fromErrNoCode(errNo)?.toString() ?: errNo.toString()

        fun sqlite3ErrorMsg(
            sqliteErrorCode: Int,
            sqliteExetendedErrorCode: Int,
            prefix: String? = null,
            sqliteMsg: String? = null
        ): String = buildString {
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
    }
}