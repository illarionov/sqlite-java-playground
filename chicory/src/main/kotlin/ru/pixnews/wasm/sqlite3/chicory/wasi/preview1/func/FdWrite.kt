package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.ext.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.ext.WasmPtr
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.SysException
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.ReadWriteStrategy
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Iovec
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Size
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.pointer

fun fdWrite(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdWrite(filesystem, moduleName, "fd_write", CHANGE_POSITION)

fun fdPwrite(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdWrite(filesystem, moduleName, "fd_pwrite", DO_NOT_CHANGE_POSITION)

private fun fdWrite(
    filesystem: FileSystem,
    moduleName: String,
    fieldName: String,
    strategy: ReadWriteStrategy
): HostFunction = HostFunction(
    FdWrite(filesystem, strategy),
    moduleName,
    fieldName,
    listOf(
        Fd.valueType, // Fd
        IovecArray.pointer, // ciov
        ValueType.I32, // ciov_cnt
        ValueType.I32.pointer, // pNum
    ),
    ParamTypes.i32,
)

private class FdWrite(
    private val filesystem: FileSystem,
    private val strategy: ReadWriteStrategy,
    private val logger: Logger = Logger.getLogger(FdWrite::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val fd = Fd(args[0].asInt())
        val pCiov = args[1].asWasmAddr()
        val cIovCnt = args[2].asInt()
        val pNum = args[3].asWasmAddr()

        val memory = instance.memory()
        val cioVecs = readCiovecs(memory, pCiov, cIovCnt)
        val bufs = cioVecs.toByteBuffers(memory)

        val errNo = try {
            val writtenBytes = filesystem.write(fd, bufs, strategy)
            memory.writeI32(pNum, writtenBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "fd_write() error" }
            e.errNo
        }

        return arrayOf(Value.i32(errNo.code.toLong()))
    }

    private fun readCiovecs(
        memory: Memory,
        pCiov: WasmPtr,
        ciovCnt: Int
    ): IovecArray {
        val iovecs = MutableList(ciovCnt) { idx ->
            val pCiovec = pCiov + 8 * idx
            Iovec(
                buf = memory.readI32(pCiovec),
                bufLen = Size(memory.readI32(pCiovec + 4))
            )
        }
        return IovecArray(iovecs)
    }

    private fun IovecArray.toByteBuffers(
        memory: Memory
    ): Array<ByteBuffer> = Array(iovecList.size) { idx ->
        val ciovec = iovecList[idx]
        val bytes = memory.readBytes(
            ciovec.buf.asWasmAddr(),
            ciovec.bufLen.value.asInt()
        )
        ByteBuffer.wrap(bytes)
    }
}
