package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * Flags determining how to interpret the timestamp provided in `subscription_clock::timeout`.
 */
@JvmInline
public value class Subclockflags(
    val rawMask: UShort,
) {
    public constructor(
        vararg flags: Subclockflags
    ) : this(
        flags.fold(0.toUShort()) { acc, flag -> acc.or(flag.mask) }
    )

    public enum class Subclockflags(
        val mask: UShort,
    ) {

        /**
         * If set, treat the timestamp provided in
         * `subscription_clock::timeout` as an absolute timestamp of clock
         * `subscription_clock::id`. If clear, treat the timestamp
         * provided in `subscription_clock::timeout` relative to the
         * current time value of clock `subscription_clock::id`.
         */
        SUBSCRIPTION_CLOCK_ABSTIME(0)

        ;

        private constructor(bit: Int) : this(1.shl(bit).toUShort())
    }

    public companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U16
    }
}