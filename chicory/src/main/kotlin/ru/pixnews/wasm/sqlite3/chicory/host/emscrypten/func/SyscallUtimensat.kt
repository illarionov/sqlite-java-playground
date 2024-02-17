package ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.EmscryptenHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.emscriptenEnvHostFunction
import ru.pixnews.wasm.host.filesystem.FileSystem

fun syscallUtimensat(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_utimensat",
    paramTypes = listOf(
        I32,
        I32,
    ),
    returnType = I32,
    moduleName = moduleName,
    handle = SyscallUtimensat(filesystem)
)

private class SyscallUtimensat(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(SyscallUtimensat::class.qualifiedName)
) : EmscryptenHostFunction {
    override fun apply(instance: Instance, vararg params: Value): Value {
        TODO("Not yet implemented")
    }
}