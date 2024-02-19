package ru.pixnews.wasm.host.wasi.preview1.ext

import java.nio.ByteBuffer
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.Fd

fun interface WasiMemoryWriter {
    fun write(memory: Memory, fd: Fd, cioVecs: CiovecArray): ULong
}

class DefaultWasiMemoryWriter(
    private val filesystem: FileSystem,
    private val strategy: ReadWriteStrategy,
) : WasiMemoryWriter {
    override fun write(memory: Memory, fd: Fd, cioVecs: CiovecArray): ULong {
        val bufs = cioVecs.toByteBuffers(memory)
        return filesystem.write(fd, bufs, strategy)
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
