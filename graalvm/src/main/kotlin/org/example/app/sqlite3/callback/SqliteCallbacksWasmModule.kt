package org.example.app.sqlite3.callback

import org.example.app.ext.toTypesByteArray
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32

const val SQLITE3_CALLBACK_MANAGER_MODULE_NAME = "sqlite-callback-manager"

fun setupSqliteCallbacksWasmModule(
    wasmContext: WasmContext,
    callbackManager: Sqlite3CallbackStore
) {
    val module = WasmModule.create(
        SQLITE3_CALLBACK_MANAGER_MODULE_NAME,
        null
    )
    val sqlite3ExecCbfuncTypeIndex = module.allocateFunctionType(
        listOf(I32, I32, I32, I32).toTypesByteArray(),
        listOf(I32).toTypesByteArray(),
        false
    )
    val sqlite3ExecCbfunc = module.declareExportedFunction(sqlite3ExecCbfuncTypeIndex, SQLITE3_EXEC_CB_FUNCTION_NAME)

    val cbModuleInstance: WasmInstance = wasmContext.readInstance(module)
    val adapter = Sqlite3CallExecAdapter(wasmContext.language(), cbModuleInstance, callbackManager)
    cbModuleInstance.setTarget(sqlite3ExecCbfunc.index(), adapter.callTarget)
}
