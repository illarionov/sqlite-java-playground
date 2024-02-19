package ru.pixnews.wasm.host.wasi.preview1.ext

import java.nio.ByteBuffer
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray

fun interface WasiMemoryReader {
    fun read(memory: Memory, fd: Fd, ioVecs: IovecArray): ULong
}

class DefaultWasiMemoryReader(
    private val filesystem: FileSystem,
    private val strategy: ReadWriteStrategy,
) : WasiMemoryReader {
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