package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

/**
 * Information about a pre-opened capability.
 */
public sealed class Prestat(
    public open val tag: Preopentype
) {
    public data class Dir(
        val prestatDir: PrestatDir
    ) : Prestat(Preopentype.DIR)

    public companion object : WasiType {
        public override val tag: ValueType = ValueType.I32
    }
}