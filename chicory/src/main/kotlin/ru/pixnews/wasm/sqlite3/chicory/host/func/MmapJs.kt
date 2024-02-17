package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem

fun mmapJs(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    MmapJs(filesystem),
    moduleName,
    "_mmap_js",
    listOf(
        ValueType.I32,
        ValueType.I32,
        ValueType.I32,
        ValueType.I32,
        ValueType.I64,
        ValueType.I32,
        ValueType.I32,
    ),
    ParamTypes.i32,
)

private class MmapJs(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(MmapJs::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        TODO("Not yet implemented")
    }
}