package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * The position relative to which to set the offset of the file descriptor.
 */
public enum class Whence(
    private val code: Int
) {
    /**
     * Seek relative to start-of-file.
     */
    SET(0),

    /**
     * Seek relative to current position.
     */
    CUR(1),

    /**
     * Seek relative to end-of-file.
     */
    END(2),

    ;

    public val id: Value
        get() = Value.i32(this.code.toLong())

    public companion object : WasiType {
        override val valueType: ValueType = U8

        fun fromIdOrNull(whence: Int): Whence? = entries.firstOrNull { it.code == whence }
    }
}