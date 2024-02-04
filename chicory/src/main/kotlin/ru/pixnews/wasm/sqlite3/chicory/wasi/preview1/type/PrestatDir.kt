package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

/**
 * The contents of a `prestat` when type is `preopentype::dir`.
 */
public data class PrestatDir(

    /**
     * The length of the directory name for use with `fd_prestat_dir_name`.
     */
    public val prNameLen: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Size // (field $pr_name_len $size)
)