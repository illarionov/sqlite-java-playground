package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * Identifiers for preopened capabilities.
 */
public enum class Preopentype(
    val value: Value,
) {
    /**
     * A pre-opened directory.
     */
    DIR(0)

    ;

    private constructor(i: Long) : this(Value.i32(i))

    public companion object : WasiType {
        override val valueType: ValueType = U8
    }
}