package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

/**
 * The contents of a `subscription` when type is `eventtype::clock`.
 *
 * @param id The clock against which to compare the timestamp.
 * @param timeout The absolute or relative timestamp.
 * @param precision The amount of time that the implementation may wait additionally to coalesce with other events.
 * @param flags Flags specifying whether the timeout is absolute or relative
 */
public data class SubscriptionClock(
    val id: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.ClockId, // (field $id $clockid)
    val timeout: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Timestamp, // (field $timeout $timestamp)
    val precision: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Timestamp, // (field $precision $timestamp)
    val flags: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Subclockflags, // (field $flags $subclockflags)
)