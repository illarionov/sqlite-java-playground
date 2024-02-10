package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

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

    public companion object : WasiType {
        public override val valueType: ValueType = ValueType.I32
    }
}