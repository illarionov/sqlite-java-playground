package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * Non-negative file size or length of a region within a file.
 */
@JvmInline
public value class FileSize(
    val value: ULong
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U64
    }
}