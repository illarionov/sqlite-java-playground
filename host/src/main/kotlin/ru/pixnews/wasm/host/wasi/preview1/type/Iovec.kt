package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * A region of memory for scatter/gather reads.
 *
 * @param buf The address of the buffer to be filled.
 * @param bufLen The length of the buffer to be filled.
 */
data class Iovec(
    val buf: WasmPtr, // (@witx const_pointer u8))
    val bufLen: Size // (field $buf_len $size)
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WebAssemblyValueType.I32
    }
}