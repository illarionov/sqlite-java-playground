package ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include

data class StructTimespec(
    val tv_sec: time_t,
    val tv_nsec: ULong,
)

typealias time_t = ULong

val StructTimespec.timeMillis
    get(): ULong = tv_sec * 1000U + tv_nsec / 1_000_000U