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
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.SysException
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Fd

fun syscallFchown32(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    Fchown32(filesystem),
    moduleName,
    "__syscall_fchown32",
    listOf(
        Fd.valueType, // fd
        ValueType.I32, // owner,
        ValueType.I32 // group,
    ),
    ParamTypes.i32,
)

private class Fchown32(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(Fchown32::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        val fd = Fd(params[0].asInt())
        val owner = params[1].asInt()
        val group = params[2].asInt()
        val code = try {
            filesystem.chown(fd, owner, group)
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.finest { "chown($fd, $owner, $group): Error ${e.errNo}" }
            e.errNo
        }
        return arrayOf(Value.i32(-code.code.toLong()))
    }
}