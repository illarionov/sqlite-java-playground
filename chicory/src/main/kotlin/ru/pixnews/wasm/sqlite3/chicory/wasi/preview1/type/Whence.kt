package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * The position relative to which to set the offset of the file descriptor.
 */
public enum class Whence(
    public val id: Value
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

    private constructor(id: Long) : this(Value.i32(id))

    public companion object {
        val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U8
    }
}