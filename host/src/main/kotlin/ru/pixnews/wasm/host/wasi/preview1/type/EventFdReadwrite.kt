package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * The contents of an `event` when type is `eventtype::fd_read` or
 * `eventtype::fd_write`.
 *
 * @param nbytes The number of bytes available for reading or writing.
 * @param flags The state of the file descriptor.
 */
public data class EventFdReadwrite(
    val nbytes: FileSize, // (field $nbytes $filesize)
    val flags: Eventrwflags, // field $flags $eventrwflags)
) {
    companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U32
    }
}