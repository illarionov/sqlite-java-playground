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
        check(value.type() == valueType)
    }

    public companion object : WasiType {
        public override val valueType: ValueType = Handle
    }
}