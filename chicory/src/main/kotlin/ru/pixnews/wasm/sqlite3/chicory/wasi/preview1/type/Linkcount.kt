package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * Number of hard links to an inode.
 */
@JvmInline
public value class Linkcount(
    val rawValue: ULong,
) {
    val value: Value get() = Value(valueType, rawValue.toLong())

    public companion object : WasiType {
        public override val valueType: ValueType = U64
    }
}