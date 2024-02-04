package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value

/**
 * A region of memory for scatter/gather writes.
 *
 * @param buf The address of the buffer to be written.
 * @param bufLen The length of the buffer to be written.
 */
data class CioVec(
    val buf: Value, // (@witx const_pointer u8))
    val bufLen: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Size // (field $buf_len $size)
)