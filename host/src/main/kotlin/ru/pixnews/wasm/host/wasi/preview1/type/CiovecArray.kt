package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

// (typename $ciovec_array (list $ciovec))
@JvmInline
value class CiovecArray(
    val ciovecList: List<CioVec>
) {
    companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U32
    }
}