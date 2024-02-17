package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.host.WebAssemblyValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.WebAssemblyValueType.WebAssemblyTypes.I64
import ru.pixnews.wasm.sqlite3.chicory.ext.EmscryptenHostFunction
import ru.pixnews.wasm.sqlite3.chicory.ext.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem

fun munmapJs(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "_munmap_js",
    paramTypes = listOf(
        I32,
        I32,
        I32,
        I32,
        I32,
        I64,
    ),
    returnType = I32,
    moduleName = moduleName,
    handle = MunmapJs(filesystem)
)

private class MunmapJs(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(MunmapJs::class.qualifiedName)
) : EmscryptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value? {
        TODO("Not yet implemented")
    }
}