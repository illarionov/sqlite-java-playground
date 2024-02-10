package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

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

    public val value: Value get() = Value(valueType, rawMask.toLong())

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

    public companion object : WasiType {
        override val valueType: ValueType = U16
    }
}