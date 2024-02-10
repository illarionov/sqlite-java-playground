package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

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
    val fsFiletype: Filetype, // (field $fs_filetype $filetype)
    val fsFlags: FdFlags, // (field $fs_flags $fdflags)
    val fsRightsBase: Rights, // (field $fs_rights_base $rights)
    val fsRightsInheriting: Rights // (field $fs_rights_inheriting $rights)
)  {
    public companion object : WasiType {
        override val valueType: ValueType = ValueType.I32
    }
}