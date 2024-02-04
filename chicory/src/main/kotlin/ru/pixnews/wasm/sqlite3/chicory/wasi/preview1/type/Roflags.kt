package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * Flags returned by `sock_recv`.
 */
@JvmInline
public value class Roflags(
    val rawMask: UShort,
) {
    public constructor(
        vararg flags: RoFlags
    ) : this(
        flags.fold(0.toUShort()) { acc, flag -> acc.or(flag.mask) }
    )

    public val value: Value get() = Value(ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.FdFlags.tag, rawMask.toLong())


    public enum class RoFlags(
        val mask: UShort
    ) {
        /**
         * Returned by `sock_recv`: Message data has been truncated.
         */
        RECV_DATA_TRUNCATED(0)

        ;

        private constructor(bit: Int) : this(1.shl(bit).toUShort())
    }

    public companion object {
        val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U16
    }
}