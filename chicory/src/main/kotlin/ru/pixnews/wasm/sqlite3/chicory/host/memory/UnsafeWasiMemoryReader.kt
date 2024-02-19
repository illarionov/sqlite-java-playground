package ru.pixnews.wasm.sqlite3.chicory.host.memory

import com.dylibso.chicory.runtime.Memory
import java.lang.reflect.Field
import java.nio.ByteBuffer
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.host.memory.WasiMemoryReader
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray

internal class UnsafeWasiMemoryReader(
    private val memory: Memory,
    private val bufferField: Field,
) : WasiMemoryReader {
    override fun read(channel: FdChannel, strategy: ReadWriteStrategy, iovecs: IovecArray): ULong {
        val memoryByteBuffer = bufferField.get(memory) as? ByteBuffer
            ?: error("Can not get memory byte buffer")

        val bbufs = iovecs.toByteBuffers(memoryByteBuffer)
        return channel.fileSystem.read(channel, bbufs, strategy)
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
            memory: Memory,
        ): UnsafeWasiMemoryReader? {
            try {
                val bufferField = Memory::class.java.getDeclaredField("buffer")
                if (!bufferField.trySetAccessible()) {
                    return null
                }
                if (bufferField.get(memory) !is ByteBuffer) {
                    return null
                }
                return UnsafeWasiMemoryReader(memory, bufferField)
            } catch (nsfe: NoSuchFieldException) {
                return null
            } catch (se: SecurityException) {
                return null
            }
        }
    }
}