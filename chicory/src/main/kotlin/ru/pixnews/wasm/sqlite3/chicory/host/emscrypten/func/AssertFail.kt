package ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func

import com.dylibso.chicory.runtime.HostFunction
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.emscrypten.AssertionFailed
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.readNullTerminatedString
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.emscriptenEnvHostFunction

fun assertFail(
    memory: Memory,
    moduleName: String = ENV_MODULE_NAME,
) : HostFunction = emscriptenEnvHostFunction(
    funcName = "__assert_fail",
    paramTypes = listOf(
        U8.pointer, // pCondition
        U8.pointer, // filename
        I32, // line
        U8.pointer // func
    ),
    returnType = null,
    moduleName = moduleName,
) { instance, params ->
    throw AssertionFailed(
        condition = memory.readNullTerminatedString(params[0]),
        filename = memory.readNullTerminatedString(params[1]),
        line = params[2].asInt(),
        func = memory.readNullTerminatedString(params[3])
    )
}
