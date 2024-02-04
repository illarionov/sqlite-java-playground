package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

/**
 * The contents of a `subscription`.
 */
public sealed class SubscriptionU(
    open val eventType: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Eventtype
) {
    public data class Clock(
        val subscriptionClock: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.SubscriptionClock
    ) : ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.SubscriptionU(ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Eventtype.CLOCK)

    public data class FdRead(
        val subscription_fd_readwrite: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.SubscriptionFdReadwrite
    ) : ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.SubscriptionU(ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Eventtype.FD_READ)

    public data class FdWrite(
        val subscription_fd_readwrite: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.SubscriptionFdReadwrite
    ) : ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.SubscriptionU(ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Eventtype.FD_WRITE)
}