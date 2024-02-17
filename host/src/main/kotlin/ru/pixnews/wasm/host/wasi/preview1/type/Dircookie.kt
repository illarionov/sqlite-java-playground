package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * A reference to the offset of a directory entry.
 *
 * The value 0 signifies the start of the directory.
 */
@JvmInline
public value class Dircookie(
    val rawValue: ULong,
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U64
    }
}