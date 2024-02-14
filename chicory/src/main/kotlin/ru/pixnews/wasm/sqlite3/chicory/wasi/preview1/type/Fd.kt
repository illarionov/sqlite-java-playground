package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * A file descriptor handle.
 */
@JvmInline
public value class Fd(
    val fd: Int
) {
    val value: Value get() = Value.i32(fd.toLong())

    public companion object : WasiType {
        public override val valueType: ValueType = Handle
    }

    override fun toString(): String = "Fd($fd)"
}