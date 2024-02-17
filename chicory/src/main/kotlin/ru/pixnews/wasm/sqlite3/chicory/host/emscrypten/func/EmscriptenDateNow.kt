package ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.wasm.types.Value
import java.time.Clock
import ru.pixnews.wasm.host.WasmValueType
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.emscriptenEnvHostFunction

fun emscriptenDateNow(
    clock: Clock = Clock.systemDefaultZone(),
    moduleName: String = ENV_MODULE_NAME,
): HostFunction =  emscriptenEnvHostFunction(
    funcName = "emscripten_date_now",
    paramTypes = listOf(),
    returnType = WasmValueType.F64,
    moduleName = moduleName
) { _, _ ->
    val millis = clock.millis()
    Value.fromDouble(millis.toDouble())
}
