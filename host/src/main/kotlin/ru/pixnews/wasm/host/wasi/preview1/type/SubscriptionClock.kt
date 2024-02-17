package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

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
    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WebAssemblyValueType.I32
    }
}