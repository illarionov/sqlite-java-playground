package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8

/**
 * Which channels on a socket to shut down.
 */
@JvmInline
public value class Sdflags(
    val rawMask: UByte,
) {
    public constructor(
        vararg flags: Sdflags
    ) : this(
        flags.fold(0.toUByte()) { acc, flag -> acc.or(flag.mask) }
    )

    public enum class Sdflags(
        val mask: UByte
    ) {
        /**
         * Disables further receive operations.
         */
        RD(0),

        /**
         * Disables further send operations.
         */
        WR(1)

        ;

        constructor(bit: Int) : this(1U.shl(bit).toUByte())
    }
    public companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = U8
    }
}