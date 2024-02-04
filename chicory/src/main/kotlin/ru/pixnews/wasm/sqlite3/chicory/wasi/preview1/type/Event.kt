package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

/**
 * An event that occurred.
 *
 * @param userdata User-provided value that got attached to `subscription::userdata`.
 * @param error (field $error $errno)
 * @param type The type of event that occured
 * @param fd_readwrite The contents of the event, if it is an `eventtype::fd_read` or `eventtype::fd_write`.
 * `eventtype::clock` events ignore this field.
 */
public data class Event(
    val userdata: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Userdata, // (field $userdata $userdata)
    val error: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno, // (field $error $errno)
    val type: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Eventtype, // (field $type $eventtype)
    val fdReadwrite: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.EventFdReadwrite, // (field $fd_readwrite $event_fd_readwrite)
)