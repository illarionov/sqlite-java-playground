package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WasmValueType

/**
 * The contents of a `prestat` when type is `preopentype::dir`.
 */
public data class PrestatDir(

    /**
     * The length of the directory name for use with `fd_prestat_dir_name`.
     */
    public val prNameLen: Size // (field $pr_name_len $size)
) {
    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasmValueType.I32
    }
}