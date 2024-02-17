package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * The state of the file descriptor subscribed to with
 * `eventtype::fd_read` or `eventtype::fd_write`.
 */
@JvmInline
public value class Eventrwflags(
    val rawMask: UShort,
) {
    public constructor(
        vararg flags: Eventrwflags
    ) : this(
        flags.fold(0.toUShort()) { acc, flag -> acc.or(flag.mask) }
    )

    public enum class Eventrwflags(
        val mask: UShort
    ) {
        /**
         * The peer of this socket has closed or disconnected.
         */
        FD_READWRITE_HANGUP(0),

        ;

        private constructor(bit: Int) : this(1.shl(bit).toUShort())
    }

    public companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U16
    }
}