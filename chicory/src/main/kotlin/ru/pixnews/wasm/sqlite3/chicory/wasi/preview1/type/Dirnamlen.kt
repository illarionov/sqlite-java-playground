package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * The type for the `dirent::d_namlen` field of `dirent` struct.
 */
@JvmInline
public value class Dirnamlen(
    val rawValue: UInt,
)  {
    val value: Value get() = Value(valueType, rawValue.toLong())

    public companion object : WasiType {
        public override val valueType: ValueType = U32
    }
}