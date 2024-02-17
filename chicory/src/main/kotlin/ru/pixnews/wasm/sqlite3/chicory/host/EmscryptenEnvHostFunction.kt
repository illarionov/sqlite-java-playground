package ru.pixnews.wasm.sqlite3.chicory.host

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.host.WasmValueType
import ru.pixnews.wasm.sqlite3.chicory.ext.chicory

internal fun emscriptenEnvHostFunction(
    funcName: String,
    paramTypes : List<WasmValueType>,
    returnType: WasmValueType?,
    moduleName: String = ENV_MODULE_NAME,
    handle: EmscryptenHostFunction,
) : HostFunction = HostFunction(
    HostFunctionAdapter(handle),
    moduleName,
    funcName,
    paramTypes.map(WasmValueType::chicory),
    returnType?.let { listOf(it.chicory) } ?: listOf()
)

internal fun interface EmscryptenHostFunction {
    fun apply(instance: Instance, vararg args: Value): Value?
}

private class HostFunctionAdapter(
    private val delegate: EmscryptenHostFunction
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val result: Value? = delegate.apply(instance, args = args)
        return if (result != null) {
            arrayOf(result)
        } else {
            arrayOf()
        }
    }
}