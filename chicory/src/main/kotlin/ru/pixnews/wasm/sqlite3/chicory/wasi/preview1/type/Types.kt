package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

/**
 * Type names used by low-level WASI interfaces.
 * https://raw.githubusercontent.com/WebAssembly/WASI/main/legacy/preview1/witx/typenames.witx
 */

val U8: ValueType = ValueType.I32
val U16: ValueType = ValueType.I32
val S32: ValueType = ValueType.I32
val U32: ValueType = ValueType.I32
val S64: ValueType = ValueType.I64
val U64: ValueType = ValueType.I64
val Handle: ValueType = ValueType.I32
