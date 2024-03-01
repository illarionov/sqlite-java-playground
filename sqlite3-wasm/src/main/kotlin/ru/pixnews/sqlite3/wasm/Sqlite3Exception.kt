package ru.pixnews.sqlite3.wasm

class Sqlite3Exception(
    val errorInfo: Sqlite3ErrorInfo,
    prefix: String? = null,
) : RuntimeException(
    errorInfo.formatErrorMessage(prefix)
) {
     constructor(
         sqliteErrorCode: Int,
         sqliteExtendedErrorCode: Int,
         prefix: String? = null,
         sqliteMsg: String? = null
     ) : this(Sqlite3ErrorInfo(sqliteErrorCode, sqliteExtendedErrorCode, sqliteMsg), prefix)

    public companion object {
        val Sqlite3Exception.sqlite3ErrNoName: String get() = sqlite3ErrNoName(errorInfo.sqliteExtendedErrorCode)

        internal fun sqlite3ErrNoName(errNo: Int): String = Sqlite3Errno.fromErrNoCode(errNo)?.toString() ?: errNo.toString()
    }
}

public data class Sqlite3ErrorInfo(
    val sqliteErrorCode: Int,
    val sqliteExtendedErrorCode: Int = sqliteErrorCode,
    val sqliteMsg: String? = null,
)

fun Sqlite3ErrorInfo.formatErrorMessage(prefix: String?)  = buildString {
    append("Sqlite error ")
    append(Sqlite3Exception.sqlite3ErrNoName(sqliteErrorCode))
    append("/")
    append(Sqlite3Exception.sqlite3ErrNoName(sqliteExtendedErrorCode))
    if (prefix?.isNotEmpty() == true) {
        append(": ")
        append(prefix)
    }
    if (sqliteMsg != null) {
        append("; ")
        append(sqliteMsg)
    }
}