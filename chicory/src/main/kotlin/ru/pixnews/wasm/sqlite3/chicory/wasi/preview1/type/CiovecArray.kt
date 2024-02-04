package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

// (typename $ciovec_array (list $ciovec))
@JvmInline
value class CiovecArray(
    val ciovecList: List<Iovec>
)