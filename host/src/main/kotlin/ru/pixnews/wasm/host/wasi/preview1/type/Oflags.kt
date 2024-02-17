package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * Open flags used by `path_open`.
 */
@JvmInline
public value class Oflags(
    val rawMask: UShort,
)  {
    public constructor(
        vararg flags: Oflags
    ) : this(
        flags.fold(0.toUShort()) { acc, flag -> acc.or(flag.mask) }
    )

    public enum class Oflags(
        val mask: UShort
    ) {
        /**
         * Create file if it does not exist.
         */
        CREAT(0),

        /**
         * Fail if not a directory.
         */
        DIRECTORY(1),

        /**
         * Fail if file already exists.
         */
        EXCL(2),

        /**
         * Truncate file to size 0.
         */
        TRUNC(3),

        ;

        private constructor(bit: Int) : this(1.shl(bit).toUShort())
    }

    public companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U16
    }
}