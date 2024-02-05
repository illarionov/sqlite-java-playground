package ru.pixnews.wasm.sqlite3.chicory.bindings

import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno

class Sqlite3Error(
    val errNo: Int,
    private val prefix: String? = null
) : RuntimeException(
    buildString {
        append("Sqlite error ")
        append(errNoName(errNo))
        if (prefix?.isNotEmpty() == true) {
            append(": ")
            append(prefix)
        }
    }
) {
    constructor(
        errNo: Value,
        msg: String? = null
    ) : this(errNo.asInt(), msg)

    public companion object {
        fun errNoName(errNo: Int): String = Errno.fromErrNoCode(errNo)?.name ?: errNo.toString()
    }
}

