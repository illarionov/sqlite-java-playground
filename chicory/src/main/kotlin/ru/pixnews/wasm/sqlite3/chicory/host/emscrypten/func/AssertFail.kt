package ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func

import com.dylibso.chicory.runtime.HostFunction
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.emscrypten.AssertionFailed
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.memory.readNullableZeroTerminatedString
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
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
) { _, params ->
    throw AssertionFailed(
        condition = memory.readNullableZeroTerminatedString(params[0].asWasmAddr()),
        filename = memory.readNullableZeroTerminatedString(params[1].asWasmAddr()),
        line = params[2].asInt(),
        func = memory.readNullableZeroTerminatedString(params[3].asWasmAddr())
    )
}
