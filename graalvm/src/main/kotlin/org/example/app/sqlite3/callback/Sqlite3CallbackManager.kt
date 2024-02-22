package org.example.app.sqlite3.callback

import java.util.concurrent.atomic.AtomicLong
import org.example.app.bindings.SqliteBindings
import org.example.app.ext.functionTable
import org.example.app.ext.toTypesByteArray
import org.example.app.ext.withWasmContext
import org.example.app.host.memory.GraalHostMemoryImpl
import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmFunctionInstance
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.WasmTable
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.WasmValueType
import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback

class Sqlite3CallbackManager(
    private val context: Context,
    private val memory: GraalHostMemoryImpl,
    private val sqliteBindings: SqliteBindings
) {
    private val moduleNameNo: AtomicLong = AtomicLong(0)
    private val freeFunctionIndexes: MutableSet<Int> = mutableSetOf()

    fun registerExecCallback(
        callback: Sqlite3ExecCallback
    ): WasmPtr<Sqlite3ExecCallback> {
        val module = WasmModule.create(
            "sqlite-exec-cb-${moduleNameNo.getAndIncrement()}",
            null
        )
        val funcTypeIndex = module.allocateFunctionType(
            listOf(WasmValueType.I32, WasmValueType.I32, WasmValueType.I32, WasmValueType.I32).toTypesByteArray(),
            listOf(WasmValueType.I32).toTypesByteArray(),
            false
        )
        val func = module.declareExportedFunction(funcTypeIndex, "e")

        context.withWasmContext { wasmContext: WasmContext ->
            val envInstance: WasmInstance = wasmContext.readInstance(module)
            val adapter = Sqlite3CallExecAdapter(
                wasmContext.language(),
                envInstance,
                callback,
            )
            envInstance.setTarget(func.index(), adapter.callTarget)

            val funcInstance: WasmFunctionInstance = envInstance.functionInstance(func)

            val functionTable = wasmContext.functionTable

            val newId = allocateIndirectTableFunctionId(functionTable)
            try {
                functionTable.set(newId, funcInstance)
                wasmContext.linker().tryLink(envInstance)
                return WasmPtr(newId)
            } catch (e: Throwable) {
                freeIndirectTableFunctionId(newId)
                throw e
            }
        }
    }

    fun unregisterCallback(
        id: WasmPtr<Sqlite3ExecCallback>
    ) {
        val idx = id.addr
        context.withWasmContext { wasmContext: WasmContext ->
            val functionTable = wasmContext.functionTable
            val oldFunc = functionTable.get(idx) as WasmFunctionInstance
            functionTable.set(idx, null)

            // TODO: How to unload module?
            oldFunc
        }
    }

    private fun allocateIndirectTableFunctionId(
        functionTable: WasmTable
    ): Int {
        while (freeFunctionIndexes.isNotEmpty()) {
            val idx = freeFunctionIndexes.last()
            if (functionTable.get(idx) == null) {
                val isRemoved = freeFunctionIndexes.remove(idx)
                require(isRemoved)
                return idx
            }
        }
        val lastId = functionTable.grow(1, null)
        return lastId
    }

    private fun freeIndirectTableFunctionId(
        id: Int
    ) {
        freeFunctionIndexes.add(id)
    }
}