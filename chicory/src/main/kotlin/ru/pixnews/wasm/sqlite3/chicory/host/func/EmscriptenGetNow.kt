package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.host.WebAssemblyValueType.WebAssemblyTypes.F64
import ru.pixnews.wasm.sqlite3.chicory.ext.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME

fun emscriptenGetNow(
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "emscripten_get_now",
    paramTypes = listOf(),
    returnType = F64,
    moduleName = moduleName,
) { _, _ ->
    val ts = System.nanoTime() / 1_000_000.0
    Value.fromDouble(ts)
}
