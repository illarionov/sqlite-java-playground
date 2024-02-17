package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.util.logging.Logger
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.chicory
import ru.pixnews.wasm.sqlite3.chicory.ext.valueType
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.pack
import ru.pixnews.wasm.sqlite3.host.filesystem.SysException

fun syscallFstat64(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    Fstat64(filesystem),
    moduleName,
    "__syscall_fstat64",
    listOf(
        ValueType.I32, // fd
        U8.pointer.chicory, // statbuf
    ),
    listOf(
        Errno.valueType
    ),
)

private class Fstat64(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(Fstat64::class.qualifiedName)
) : WasmFunctionHandle {

    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        val result = fstat64(
            instance,
            Fd(params[0].asInt()),
            params[1].asWasmAddr(),
        )
        return arrayOf(Value.i32(result.toLong()))
    }

    private fun fstat64(
        instance: Instance,
        fd: Fd,
        dst: WasmPtr,
    ): Int {
        try {
            val stat = filesystem.stat(fd).also {
                logger.finest { "fStat64($fd): OK $it" }
            }.pack()
            instance.memory().write(dst, stat)
        } catch (e: SysException) {
            logger.finest { "fStat64(${fd}): Error ${e.errNo}" }
            return -e.errNo.code
        }
        return 0
    }
}