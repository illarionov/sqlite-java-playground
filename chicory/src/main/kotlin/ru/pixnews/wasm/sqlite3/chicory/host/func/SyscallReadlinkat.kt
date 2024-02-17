package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem

fun syscallReadlinkat(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    SyscallReadlinkat(filesystem),
    moduleName,
    "__syscall_readlinkat",
    ParamTypes.i32i32,
    ParamTypes.i32,
)

private class SyscallReadlinkat(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(SyscallReadlinkat::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        TODO("Not yet implemented")
    }
}