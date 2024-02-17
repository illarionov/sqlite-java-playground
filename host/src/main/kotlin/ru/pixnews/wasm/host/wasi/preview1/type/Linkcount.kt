package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * Number of hard links to an inode.
 */
@JvmInline
public value class Linkcount(
    val rawValue: ULong,
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U64
    }
}