package ru.pixnews.sqlite3.wasm

import java.net.URL

public object Sqlite3Wasm {
    val factorialWasm: URL
        get() = getUrl("factorial.wasm")

    val factorialWat: URL
        get() = getUrl("factorial.wat")

    public object Emscripten {
        public val sqlite3_345: URL
            get() = getUrl("sqlite3_3450000.wasm")

        val sqlite3_346: URL
            get() = getUrl("sqlite3_3460000.wasm")

        val sqlite3_346_debug: URL
            get() = getUrl("sqlite3_3460000_o0_stripped.wasm")

        val sqlite3_346_debug_unstripped: URL
            get() = getUrl("sqlite3_3460000_o0_unstripped.wasm")
    }

    public object WasiSdk {
        public val sqlite3_346: URL
            get() = getUrl("sqlite3-wasi-sdk.wasm")
    }


    private fun getUrl(fileName: String): URL  = requireNotNull(Sqlite3Wasm::class.java.getResource(fileName))
}