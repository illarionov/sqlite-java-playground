package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8

/**
 * Type of a subscription to an event or its occurrence.
 */
public enum class Eventtype(
    public val id: Int
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

    private constructor(id: Long) : this(id.toInt())

    companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = U8
    }
}