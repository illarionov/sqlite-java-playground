package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * The type for the `dirent::d_namlen` field of `dirent` struct.
 */
@JvmInline
public value class Dirnamlen(
    val rawValue: UInt,
) {
    val value: Value get() = Value(tag, rawValue.toLong())

    public companion object {
        public val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U32
    }
}