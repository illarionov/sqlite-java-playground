package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

@JvmInline
public value class ExitCode(
    val rawValue: UInt,
) {
    val value: Value get() = Value(tag, rawValue.toLong())

    public companion object : WasiType {
        public override val tag: ValueType = U32
    }
}