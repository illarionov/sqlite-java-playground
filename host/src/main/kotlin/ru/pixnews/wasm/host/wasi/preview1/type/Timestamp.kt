package ru.pixnews.wasm.host.wasi.preview1.type

/**
 * Timestamp in nanoseconds.
 */
@JvmInline
public value class Timestamp(
    val value: ULong
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType = WasiValueTypes.U64
    }
}