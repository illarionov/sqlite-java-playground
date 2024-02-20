package ru.pixnews.wasm.sqlite3.chicory.host.memory

import com.dylibso.chicory.runtime.Memory
import java.lang.reflect.Field
import java.nio.ByteBuffer
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.host.memory.WasiMemoryWriter
import ru.pixnews.wasm.host.wasi.preview1.type.CiovecArray

internal class UnsafeWasiMemoryWriter private constructor(
    private val memory: Memory,
    private val bufferField: Field
) : WasiMemoryWriter {
    override fun write(channel: FdChannel, strategy: ReadWriteStrategy, cioVecs: CiovecArray): ULong {
        val memoryByteBuffer = bufferField.get(memory) as? ByteBuffer
            ?: error("Can not get memory byte buffer")

        val bbufs = cioVecs.toByteBuffers(memoryByteBuffer)
        return channel.fileSystem.write(channel, bbufs, strategy)
    }

    private fun CiovecArray.toByteBuffers(
        memoryBuffer: ByteBuffer
    ): Array<ByteBuffer> = Array(ciovecList.size) {
        val ioVec = ciovecList[it]
        memoryBuffer.slice(
            ioVec.buf.addr,
            ioVec.bufLen.value.toInt()
        )
    }

    companion object {
        fun create(
            memory: Memory,
        ): UnsafeWasiMemoryWriter? {
            try {
                val bufferField = Memory::class.java.getDeclaredField("buffer")
                if (!bufferField.trySetAccessible()) {
                    return null
                }
                if (bufferField.get(memory) !is ByteBuffer) {
                    return null
                }
                return UnsafeWasiMemoryWriter(memory, bufferField)
            } catch (nsfe: NoSuchFileException) {
                return null
            } catch (se: SecurityException) {
                return null
            }
        }
    }
}