package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.host.WebAssemblyValueType
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.EmscryptenHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem

fun syscallIoctl(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_ioctl",
    paramTypes = listOf(
        WebAssemblyValueType.I32,
        WebAssemblyValueType.I32,
        WebAssemblyValueType.I32,
    ),
    returnType = Errno.webAssemblyValueType,
    moduleName = moduleName,
    handle = SyscallIoctl(filesystem)
)

private class SyscallIoctl(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(SyscallIoctl::class.qualifiedName)
) : EmscryptenHostFunction {
    override fun apply(instance: Instance, vararg params: Value): Value {
        TODO("Not yet implemented")
    }
}