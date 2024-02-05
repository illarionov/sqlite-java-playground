package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 *  Which file time attributes to adjust.
 */
@JvmInline
public value class Fstflags(
    val rawMask: UShort,
) {
    public constructor(
        vararg flags: Fstflags
    ) : this(
        flags.fold(0.toUShort()) { acc, flag -> acc.or(flag.mask) }
    )

    public val value: Value get() = Value(tag, rawMask.toLong())

    public enum class Fstflags(
        val mask: UShort,
    ) {
        /**
         * Adjust the last data access timestamp to the value stored in `filestat::atim`.
         */
        ATIM(0),

        /**
         * Adjust the last data access timestamp to the time of clock `clockid::realtime`.
         */
        ATIM_NOW(1),

        /**
         * Adjust the last data modification timestamp to the value stored in `filestat::mtim`.
         */
        MTIM(2),

        /**
         * Adjust the last data modification timestamp to the time of clock `clockid::realtime`.
         */
        MTIM_NOW(3),

        ;

        private constructor(bit: Int) : this(1UL.shl(bit).toUShort())
    }

    public companion object : WasiType {
        override val tag: ValueType = U16
    }
}