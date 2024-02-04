package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

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

    public val value: Value get() = Value(tag, rawMask.toLong())

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

    public companion object {
        val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U32
    }
}