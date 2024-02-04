package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * A file descriptor handle.
 */
@JvmInline
public value class Fd(
    val value: Value
) {
    constructor(value: Long) : this(Value.i32(value))

    init {
        check(value.type() == ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Fd.Companion.tag)
    }

    public companion object {
        public val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Handle
    }
}