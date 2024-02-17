package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME

fun tzsetJs(
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    TzsetJs(),
    moduleName,
    "_tzset_js",
    ParamTypes.i32i32i32,
    listOf(),
)

private class TzsetJs(
    private val logger: Logger = Logger.getLogger(TzsetJs::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        TODO("Not yet implemented")
    }
}