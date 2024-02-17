package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Level
import java.util.logging.Logger
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I64
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import ru.pixnews.wasm.host.wasi.preview1.type.Whence
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.FdChannel
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.position
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.WasiHostFunction
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.wasiHostFunction
import ru.pixnews.wasm.sqlite3.host.filesystem.SysException

fun fdSeek(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = wasiHostFunction(
    funcName = "fd_seek",
    paramTypes = listOf(
        Fd.wasmValueType, // fd
        I64, // offset
        I32, // whence
        I64.pointer // *newOffset
    ),
    moduleName = moduleName,
    handle = FdSeek(filesystem)
)

private class FdSeek(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(FdSeek::class.qualifiedName)
) : WasiHostFunction {
    override fun apply(instance: Instance, vararg params: Value): Errno {
        val fd = Fd(params[0].asInt())
        val offset = params[1].asLong()
        val whence = Whence.fromIdOrNull(params[2].asInt()) ?: return Errno.INVAL
        val pNewOffset = params[3].asWasmAddr()
        return fdSeek(instance, fd, offset, whence, pNewOffset)
    }

    private fun fdSeek(
        instance: Instance,
        fd: Fd,
        offset: Long,
        whence: Whence,
        pNewOffset: WasmPtr,
    ): Errno {
        return try {
            val channel: FdChannel = filesystem.getStreamByFd(fd)
            filesystem.seek(channel, offset, whence)

            val newPosition = channel.position

            instance.memory().writeLong(pNewOffset, newPosition)

            Errno.SUCCESS
        } catch (sysException: SysException) {
            logger.log(Level.INFO, sysException) { "fdSeek() error" }
            sysException.errNo
        }
    }
}