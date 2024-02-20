package ru.pixnews.wasm.sqlite3.chicory.sqlite3

import com.dylibso.chicory.runtime.Instance
import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import ru.pixnews.wasm.sqlite3.chicory.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite3.chicory.host.memory.ChicoryMemoryImpl

class Sqlite3CallbackManager(
    private val memory: ChicoryMemoryImpl,
    private val runtimeInstance: Instance,
    private val bindings: SqliteBindings,
) {

    fun registerExecCallback(
        callback: Sqlite3ExecCallback
    ): WasmPtr<Sqlite3ExecCallback> {
        return WasmPtr(0x12345);
        //return 0
    }

    fun unregisterCallback(
        callback: WasmPtr<*>
    ) {

    }
}