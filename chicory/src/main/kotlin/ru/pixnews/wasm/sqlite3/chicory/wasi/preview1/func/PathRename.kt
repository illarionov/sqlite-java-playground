package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.ext.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem

fun pathRename(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = HostFunction(
    PathRename(filesystem),
    moduleName,
    "path_rename",
    List(6) { ValueType.I32 },
    ParamTypes.i32
)

private class PathRename(
    fileSystem: FileSystem,
    private val logger: Logger = Logger.getLogger(PathRename::class.qualifiedName),
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        TODO("Not yet implemented")
    }
}