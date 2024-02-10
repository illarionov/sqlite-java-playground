package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

@JvmInline
public value class ExitCode(
    val rawValue: UInt,
) {
    val value: Value get() = Value(valueType, rawValue.toLong())

    public companion object : WasiType {
        public override val valueType: ValueType = U32
    }
}