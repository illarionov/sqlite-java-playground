package org.example.app.host.memory

import java.io.IOException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.Channels
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonReadableChannelException
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.host.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.host.memory.WasiMemoryReader
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray

class GraalIInputStreamWasiMemoryReader(
    private val memory: WasmHostMemoryImpl
) : WasiMemoryReader {
    private val wasmMemory = memory.memory
    private val defaultMemoryReader = DefaultWasiMemoryReader(memory)

    override fun read(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray
    ): ULong {
        return if (strategy == CHANGE_POSITION) {
            read(channel, iovecs)
        } else {
            defaultMemoryReader.read(channel, strategy, iovecs)
        }
    }

    private fun read(
        channel: FdChannel,
        iovecs: IovecArray
    ): ULong {
        var totalBytesRead: ULong = 0U
        try {
            for (vec in iovecs.iovecList) {
                val inputStream = Channels.newInputStream(channel.channel)
                val limit = vec.bufLen.value.toInt()
                val bytesRead = wasmMemory.copyFromStream(memory.node, inputStream, vec.buf.addr, limit)
                if (bytesRead > 0) {
                    totalBytesRead += bytesRead.toULong()
                }
                if (bytesRead < limit) {
                    break
                }
            }
        } catch (cce: ClosedChannelException) {
            throw SysException(Errno.IO, "Channel closed", cce)
        } catch (ace: AsynchronousCloseException) {
            throw SysException(Errno.IO, "Channel closed on other thread", ace)
        } catch (ci: ClosedByInterruptException) {
            throw SysException(Errno.INTR, "Interrupted", ci)
        } catch (nre: NonReadableChannelException) {
            throw SysException(Errno.BADF, "Non readable channel", nre)
        } catch (ioe: IOException) {
            throw SysException(Errno.IO, "I/o error", ioe)
        }

        return totalBytesRead
    }
}