package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * Flags provided to `sock_recv`.
 */
@JvmInline
public value class Riflags(
    val rawMask: UShort,
) {
    public constructor(
        vararg flags: RiFlags
    ) : this(
        flags.fold(0.toUShort()) { acc, flag -> acc.or(flag.mask) }
    )

    public val value: Value get() = Value(tag, rawMask.toLong())

    public enum class RiFlags(
        val mask: UShort
    ) {

        /**
         * Returns the message without removing it from the socket's receive queue.
         */
        RECV_PEEK(0),

        /**
         * On byte-stream sockets, block until the full amount of data can be returned.
         */
        RECV_WAITALL(1),

        ;

        private constructor(bit: Int) : this(1.shl(bit).toUShort())
    }

    public companion object {
        val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U16
    }
}