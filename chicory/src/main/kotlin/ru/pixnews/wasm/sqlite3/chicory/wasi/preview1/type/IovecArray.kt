package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

// (typename $iovec_array (list $iovec))
@JvmInline
value class IovecArray(
    val iovecList: List<Iovec>
) {
    public companion object : WasiType {
        public override val valueType: ValueType = ValueType.I32
    }
}