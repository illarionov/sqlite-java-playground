package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME

fun emscriptenGetNowIsMonotonic(
    isMonotonic: Boolean = true,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    EmscriptenGetNowIsMonotonic(isMonotonic),
    moduleName,
    "_emscripten_get_now_is_monotonic",
    listOf(),
    ParamTypes.i32,
)

private class EmscriptenGetNowIsMonotonic(
    isMonotonic: Boolean = true,
) : WasmFunctionHandle {
    private val isMonotonic = arrayOf(Value.i32(if (isMonotonic) 1 else 0))
    override fun apply(instance: Instance?, vararg args: Value?): Array<Value> = isMonotonic
}