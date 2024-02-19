package ru.pixnews.wasm.host.memory

import java.nio.ByteBuffer
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray

fun interface WasiMemoryWriter {
    fun write(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        cioVecs: CiovecArray): ULong
}

class DefaultWasiMemoryWriter(
    private val memory: Memory,
) : WasiMemoryWriter {
    override fun write(channel: FdChannel, strategy: ReadWriteStrategy, cioVecs: CiovecArray): ULong {
        val bufs = cioVecs.toByteBuffers(memory)
        return channel.fileSystem.write(channel, bufs, strategy)
    }

    private fun CiovecArray.toByteBuffers(
        memory: Memory
    ): Array<ByteBuffer> = Array(ciovecList.size) { idx ->
        val ciovec = ciovecList[idx]
        val bytes = memory.readBytes(
            ciovec.buf,
            ciovec.bufLen.value.toInt()
        )
        ByteBuffer.wrap(bytes)
    }


}
