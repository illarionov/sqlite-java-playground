package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.ext.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.SysException
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Fd

fun fdClose(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = HostFunction(
    FdClose(filesystem),
    moduleName,
    "fd_close",
    listOf(
        Fd.valueType, // Fd
    ),
    ParamTypes.i32,
)

private class FdClose(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(FdClose::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val fd = Fd(args[0].asInt())

        val errNo = try {
            filesystem.close(fd)
            Errno.SUCCESS
        } catch (e: SysException) {
            e.errNo
        }

        return arrayOf(Value.i32(errNo.code.toLong()))
    }
}