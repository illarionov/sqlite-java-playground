package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

/**
 * The contents of a `subscription` when type is type is `eventtype::fd_read` or `eventtype::fd_write`.
 *
 * @param fileDescriptor The file descriptor on which to wait for it to become ready for reading or writing.
 */
public data class SubscriptionFdReadwrite(
    val fileDescriptor: Fd // (field $file_descriptor $fd)
) {
    public companion object : WasiType {
        public override val tag: ValueType = ValueType.I32
    }
}