package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * User-provided value that may be attached to objects that is retained when
 * extracted from the implementation.
 */
@JvmInline
public value class Userdata(
    val rawValue: ULong,
) {
    val value: Value get() = Value(tag, rawValue.toLong())

    public companion object : WasiType {
        public override val tag: ValueType = U64
    }
}