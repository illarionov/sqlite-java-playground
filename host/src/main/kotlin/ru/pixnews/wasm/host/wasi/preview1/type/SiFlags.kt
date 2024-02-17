package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * Flags provided to `sock_send`. As there are currently no flags
 * defined, it must be set to zero.
 */
@JvmInline
public value class SiFlags(
    val rawValue: UInt,
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U16
    }
}