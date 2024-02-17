package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

/**
 * File serial number that is unique within its file system.
 */
@JvmInline
public value class Inode(
    val rawValue: ULong,
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U64
    }
}