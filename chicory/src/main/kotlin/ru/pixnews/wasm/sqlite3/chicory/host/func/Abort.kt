package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME

fun abortFunc(
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    { _: Instance, _: Array<Value> -> error("native code called abort()") },
    moduleName,
    "abort",
    listOf(),
    listOf(),
)

