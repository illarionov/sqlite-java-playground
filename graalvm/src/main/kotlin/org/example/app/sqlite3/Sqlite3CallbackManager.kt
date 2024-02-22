package org.example.app.sqlite3

import java.util.concurrent.atomic.AtomicLong
import org.example.app.bindings.SqliteBindings
import org.example.app.ext.functionTable
import org.example.app.ext.toTypesByteArray
import org.example.app.ext.withWasmContext
import org.example.app.host.memory.GraalHostMemoryImpl
import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback

class Sqlite3CallbackManager(
    private val context: Context,
    private val memory: GraalHostMemoryImpl,
    private val sqliteBindings: SqliteBindings
) {
    private val moduleNameNo: AtomicLong = AtomicLong(0)

    fun registerExecCallback(
        callback: Sqlite3ExecCallback
    ): WasmPtr<Sqlite3ExecCallback> {
        val module = WasmModule.create(
            "sqlite-exec-cb-${moduleNameNo.getAndIncrement()}",
            null
        )
        val funcTypeIndex = module.allocateFunctionType(
            listOf(I32, I32, I32, I32).toTypesByteArray(),
            listOf(I32).toTypesByteArray(),
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

            val funcInstance = envInstance.functionInstance(func)

            val indirectFunctionTable = wasmContext.functionTable
            val oldSize = indirectFunctionTable.grow(1, funcInstance)

            wasmContext.linker().tryLink(envInstance)

            return WasmPtr(oldSize)
        }
    }

    fun unregisterCallback(
        callback: WasmPtr<*>
    ) {

    }

//    private fun getFunctionValue(id: IndirectFunctionTableId): IndirectFunctionRef {
//        val ref = indirectFunctionTable.getArrayElement(id.funcId.toLong())
//        return IndirectFunctionRef.FuncRef(ref.asInt())
//    }


}
