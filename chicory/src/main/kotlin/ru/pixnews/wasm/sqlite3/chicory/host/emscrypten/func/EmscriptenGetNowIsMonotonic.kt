package ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.emscriptenEnvHostFunction

fun emscriptenGetNowIsMonotonic(
    isMonotonic: Boolean = true,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "_emscripten_get_now_is_monotonic",
    paramTypes = listOf(),
    returnType = I32,
    moduleName = moduleName,
) { _, _ ->
    Value.i32(if (isMonotonic) 1 else 0)
}