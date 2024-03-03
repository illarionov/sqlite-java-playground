package org.example.app.sqlite3.callback

import org.example.app.ext.functionTable
import org.example.app.ext.setupWasmModuleFunctions
import org.example.app.ext.withWasmContext
import org.example.app.host.Host
import org.example.app.host.HostFunction
import org.example.app.host.fn
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.wasm.WasmFunctionInstance
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.functiontable.IndirectFunctionTableIndex

const val SQLITE3_CALLBACK_MANAGER_MODULE_NAME = "sqlite3-callback-manager"

class SqliteCallbacksModuleBuilder(
    private val graalContext: Context,
    private val host: Host,
    private val callbackStore: Sqlite3CallbackStore
) {
    private val sqliteCallbackHostFunctions: List<HostFunction> = buildList {
        fn(
            name = SQLITE3_EXEC_CB_FUNCTION_NAME,
            paramTypes = listOf(I32, I32, I32, I32),
            retType = I32,
            nodeFactory = { language: WasmLanguage, instance: WasmInstance, _: Host, functionName: String ->
                Sqlite3CallExecAdapter(
                    language = language,
                    instance = instance,
                    callbackStore = callbackStore,
                    functionName = functionName
                )
            }
        )
    }

    fun setupModule() {
        val module = WasmModule.create(
            SQLITE3_CALLBACK_MANAGER_MODULE_NAME,
            null
        )
        graalContext.withWasmContext { wasmContext ->
            setupWasmModuleFunctions(wasmContext, host, module, sqliteCallbackHostFunctions)
        }
    }

    fun setupIndirectFunctionTable(): Sqlite3CallbackFunctionIndexes {
        val callbackManagerModule: Value = graalContext
            .getBindings("wasm")
            .getMember(SQLITE3_CALLBACK_MANAGER_MODULE_NAME)

        graalContext.withWasmContext { wasmContext ->
            val functionTable = wasmContext.functionTable
            val firstFuncId = functionTable.grow(sqliteCallbackHostFunctions.size, null)
            val funcIdx: Map<String, IndirectFunctionTableIndex> = sqliteCallbackHostFunctions
                .mapIndexed { index, hostFunction ->
                    val funcId = firstFuncId + index
                    val funcName = hostFunction.name
                    functionTable[funcId] = callbackManagerModule
                        .getMember(funcName)
                        .`as`(WasmFunctionInstance::class.java)
                    funcName to IndirectFunctionTableIndex(funcId)
                }.toMap()
            return Sqlite3CallbackFunctionIndexes(funcIdx)
        }
    }
}