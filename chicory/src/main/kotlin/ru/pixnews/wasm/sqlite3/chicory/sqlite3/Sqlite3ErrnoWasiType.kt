package ru.pixnews.wasm.sqlite3.chicory.sqlite3

import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.sqlite3.wasm.Sqlite3Errno
import ru.pixnews.wasm.host.WebAssemblyValueType
import ru.pixnews.wasm.host.WebAssemblyValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.wasi.preview1.type.WasiTypename

public val Sqlite3Errno.value: Value get() = Value.i32(code.toLong())

object Sqlite3ErrnoWasiType : WasiTypename {
    override val webAssemblyValueType: WebAssemblyValueType = I32
}