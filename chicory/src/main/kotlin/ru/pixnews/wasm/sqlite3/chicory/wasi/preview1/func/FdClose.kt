package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Level
import java.util.logging.Logger
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.WasiHostFunction
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.wasiHostFunction
import ru.pixnews.wasm.sqlite3.host.filesystem.SysException

fun fdClose(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = wasiHostFunction(
    funcName = "fd_close",
    paramTypes = listOf(
        Fd.wasmValueType, // Fd
    ),
    moduleName = moduleName,
    handle = FdClose(filesystem)
)

private class FdClose(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(FdClose::class.qualifiedName)
) : WasiHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Errno {
        val fd = Fd(args[0].asInt())
        return try {
            filesystem.close(fd)
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "fd_close() error: $e" }
            e.errNo
        }
    }
}