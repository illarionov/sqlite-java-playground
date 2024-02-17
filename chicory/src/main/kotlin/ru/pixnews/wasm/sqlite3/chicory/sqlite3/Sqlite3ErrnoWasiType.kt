package ru.pixnews.wasm.sqlite3.chicory.sqlite3

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.sqlite3.wasm.Sqlite3Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.WasiType

public val Sqlite3Errno.value: Value get() = Value.i32(code.toLong())

object  Sqlite3ErrnoWasiType : WasiType {
    override val valueType: ValueType = ValueType.I32
}