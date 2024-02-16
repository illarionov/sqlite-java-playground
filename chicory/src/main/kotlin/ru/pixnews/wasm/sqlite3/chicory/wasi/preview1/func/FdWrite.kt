package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.lang.reflect.Field
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
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.CioVec
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Fd
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
    filesystem: FileSystem,
    strategy: ReadWriteStrategy,
    private val logger: Logger = Logger.getLogger(FdWrite::class.qualifiedName)
) : WasmFunctionHandle {
    private val memoryWriter: MemoryWriter = UnsafeMemoryWriter.create(filesystem, strategy)
        ?: DefaultMemoryWriter(filesystem, strategy)

    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val fd = Fd(args[0].asInt())
        val pCiov = args[1].asWasmAddr()
        val cIovCnt = args[2].asInt()
        val pNum = args[3].asWasmAddr()

        val memory = instance.memory()
        val cioVecs = readCiovecs(memory, pCiov, cIovCnt)
        val errNo = try {
            val writtenBytes = memoryWriter.write(memory, fd, cioVecs)
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
    ): CiovecArray {
        val iovecs = MutableList(ciovCnt) { idx ->
            val pCiovec = pCiov + 8 * idx
            CioVec(
                buf = memory.readI32(pCiovec),
                bufLen = Size(memory.readI32(pCiovec + 4))
            )
        }
        return CiovecArray(iovecs)
    }

    private fun interface MemoryWriter {
        fun write(memory: Memory, fd: Fd, cioVecs: CiovecArray): ULong
    }

    private class UnsafeMemoryWriter private constructor(
        private val filesystem: FileSystem,
        private val strategy: ReadWriteStrategy,
        private val bufferField: Field
    ) : MemoryWriter {

        override fun write(
            memory: Memory,
            fd: Fd,
            cioVecs: CiovecArray
        ): ULong {
            val memoryByteBuffer = bufferField.get(memory) as? ByteBuffer
                ?: error("Can not get memory byte buffer")

            val bbufs = cioVecs.toByteBuffers(memoryByteBuffer)
            return filesystem.write(fd, bbufs, strategy)
        }

        private fun CiovecArray.toByteBuffers(
            memoryBuffer: ByteBuffer
        ): Array<ByteBuffer> = Array(ciovecList.size) {
            val ioVec = ciovecList[it]
            memoryBuffer.slice(
                ioVec.buf.asWasmAddr(),
                ioVec.bufLen.value.asInt()
            )
        }

        companion object {
            fun create(
                filesystem: FileSystem,
                strategy: ReadWriteStrategy,
            ): UnsafeMemoryWriter? {
                try {
                    val bufferField = Memory::class.java.getDeclaredField("buffer")
                    if (!bufferField.trySetAccessible()) {
                        return null
                    }
                    return UnsafeMemoryWriter(filesystem, strategy, bufferField)
                } catch (nsfe: NoSuchFileException) {
                    return null
                } catch (se: SecurityException) {
                    return null
                }
            }
        }
    }

    private class DefaultMemoryWriter(
        private val filesystem: FileSystem,
        private val strategy: ReadWriteStrategy,
    ) : MemoryWriter {
        override fun write(memory: Memory, fd: Fd, cioVecs: CiovecArray): ULong {
            val bufs = cioVecs.toByteBuffers(memory)
            return filesystem.write(fd, bufs, strategy)
        }

        private fun CiovecArray.toByteBuffers(
            memory: Memory
        ): Array<ByteBuffer> = Array(ciovecList.size) { idx ->
            val ciovec = ciovecList[idx]
            val bytes = memory.readBytes(
                ciovec.buf.asWasmAddr(),
                ciovec.bufLen.value.asInt()
            )
            ByteBuffer.wrap(bytes)
        }
    }

}
