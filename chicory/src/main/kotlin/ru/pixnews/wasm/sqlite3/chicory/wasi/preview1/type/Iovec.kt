package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * A region of memory for scatter/gather reads.
 *
 * @param buf The address of the buffer to be filled.
 * @param bufLen The length of the buffer to be filled.
 */
data class Iovec(
    val buf: Value, // (@witx const_pointer u8))
    val bufLen: Size // (field $buf_len $size)
) {
    public companion object : WasiType {
        public override val tag: ValueType = ValueType.I32
    }
}