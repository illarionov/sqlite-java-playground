package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

public interface WasiType {
    val valueType: ValueType
}

public val WasiType.pointer: ValueType
    get() = ValueType.I32

public val ValueType.pointer: ValueType
    get() = ValueType.I32
