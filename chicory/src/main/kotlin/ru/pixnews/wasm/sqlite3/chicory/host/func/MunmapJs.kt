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

fun munmapJs(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    MunmapJs(filesystem),
    moduleName,
    "_munmap_js",
    listOf(
        ValueType.I32,
        ValueType.I32,
        ValueType.I32,
        ValueType.I32,
        ValueType.I32,
        ValueType.I64,
    ),
    ParamTypes.i32,
)

private class MunmapJs(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(MunmapJs::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        TODO("Not yet implemented")
    }
}