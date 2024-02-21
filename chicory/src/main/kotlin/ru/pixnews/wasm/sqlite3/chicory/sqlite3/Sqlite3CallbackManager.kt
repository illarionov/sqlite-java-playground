package ru.pixnews.wasm.sqlite3.chicory.sqlite3

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.host.functiontable.IndirectFunctionTableId
import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.functiontable.IndirectFunctionRef
import ru.pixnews.wasm.host.functiontable.IndirectFunctionRef.FuncRef
import ru.pixnews.wasm.sqlite3.chicory.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite3.chicory.host.memory.ChicoryMemoryImpl

class Sqlite3CallbackManager(
    private val memory: ChicoryMemoryImpl,
    private val runtimeInstance: Instance,
    private val bindings: SqliteBindings,
) {
    // (table (;0;) 704 funcref)
    val indirectFunctionTable = runtimeInstance.table(0)
    // val __indirect_function_table = runtimeInstance.export("__indirect_function_table") // 0

    fun registerExecCallback(
        callback: Sqlite3ExecCallback
    ): WasmPtr<Sqlite3ExecCallback> {
        // TODO
        runtimeInstance.module()
        return WasmPtr(7)
    }

    fun unregisterCallback(
        callback: WasmPtr<*>
    ) {

    }

    private fun getFunctionValue(id: IndirectFunctionTableId): IndirectFunctionRef {
        val ref: Value = indirectFunctionTable.ref(id.funcId)
        return when (ref.type()) {
            ValueType.FuncRef -> FuncRef(ref.asFuncRef())
            ValueType.ExternRef -> IndirectFunctionRef.ExternalRef(ref.asExtRef())
            else -> error("Unexpected type")
        }
    }
}