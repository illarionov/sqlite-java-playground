package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

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

    public companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U8

        fun fromIdOrNull(whence: Int): Whence? = entries.firstOrNull { it.code == whence }
    }
}