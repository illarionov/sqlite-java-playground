package ru.pixnews.wasm.host.memory

import java.nio.ByteBuffer
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray

fun interface WasiMemoryReader {
    fun read(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray
    ): ULong
}

class DefaultWasiMemoryReader(
    private val memory: Memory,
) : WasiMemoryReader {
    override fun read(channel: FdChannel, strategy: ReadWriteStrategy, iovecs: IovecArray): ULong {
        val bbufs = iovecs.toByteBuffers()
        val readBytes = channel.fileSystem.read(channel, bbufs, strategy)
        iovecs.iovecList.forEachIndexed { idx, vec ->
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