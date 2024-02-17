package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WasmValueType


/**
 * A file descriptor handle.
 */
@JvmInline
public value class Fd(
    val fd: Int
) {
    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasiValueTypes.Handle
    }

    override fun toString(): String = "Fd($fd)"
}