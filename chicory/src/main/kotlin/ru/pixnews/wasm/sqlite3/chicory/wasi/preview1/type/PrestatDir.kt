package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

/**
 * The contents of a `prestat` when type is `preopentype::dir`.
 */
public data class PrestatDir(

    /**
     * The length of the directory name for use with `fd_prestat_dir_name`.
     */
    public val prNameLen: Size // (field $pr_name_len $size)
) {
    public companion object : WasiType {
        public override val valueType: ValueType = ValueType.I32
    }
}