package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.wasm.types.ValueType

internal object ParamTypes {
    val i32 = listOf(ValueType.I32)
    val i32i32 = listOf(ValueType.I32, ValueType.I32)
    val i32i32i32 = listOf(ValueType.I32, ValueType.I32, ValueType.I32)
    val i32i32i32i32 = listOf(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32)
    val i32i32i32i32i32 = List(5) { ValueType.I32 }
}