package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType


/**
 * A file descriptor handle.
 */
@JvmInline
public value class Fd(
    val fd: Int
) {
    public companion object : WasiTypename {
        public override val webAssemblyValueType: WebAssemblyValueType = WasiValueTypes.Handle
    }

    override fun toString(): String = "Fd($fd)"
}