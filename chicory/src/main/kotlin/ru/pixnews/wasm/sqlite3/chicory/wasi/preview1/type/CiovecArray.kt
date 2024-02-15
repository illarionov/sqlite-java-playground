package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

// (typename $ciovec_array (list $ciovec))
@JvmInline
value class CiovecArray(
    val ciovecList: List<CioVec>
) {
    companion object : WasiType {
        override val valueType: ValueType = U32
    }
}