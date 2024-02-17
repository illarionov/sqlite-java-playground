package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WasmValueType

/**
 * Flags determining the method of how paths are resolved.
 */
@JvmInline
public value class LookupFlags(
    val rawMask: UInt,
) {
    public constructor(
        vararg flags: LookupFlag
    ) : this(
        flags.fold(0U) { acc, flag -> acc.or(flag.mask) }
    )

    public enum class LookupFlag(
        val mask: UInt,
    ) {

        /**
         * As long as the resolved path corresponds to a symbolic link, it is expanded.
         */
        SYMLINK_FOLLOW(0),

        ;

        private constructor(bit: Byte) : this(1U.shl(bit.toInt()))
    }

    public companion object : WasiTypename {
        override val wasmValueType: WasmValueType = WasiValueTypes.U32
    }
}