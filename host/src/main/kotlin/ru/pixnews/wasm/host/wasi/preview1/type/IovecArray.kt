package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WasmValueType

// (typename $iovec_array (list $iovec))
@JvmInline
value class IovecArray(
    val iovecList: List<Iovec>
) {
    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasmValueType.I32
    }
}