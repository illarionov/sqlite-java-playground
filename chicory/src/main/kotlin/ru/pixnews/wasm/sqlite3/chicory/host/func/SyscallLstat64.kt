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
): HostFunction = HostFunction(
    Lstat64(filesystem),
    moduleName,
    "__syscall_lstat64",
    listOf(
        U8.pointer, // pathname
        U8.pointer, // statbuf
    ),
    listOf(
        Errno.valueType
    ),
)

private class Lstat64(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(Lstat64::class.qualifiedName)
) : WasmFunctionHandle {

    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        val result = lstat64(
            instance,
            params[0].asWasmAddr(),
            params[1].asWasmAddr(),
        )
        return arrayOf(Value.i32(result.toLong()))
    }

    private fun lstat64(
        instance: Instance,
        pathnamePtr: WasmPtr,
        dst: WasmPtr,
    ): Int {
        try {
            val path = instance.memory().readNullTerminatedString(pathnamePtr)
            val stat = filesystem.stat(path).also {
                logger.finest { "lStast64($path): $it" }
            }.pack()
            instance.memory().write(dst, stat)
        } catch (e: SysException) {
            logger.finest { "lStast64(): error ${e.errNo}" }
            return -e.errNo.code
        }

        return 0
    }
}