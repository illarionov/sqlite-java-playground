package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

// (typename $iovec_array (list $iovec))
@JvmInline
value class IovecArray(
    val iovecList: List<Iovec>
)