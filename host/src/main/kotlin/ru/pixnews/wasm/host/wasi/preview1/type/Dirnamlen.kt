package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WasmValueType

/**
 * The type for the `dirent::d_namlen` field of `dirent` struct.
 */
@JvmInline
public value class Dirnamlen(
    val rawValue: UInt,
)  {
    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasiValueTypes.U32
    }
}