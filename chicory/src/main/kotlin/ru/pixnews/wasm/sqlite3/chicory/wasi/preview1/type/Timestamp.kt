package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value

/**
 * Timestamp in nanoseconds.
 */
@JvmInline
public value class Timestamp(
    val value: Value
) {
    constructor(value: ULong) : this(Value.i64(value.toLong()))

    init {
        check(value.type() == ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Timestamp.valueType)
    }

    public companion object : WasiType {
        public override val valueType = U64
    }
}