package ru.pixnews.wasm.host.wasi.preview1.type

@JvmInline
public value class Size(
    val value: UInt
) {
    public companion object : WasiTypename {
        public override val wasmValueType = WasiValueTypes.U32
    }
}