package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WasmValueType

/**
 * File descriptor flags.
 */
@JvmInline
public value class FdFlags(
    val rawMask: UShort,
) {
    public constructor(
        vararg flags: Flags
    ) : this(
        flags.fold(0.toUShort()) { acc, flag -> acc.or(flag.mask) }
    )

    public enum class Flags(
        val mask: UShort
    ) {
        /**
         * Append mode: Data written to the file is always appended to the file's end.
         */
        APPEND(0),

        /**
         * Write according to synchronized I/O data integrity completion. Only the data stored in the file is synchronized.
         */
        DSYNC(1),

        /**
         * Non-blocking mode.
         */
        NONBLOCK(2),

        /**
         * Synchronized read I/O operations.
         */
        RSYNC(3),

        /**
         * Write according to synchronized I/O file integrity completion. In
         * addition to synchronizing the data stored in the file, the implementation
         * may also synchronously update the file's metadata.
         */
        SYNC(4)

        ;

        private constructor(bit: Int) : this(1.shl(bit).toUShort())
    }

    public companion object : WasiTypename {
        override val wasmValueType: WasmValueType = WasiValueTypes.U16
    }
}