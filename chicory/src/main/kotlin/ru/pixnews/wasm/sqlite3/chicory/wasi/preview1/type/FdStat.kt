package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

/**
 * File descriptor attributes.
 *
 * @param fsFiletype File type.
 * @param fsFlags File descriptor flags.
 * @param fsRightsBase Rights that apply to this file descriptor.
 * @param fsRightsInheriting Maximum set of rights that may be installed on new file descriptors that are created
 * through this file descriptor, e.g., through `path_open`.
 */
public data class FdStat(
    val fsFiletype: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Filetype, // (field $fs_filetype $filetype)
    val fsFlags: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.FdFlags, // (field $fs_flags $fdflags)
    val fsRightsBase: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Rights, // (field $fs_rights_base $rights)
    val fsRightsInheriting: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Rights // (field $fs_rights_inheriting $rights)
)