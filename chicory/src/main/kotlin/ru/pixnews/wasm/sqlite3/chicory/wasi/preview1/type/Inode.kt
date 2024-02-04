package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * File serial number that is unique within its file system.
 */
@JvmInline
public value class Inode(
    val rawValue: ULong,
) {
    val value: Value get() = Value(tag, rawValue.toLong())

    public companion object {
        public val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U64
    }
}