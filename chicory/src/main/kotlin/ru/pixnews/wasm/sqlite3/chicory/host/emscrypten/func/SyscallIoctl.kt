package ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.host.WasmValueType
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.EmscryptenHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.emscriptenEnvHostFunction
import ru.pixnews.wasm.host.filesystem.FileSystem

fun syscallIoctl(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_ioctl",
    paramTypes = listOf(
        WasmValueType.I32,
        WasmValueType.I32,
        WasmValueType.I32,
    ),
    returnType = Errno.wasmValueType,
    moduleName = moduleName,
    handle = SyscallIoctl(filesystem)
)

private class SyscallIoctl(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(SyscallIoctl::class.qualifiedName)
) : EmscryptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        TODO("Not yet implemented")
    }
}