package org.example.app.sqlite3

import com.oracle.truffle.api.frame.VirtualFrame
import java.util.concurrent.atomic.AtomicLong
import org.example.app.bindings.SqliteBindings
import org.example.app.ext.asWasmPtr
import org.example.app.ext.functionTable
import org.example.app.ext.toTypesByteArray
import org.example.app.ext.withWasmContext
import org.example.app.host.BaseWasmNode
import org.example.app.host.memory.GraalHostMemoryImpl
import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.functiontable.IndirectFunctionRef
import ru.pixnews.wasm.host.functiontable.IndirectFunctionTableId
import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback

class Sqlite3CallbackManager(
    private val context: Context,
    private val memory: GraalHostMemoryImpl,
    private val sqliteBindings: SqliteBindings
) {
    private val moduleNameNo: AtomicLong = AtomicLong(0)

    //val indirectFunctionTable = sqliteBindings.mainBindings.getMember("__indirect_function_table")

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

    private class Sqlite3CallExecAdapter(
        language: WasmLanguage,
        instance: WasmInstance,
        private val delegate: Sqlite3ExecCallback,
        functionName: String = "e",
    ) : BaseWasmNode(language, instance, functionName) {
        override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
            val args = frame.arguments
            return delegate(
                args.asWasmPtr(0),
                args[1] as Int,
                args.asWasmPtr(2),
                args.asWasmPtr(3),
            )
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
