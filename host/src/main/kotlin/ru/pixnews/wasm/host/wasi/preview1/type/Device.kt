package ru.pixnews.wasm.host.wasi.preview1.type

@JvmInline
public value class Device(
    val rawValue: ULong
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType = WasiValueTypes.U64
    }
}