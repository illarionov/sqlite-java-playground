package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.ext.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem

fun fdFdstatSetFlags(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = HostFunction(
    FdFdstatSetFlags(filesystem),
    moduleName,
    "fd_fdstat_set_flags",
    ParamTypes.i32i32,
    ParamTypes.i32,
)

private class FdFdstatSetFlags(
    fileSystem: FileSystem,
    private val logger: Logger = Logger.getLogger(FdFdstatSetFlags::class.qualifiedName),
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        TODO("Not yet implemented")
    }
}