package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

/**
 * Subscription to an event.
 *
 * @param userdata User-provided value that is attached to the subscription in the  implementation and returned
 * through `event::userdata`.
 * @param u The type of the event to which to subscribe, and its contents
 */
public data class Subscription(
    val userdata: Userdata, // (field $userdata $userdata)
    val u: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.SubscriptionU, // (field $u $subscription_u)
) {
    public companion object : WasiType {
        public override val tag: ValueType = ValueType.I32
    }
}