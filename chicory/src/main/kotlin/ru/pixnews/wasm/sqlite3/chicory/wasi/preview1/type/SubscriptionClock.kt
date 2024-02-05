package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

/**
 * The contents of a `subscription` when type is `eventtype::clock`.
 *
 * @param id The clock against which to compare the timestamp.
 * @param timeout The absolute or relative timestamp.
 * @param precision The amount of time that the implementation may wait additionally to coalesce with other events.
 * @param flags Flags specifying whether the timeout is absolute or relative
 */
public data class SubscriptionClock(
    val id: ClockId, // (field $id $clockid)
    val timeout: Timestamp, // (field $timeout $timestamp)
    val precision: Timestamp, // (field $precision $timestamp)
    val flags: Subclockflags, // (field $flags $subclockflags)
) {
    public companion object : WasiType {
        public override val tag: ValueType = ValueType.I32
    }
}