package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr.Companion.SQLITE3_NULL

@JvmInline
public value class WasmPtr<P: Any?>(
    val addr: Int
) {
    override fun toString(): String = "0x" + addr.toString(16)

    @Suppress("UNCHECKED_CAST")
    public companion object {
        public const val WASM_SIZEOF_PTR = 4U
        public val SQLITE3_NULL: WasmPtr<*> = WasmPtr<Unit>(0)
        fun <P> sqlite3Null(): WasmPtr<P> = SQLITE3_NULL as WasmPtr<P>
    }
}

public fun WasmPtr<*>.isSqlite3Null(): Boolean = this == SQLITE3_NULL

operator fun <P> WasmPtr<P>.plus(bytes: Int): WasmPtr<P> = WasmPtr(addr + bytes)
