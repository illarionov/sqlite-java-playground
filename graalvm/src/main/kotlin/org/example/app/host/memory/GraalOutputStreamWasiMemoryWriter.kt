package org.example.app.host.memory

import java.io.IOException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.Channels
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonReadableChannelException
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.host.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.host.memory.WasiMemoryWriter
import ru.pixnews.wasm.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.Errno

class GraalOutputStreamWasiMemoryWriter(
    private val memory: WasmHostMemoryImpl
) : WasiMemoryWriter {
    private val wasmMemory = memory.memory
    private val defaultMemoryWriter = DefaultWasiMemoryWriter(memory)

    override fun write(channel: FdChannel, strategy: ReadWriteStrategy, cioVecs: CiovecArray): ULong {
        return if (strategy == ReadWriteStrategy.CHANGE_POSITION) {
            write(channel, cioVecs)
        } else {
            defaultMemoryWriter.write(channel, strategy, cioVecs)
        }
    }

    private fun write(channel: FdChannel, cioVecs: CiovecArray): ULong {
        var totalBytesWritten: ULong = 0U
        try {
            for (vec in cioVecs.ciovecList) {
                val outputStream = Channels.newOutputStream(channel.channel)
                val limit = vec.bufLen.value.toInt()
                wasmMemory.copyToStream(memory.node, outputStream, vec.buf.addr, limit)
                totalBytesWritten += limit.toUInt()
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

        return totalBytesWritten
    }
}