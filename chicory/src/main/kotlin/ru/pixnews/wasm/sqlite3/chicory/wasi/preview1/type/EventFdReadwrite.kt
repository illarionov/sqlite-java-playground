package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

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
    companion object : WasiType {
        override val tag: ValueType = U32
    }
}