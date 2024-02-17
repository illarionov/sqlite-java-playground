package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WasmValueType

/**
 * The contents of a `subscription`.
 */
public sealed class SubscriptionU(
    open val eventType: Eventtype
) {
    public data class Clock(
        val subscriptionClock: SubscriptionClock
    ) : SubscriptionU(Eventtype.CLOCK)

    public data class FdRead(
        val subscriptionFdReadwrite: SubscriptionFdReadwrite
    ) : SubscriptionU(Eventtype.FD_READ)

    public data class FdWrite(
        val subscriptionFdReadwrite: SubscriptionFdReadwrite
    ) : SubscriptionU(Eventtype.FD_WRITE)

    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasmValueType.I32
    }
}