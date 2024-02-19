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
import ru.pixnews.wasm.host.wasi.preview1.ext.DefaultWasiMemoryWriter
import ru.pixnews.wasm.host.wasi.preview1.ext.FdWriteExt.readCiovecs
import ru.pixnews.wasm.host.wasi.preview1.ext.WasiMemoryWriter
import ru.pixnews.wasm.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WasiHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.wasiHostFunction

fun fdWrite(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdWrite(memory, filesystem, moduleName, "fd_write", CHANGE_POSITION)

fun fdPwrite(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdWrite(memory, filesystem, moduleName, "fd_pwrite", DO_NOT_CHANGE_POSITION)

private fun fdWrite(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String,
    fieldName: String,
    strategy: ReadWriteStrategy
): HostFunction = wasiHostFunction(
    funcName = fieldName,
    paramTypes = listOf(
        Fd.wasmValueType, // Fd
        IovecArray.pointer, // ciov
        I32, // ciov_cnt
        I32.pointer, // pNum
    ),
    moduleName = moduleName,
    handle = FdWrite(memory, filesystem, strategy)
)

private class FdWrite(
    private val memory: Memory,
    filesystem: FileSystem,
    strategy: ReadWriteStrategy,
    private val logger: Logger = Logger.getLogger(FdWrite::class.qualifiedName)
) : WasiHostFunction {
    private val memoryWriter: WasiMemoryWriter = UnsafeWasiMemoryWriter.create(filesystem, strategy)
        ?: DefaultWasiMemoryWriter(filesystem, strategy)

    override fun apply(instance: Instance, vararg args: Value): Errno {
        val fd = Fd(args[0].asInt())
        val pCiov = args[1].asWasmAddr()
        val cIovCnt = args[2].asInt()
        val pNum = args[3].asWasmAddr()

        val cioVecs = readCiovecs(memory, pCiov, cIovCnt)
        return try {
            val writtenBytes = memoryWriter.write(memory, fd, cioVecs)
            memory.writeI32(pNum, writtenBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "write() error" }
            e.errNo
        }
    }

    private class UnsafeWasiMemoryWriter private constructor(
        private val filesystem: FileSystem,
        private val strategy: ReadWriteStrategy,
        private val bufferField: Field
    ) : WasiMemoryWriter {

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
                ioVec.buf,
                ioVec.bufLen.value.toInt()
            )
        }

        companion object {
            fun create(
                filesystem: FileSystem,
                strategy: ReadWriteStrategy,
            ): UnsafeWasiMemoryWriter? {
                try {
                    val bufferField = Memory::class.java.getDeclaredField("buffer")
                    if (!bufferField.trySetAccessible()) {
                        return null
                    }
                    return UnsafeWasiMemoryWriter(filesystem, strategy, bufferField)
                } catch (nsfe: NoSuchFileException) {
                    return null
                } catch (se: SecurityException) {
                    return null
                }
            }
        }
    }
}
