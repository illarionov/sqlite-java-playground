package ru.pixnews.wasm.sqlite3.chicory.bindings

import com.dylibso.chicory.wasm.types.Value

class Sqlite3Error(
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
    constructor(
        sqliteErrorCode: Value,
        sqliteExtendedErrorCode: Value,
        msg: String? = null,
        sqliteMsg: String? = null,
    ) : this(
        sqliteErrorCode.asInt(),
        sqliteExtendedErrorCode.asInt(),
        msg,
        sqliteMsg,
    )

    public companion object {
        fun sqlite3ErrNoName(errNo: Int): String = Sqlite3Errno.fromErrNoCode(errNo)?.toString() ?: errNo.toString()
    }
}

