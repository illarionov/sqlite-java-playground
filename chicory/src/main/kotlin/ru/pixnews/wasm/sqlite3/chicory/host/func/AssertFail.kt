package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import ru.pixnews.wasm.host.WebAssemblyValueType
import ru.pixnews.wasm.host.WebAssemblyValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.readNullTerminatedString
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.emscriptenEnvHostFunction

fun assertFail(
    moduleName: String = ENV_MODULE_NAME,
) : HostFunction = emscriptenEnvHostFunction(
    funcName = "__assert_fail",
    paramTypes = listOf(
        U8.pointer, // pCondition
        U8.pointer, // filename
        I32, // line
        U8.pointer // func
    ),
    returnType = WebAssemblyValueType.F64,
    moduleName = moduleName,
) { instance, params ->
    val memory = instance.memory()
    throw AssertionFailed(
        condition = memory.readNullTerminatedString(params[0]),
        filename = memory.readNullTerminatedString(params[1]),
        line = params[2].asInt(),
        func = memory.readNullTerminatedString(params[3])
    )
}

public class AssertionFailed(
    val condition: String?,
    val filename: String?,
    val line: Int,
    val func: String?,
) : RuntimeException(formatErrMsg(condition, filename, line, func)) {
    private companion object {
        fun formatErrMsg(
            condition: String?,
            filename: String?,
            line: Int,
            func: String?,
        ): String = buildString {
            append("Assertion failed: ")
            append(condition ?: "``")
            append(",  at ")
            listOf(
                filename ?: "unknown filename",
                line.toString(),
                func ?: "unknown function"
            ).joinTo(this, ", ", prefix = "[", postfix = "]")
        }
    }
}
