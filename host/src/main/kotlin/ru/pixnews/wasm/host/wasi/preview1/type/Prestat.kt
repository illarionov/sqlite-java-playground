package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * Information about a pre-opened capability.
 */
public sealed class Prestat(
    public open val tag: Preopentype
) {
    public data class Dir(
        val prestatDir: PrestatDir
    ) : Prestat(Preopentype.DIR)

    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WebAssemblyValueType.I32
    }
}