package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * A reference to the offset of a directory entry.
 *
 * The value 0 signifies the start of the directory.
 */
@JvmInline
public value class Dircookie(
    val rawValue: ULong,
) {
    val value: Value get() = Value(valueType, rawValue.toLong())

    public companion object : WasiType {
        public override val valueType: ValueType = U64
    }
}