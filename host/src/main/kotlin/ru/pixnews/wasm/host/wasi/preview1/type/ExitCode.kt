package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType


@JvmInline
public value class ExitCode(
    val rawValue: UInt,
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.U32
    }
}