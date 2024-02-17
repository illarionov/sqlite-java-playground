package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * Relative offset within a file.
 */
@JvmInline
public value class FileDelta(
    val rawValue: Int,
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WebAssemblyValueType.I32
    }
}