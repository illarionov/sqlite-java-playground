package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8

/**
 * Identifiers for preopened capabilities.
 */
public enum class Preopentype(
    val value: UInt,
) {
    /**
     * A pre-opened directory.
     */
    DIR(0U)

    ;

    public companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = U8
    }
}