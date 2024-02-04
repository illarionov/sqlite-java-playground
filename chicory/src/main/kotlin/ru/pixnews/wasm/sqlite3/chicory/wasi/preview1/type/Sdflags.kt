package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

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

    public val value: Value get() = Value(tag, rawMask.toLong())


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

        private constructor(bit: Int) : this(1U.shl(bit).toUByte())
    }
    public companion object {
        val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U8
    }
}