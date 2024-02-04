package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * Identifiers for clocks.
 */
enum class ClockId(
    public val id: Value
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

    private constructor(i: Long) : this(Value.i32(i))

    public companion object {
        val tag: ValueType = ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U32
    }
}