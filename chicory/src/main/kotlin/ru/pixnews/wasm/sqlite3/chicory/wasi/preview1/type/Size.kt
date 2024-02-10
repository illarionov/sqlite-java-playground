package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value

@JvmInline
public value class Size(
    val value: Value
) {
    constructor(value: UInt) : this(Value.i32(value.toLong()))

    init {
        check(value.type() == valueType)
    }

    public companion object : WasiType {
        public override val valueType = U32
    }
}