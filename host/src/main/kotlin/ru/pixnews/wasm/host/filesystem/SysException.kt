package ru.pixnews.wasm.host.filesystem

import java.lang.RuntimeException
import ru.pixnews.wasm.host.wasi.preview1.type.Errno

class SysException(
    public val errNo: Errno,
    val msg: String? = null,
    cause: Throwable? = null,
) : RuntimeException(msg, cause)