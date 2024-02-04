package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * Non-negative file size or length of a region within a file.
 */
@JvmInline
public value class FileSize(
    val value: Value
) {
    constructor(value: ULong) : this(Value.i64(value.toLong()))

    init {
        check(value.type() == tag)
    }

    public companion object {
        public val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U64
    }
}