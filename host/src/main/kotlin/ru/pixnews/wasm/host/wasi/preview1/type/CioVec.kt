package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType


/**
 * A region of memory for scatter/gather writes.
 *
 * @param buf The address of the buffer to be written.
 * @param bufLen The length of the buffer to be written.
 */
data class CioVec(
    val buf: WasmPtr, // (@witx const_pointer u8))
    val bufLen: Size // (field $buf_len $size)
) {
    companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U32
    }
}