package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.WasmPtr
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.readNullTerminatedString
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.SysException
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.pack
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U8
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.pointer

fun syscallLstat64(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = stat64Func(
    filesystem = filesystem,
    fieldName = "__syscall_lstat64",
    followSymlinks = false,
    moduleName = moduleName
)

fun syscallStat64(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = stat64Func(
    filesystem = filesystem,
    fieldName = "__syscall_stat64",
    followSymlinks = true,
    moduleName = moduleName
)

private fun stat64Func(
    filesystem: FileSystem,
    fieldName: String,
    followSymlinks: Boolean = true,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    Stat64(filesystem = filesystem, followSymlinks = followSymlinks),
    moduleName,
    fieldName,
    listOf(
        U8.pointer, // pathname
        U8.pointer, // statbuf
    ),
    listOf(Errno.valueType),
)

private class Stat64(
    private val filesystem: FileSystem,
    private val followSymlinks: Boolean = false,
    private val logger: Logger = Logger.getLogger(Stat64::class.qualifiedName)
) : WasmFunctionHandle {
    private val syscallName = if (followSymlinks) "Stat64" else "Lstat64"

    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        val result = stat64(
            instance,
            params[0].asWasmAddr(),
            params[1].asWasmAddr(),
        )
        return arrayOf(Value.i32(result.toLong()))
    }

    private fun stat64(
        instance: Instance,
        pathnamePtr: WasmPtr,
        dst: WasmPtr,
    ): Int {
        var path = ""
        try {
            path = instance.memory().readNullTerminatedString(pathnamePtr)
            val stat = filesystem.stat(
                path = path,
                followSymlinks = followSymlinks
            ).also {
                logger.finest { "$syscallName($path): $it" }
            }.pack()
            instance.memory().write(dst, stat)
        } catch (e: SysException) {
            logger.finest { "$syscallName(`$path`): error ${e.errNo}" }
            return -e.errNo.code
        }

        return 0
    }
}