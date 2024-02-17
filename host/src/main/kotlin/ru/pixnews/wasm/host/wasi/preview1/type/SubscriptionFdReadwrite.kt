package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * The contents of a `subscription` when type is type is `eventtype::fd_read` or `eventtype::fd_write`.
 *
 * @param fileDescriptor The file descriptor on which to wait for it to become ready for reading or writing.
 */
public data class SubscriptionFdReadwrite(
    val fileDescriptor: Fd // (field $file_descriptor $fd)
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WebAssemblyValueType.I32
    }
}