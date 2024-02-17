package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WasmValueType

/**
 * User-provided value that may be attached to objects that is retained when
 * extracted from the implementation.
 */
@JvmInline
public value class Userdata(
    val rawValue: ULong,
) {
    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasiValueTypes.U64
    }
}