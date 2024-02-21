package org.example.app.host.memory

import java.nio.ByteOrder
import org.graalvm.polyglot.Value
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.host.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.host.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.memory.WasiMemoryReader
import ru.pixnews.wasm.host.memory.WasiMemoryWriter
import ru.pixnews.wasm.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.host.WasmPtr

class GraalHostMemoryImpl(
    val memory: Value
) : Memory {
    private val memoryReader: WasiMemoryReader = DefaultWasiMemoryReader(this)
    private val memoryWriter: WasiMemoryWriter = DefaultWasiMemoryWriter(this)

    override fun readI8(addr: WasmPtr<*>): Byte {
        return memory.readBufferByte(addr.addr.toLong())
    }

    override fun readI32(addr: WasmPtr<*>): Int {
        return memory.readBufferInt(ByteOrder.LITTLE_ENDIAN, addr.addr.toLong())
    }

    override fun readBytes(addr: WasmPtr<*>, length: Int): ByteArray = ByteArray(length) { offset ->
        memory.readBufferByte(addr.addr + offset.toLong())
    }

    override fun writeByte(addr: WasmPtr<*>, data: Byte) {
        memory.writeBufferByte(addr.addr.toLong(), data)
    }

    override fun writeI32(addr: WasmPtr<*>, data: Int) {
        memory.writeBufferInt(ByteOrder.LITTLE_ENDIAN, addr.addr.toLong(), data)
    }

    override fun writeI64(addr: WasmPtr<*>, data: Long) {
        memory.writeBufferLong(ByteOrder.LITTLE_ENDIAN, addr.addr.toLong(), data)
    }

    override fun write(addr: WasmPtr<*>, data: ByteArray, offset: Int, size: Int) {
        // TODO
        data.forEachIndexed { index, byte ->
            memory.writeBufferByte(addr.addr + index.toLong(), byte)
        }
    }

    override fun readFromChannel(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray
    ): ULong = memoryReader.read(channel, strategy, iovecs)

    override fun writeToChannel(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        cioVecs: CiovecArray
    ): ULong = memoryWriter.write(channel, strategy, cioVecs)
}