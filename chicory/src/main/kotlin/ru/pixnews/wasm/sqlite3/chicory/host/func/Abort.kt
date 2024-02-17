package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.emscriptenEnvHostFunction

fun abortFunc(
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "abort",
    paramTypes = listOf(),
    returnType = null,
    moduleName = moduleName
) { _, _ -> error("native code called abort()") }