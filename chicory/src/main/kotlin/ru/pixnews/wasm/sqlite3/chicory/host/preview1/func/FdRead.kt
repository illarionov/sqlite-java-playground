package ru.pixnews.wasm.sqlite3.chicory.host.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.lang.reflect.Field
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.wasi.preview1.ext.DefaultWasiMemoryReader
import ru.pixnews.wasm.host.wasi.preview1.ext.FdReadExt.readIovecs
import ru.pixnews.wasm.host.wasi.preview1.ext.WasiMemoryReader
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WasiHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.wasiHostFunction

fun fdRead(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdRead(memory, filesystem, moduleName, "fd_read", CHANGE_POSITION)

fun fdPread(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdRead(memory, filesystem, moduleName, "fd_pread", DO_NOT_CHANGE_POSITION)

private fun fdRead(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String,
    fieldName: String,
    strategy: ReadWriteStrategy
): HostFunction = wasiHostFunction(
    funcName = fieldName,
    paramTypes = listOf(
        Fd.wasmValueType, // Fd
        IovecArray.pointer, // iov
        I32, // iov_cnt
        I32.pointer, // pNum
    ),
    moduleName = moduleName,
    handle = FdRead(memory, filesystem, strategy)
)

private class FdRead(
    private val memory: Memory,
    filesystem: FileSystem,
    strategy: ReadWriteStrategy,
    private val logger: Logger = Logger.getLogger(FdRead::class.qualifiedName)
) : WasiHostFunction {
    private val memoryReader: WasiMemoryReader = UnsafeWasiMemoryReader.create(filesystem, strategy)
        ?: DefaultWasiMemoryReader(filesystem, strategy)

    override fun apply(instance: Instance, vararg args: Value): Errno {
        val fd = Fd(args[0].asInt())
        val pIov = args[1].asWasmAddr()
        val iovCnt = args[2].asInt()
        val pNum = args[3].asWasmAddr()

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

    private class UnsafeWasiMemoryReader private constructor(
        private val filesystem: FileSystem,
        private val strategy: ReadWriteStrategy,
        private val bufferField: Field
    ) : WasiMemoryReader {
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
            ): UnsafeWasiMemoryReader? {
                try {
                    val bufferField = Memory::class.java.getDeclaredField("buffer")
                    if (!bufferField.trySetAccessible()) {
                        return null
                    }
                    return UnsafeWasiMemoryReader(filesystem, strategy, bufferField)
                } catch (nsfe: NoSuchFileException) {
                    return null
                } catch (se: SecurityException) {
                    return null
                }
            }
        }
    }
}