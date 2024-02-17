package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WasmValueType

// (typename $ciovec_array (list $ciovec))
@JvmInline
value class CiovecArray(
    val ciovecList: List<CioVec>
) {
    companion object : WasiTypename {
        override val wasmValueType: WasmValueType = WasiValueTypes.U32
    }
}