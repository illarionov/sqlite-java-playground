package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * Identifiers for clocks.
 */
enum class ClockId(
    public val id: Int
) {
    /**
     * The clock measuring real time. Time value zero corresponds with 1970-01-01T00:00:00Z.
     */
    REALTIME(0),

    /**
     * The store-wide monotonic clock, which is defined as a clock measuring
     * real time, whose value cannot be adjusted and which cannot have negative
     * clock jumps. The epoch of this clock is undefined. The absolute time
     * value of this clock therefore has no meaning.
     */
    MONOTONIC(1),

    /**
     * The CPU-time clock associated with the current process.
     */
    PROCESS_CPUTIME_ID(2),

    /**
     * The CPU-time clock associated with the current thread.
     */
    THREAD_CPUTIME_ID(3),

    ;

    public companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U32
    }
}