package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * Relative offset within a file.
 */
@JvmInline
public value class FileDelta(
    val rawValue: Int,
) {
    val value: Value get() = Value(tag, rawValue)

    public companion object : WasiType {
        public override val tag: ValueType = ValueType.I32
    }
}