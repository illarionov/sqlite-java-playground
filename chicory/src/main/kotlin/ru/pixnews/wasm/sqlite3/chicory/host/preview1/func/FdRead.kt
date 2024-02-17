package ru.pixnews.wasm.sqlite3.chicory.host.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.wasm.types.Value
import java.lang.reflect.Field
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.Iovec
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.Size
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.ReadWriteStrategy
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WasiHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.wasiHostFunction
import ru.pixnews.wasm.sqlite3.host.filesystem.SysException

fun fdRead(
    filesystem: FileSystem,
    moduleName: String = ru.pixnews.wasm.sqlite3.chicory.host.preview1.WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdRead(filesystem, moduleName, "fd_read", CHANGE_POSITION)

fun fdPread(
    filesystem: FileSystem,
    moduleName: String = ru.pixnews.wasm.sqlite3.chicory.host.preview1.WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdRead(filesystem, moduleName, "fd_pread", DO_NOT_CHANGE_POSITION)

private fun fdRead(
    filesystem: FileSystem,
    moduleName: String,
    fieldName: String,
    strategy: ReadWriteStrategy
): HostFunction = ru.pixnews.wasm.sqlite3.chicory.host.preview1.wasiHostFunction(
    funcName = fieldName,
    paramTypes = listOf(
        Fd.wasmValueType, // Fd
        IovecArray.pointer, // iov
        I32, // iov_cnt
        I32.pointer, // pNum
    ),
    moduleName = moduleName,
    handle = FdRead(filesystem, strategy)
)

private class FdRead(
    filesystem: FileSystem,
    strategy: ReadWriteStrategy,
    private val logger: Logger = Logger.getLogger(FdRead::class.qualifiedName)
) : ru.pixnews.wasm.sqlite3.chicory.host.preview1.WasiHostFunction {
    private val memoryReader: MemoryReader = UnsafeMemoryReader.create(filesystem, strategy)
        ?: DefaultMemoryReader(filesystem, strategy)

    override fun apply(instance: Instance, vararg args: Value): Errno {
        val fd = Fd(args[0].asInt())
        val pIov = args[1].asWasmAddr()
        val iovCnt = args[2].asInt()
        val pNum = args[3].asWasmAddr()

        val memory = instance.memory()
        val ioVecs = readIovecs(memory, pIov, iovCnt)
        return try {
            val readBytes = memoryReader.read(memory, fd, ioVecs)
            memory.writeI32(pNum, readBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "read() error" }
            e.errNo
        }
    }

    private fun readIovecs(
        memory: Memory,
        pIov: WasmPtr,
        iovCnt: Int
    ): IovecArray {
        val iovecs = MutableList(iovCnt) { idx ->
            val pIovec = pIov + 8 * idx
            Iovec(
                buf = memory.readI32(pIovec).asWasmAddr(),
                bufLen = Size(memory.readI32(pIovec + 4).asInt().toUInt())
            )
        }
        return IovecArray(iovecs)
    }

    private fun interface MemoryReader {
        fun read(memory: Memory, fd: Fd, ioVecs: IovecArray): ULong
    }

    private class UnsafeMemoryReader private constructor(
        private val filesystem: FileSystem,
        private val strategy: ReadWriteStrategy,
        private val bufferField: Field
    ) : MemoryReader {

        override fun read(
            memory: Memory,
            fd: Fd,
            ioVecs: IovecArray
        ): ULong {
            val memoryByteBuffer = bufferField.get(memory) as? ByteBuffer
                ?: error("Can not get memory byte buffer")

            val bbufs = ioVecs.toByteBuffers(memoryByteBuffer)
            return filesystem.read(fd, bbufs, strategy)
        }

        private fun IovecArray.toByteBuffers(
            memoryBuffer: ByteBuffer
        ): Array<ByteBuffer> = Array(iovecList.size) {
            val ioVec = iovecList[it]
            memoryBuffer.slice(
                ioVec.buf,
                ioVec.bufLen.value.toInt()
            )
        }

        companion object {
            fun create(
                filesystem: FileSystem,
                strategy: ReadWriteStrategy,
            ): UnsafeMemoryReader? {
                try {
                    val bufferField = Memory::class.java.getDeclaredField("buffer")
                    if (!bufferField.trySetAccessible()) {
                        return null
                    }
                    return UnsafeMemoryReader(filesystem, strategy, bufferField)
                } catch (nsfe: NoSuchFileException) {
                    return null
                } catch (se: SecurityException) {
                    return null
                }
            }
        }
    }

    private class DefaultMemoryReader(
        private val filesystem: FileSystem,
        private val strategy: ReadWriteStrategy,
    ) : MemoryReader {
        override fun read(memory: Memory, fd: Fd, ioVecs: IovecArray): ULong {
            val bbufs = ioVecs.toByteBuffers()
            val readBytes = filesystem.read(fd, bbufs, strategy)
            ioVecs.iovecList.forEachIndexed { idx, vec ->
                val bbuf: ByteBuffer = bbufs[idx]
                bbuf.flip()
                if (bbuf.limit() != 0) {
                    require(bbuf.hasArray())
                    memory.write(
                        vec.buf,
                        bbuf.array(),
                        0,
                        bbuf.limit()
                    )
                }
            }
            return readBytes
        }

        private fun IovecArray.toByteBuffers(): Array<ByteBuffer> = Array(iovecList.size) {
            ByteBuffer.allocate(iovecList[it].bufLen.value.toInt())
        }
    }
}