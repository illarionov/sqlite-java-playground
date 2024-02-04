package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value

@JvmInline
public value class Device(
    val rawValue: ULong
) {
    val value: Value get() = Value(ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Device.Companion.tag, rawValue.toLong())

    public companion object {
        public val tag = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U64
    }
}