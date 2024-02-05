package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * Type of a subscription to an event or its occurrence.
 */
public enum class Eventtype(
    public val id: Value
) {
    /**
     * The time value of clock `subscription_clock::id` has
     * reached timestamp `subscription_clock::timeout`.
     */
    CLOCK(0),

    /**
     * File descriptor `subscription_fd_readwrite::file_descriptor` has data
     * available for reading. This event always triggers for regular files.
     */
    FD_READ(1),

    /**
     * File descriptor `subscription_fd_readwrite::file_descriptor` has capacity
     * available for writing. This event always triggers for regular files.
     */
    FD_WRITE(2),

    ;

    private constructor(id: Long) : this(Value.i32(id))

    companion object : WasiType {
        override val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U8
    }
}