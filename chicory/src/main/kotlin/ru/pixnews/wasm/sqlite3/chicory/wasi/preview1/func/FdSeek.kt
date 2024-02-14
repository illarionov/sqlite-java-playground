package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.ext.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.ext.WasmPtr
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.SysException
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.FdChannel
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.position
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Whence
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.pointer

fun fdSeek(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = HostFunction(
    FdSeek(filesystem),
    moduleName,
    "fd_seek",
    listOf(
        Fd.valueType, // fd
        ValueType.I64, // offset
        ValueType.I32, // whence
        ValueType.I64.pointer // *newOffset
    ),
    ParamTypes.i32,
)

private class FdSeek(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(FdSeek::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        val fd = Fd(params[0].asInt())
        val offset = params[1].asLong()
        val whence = Whence.fromIdOrNull(params[2].asInt()) ?: return arrayOf(Value.i32(Errno.INVAL.code.toLong()))
        val pNewOffset = params[3].asWasmAddr()
        val result = fdSeek(instance, fd, offset, whence, pNewOffset)
        return arrayOf(Value.i32(result.code.toLong()))
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
            sysException.errNo
        }
    }
}