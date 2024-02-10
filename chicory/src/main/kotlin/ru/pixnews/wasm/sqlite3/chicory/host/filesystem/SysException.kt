package ru.pixnews.wasm.sqlite3.chicory.host.filesystem

import java.lang.RuntimeException
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno

class SysException(
    public val errNo: Errno,
    val msg: String? = null,
    cause: Throwable? = null,
) : RuntimeException(msg, cause)
