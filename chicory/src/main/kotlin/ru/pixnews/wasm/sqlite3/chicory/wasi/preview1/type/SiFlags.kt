package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * Flags provided to `sock_send`. As there are currently no flags
 * defined, it must be set to zero.
 */
@JvmInline
public value class SiFlags(
    val rawValue: UInt,
) {
    val value: Value get() = Value(tag, rawValue.toLong())

    public companion object {
        public val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U16
    }
}