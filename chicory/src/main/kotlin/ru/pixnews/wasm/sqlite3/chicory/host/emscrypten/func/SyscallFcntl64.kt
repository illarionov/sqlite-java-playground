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

fun syscallFcntl64(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_fcntl64",
    paramTypes = listOf(
        I32,
        I32, // owner,
        I32, // group,
    ),
    returnType = I32,
    moduleName = moduleName,
    handle = SyscallFcntl64(filesystem)
)

private class SyscallFcntl64(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(SyscallFcntl64::class.qualifiedName)
) : EmscryptenHostFunction {
    override fun apply(instance: Instance, vararg params: Value): Value {
        TODO("Not yet implemented")
    }
}