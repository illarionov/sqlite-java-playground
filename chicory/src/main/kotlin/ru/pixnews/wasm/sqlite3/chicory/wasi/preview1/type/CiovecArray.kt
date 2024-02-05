package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

// (typename $ciovec_array (list $ciovec))
@JvmInline
value class CiovecArray(
    val ciovecList: List<Iovec>
) {
    companion object : WasiType {
        override val tag: ValueType = U32
    }
}