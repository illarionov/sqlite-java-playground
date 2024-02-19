package ru.pixnews.wasm.sqlite3.chicory.host.memory

import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.host.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.host.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.memory.WasiMemoryReader
import ru.pixnews.wasm.host.memory.WasiMemoryWriter
import ru.pixnews.wasm.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import com.dylibso.chicory.runtime.Memory as ChicoryMemory

public class ChicoryMemoryImpl(
    public val memory: ChicoryMemory
) : Memory {
    private val memoryReader: WasiMemoryReader = UnsafeWasiMemoryReader.create(memory)
        ?: DefaultWasiMemoryReader(this)
    private val memoryWriter: WasiMemoryWriter = UnsafeWasiMemoryWriter.create(memory)
        ?: DefaultWasiMemoryWriter(this)

    override fun readI8(addr: WasmPtr): Byte = memory.read(addr)

    override fun readI32(addr: WasmPtr): Int = memory.readI32(addr).asInt()
    override fun readBytes(addr: WasmPtr, length: Int): ByteArray = memory.readBytes(addr, length)

    override fun writeByte(addr: WasmPtr, data: Byte) = memory.writeByte(addr, data)

    override fun writeI32(addr: WasmPtr, data: Int) = memory.writeI32(addr, data)

    override fun writeI64(addr: WasmPtr, data: Long) = memory.writeLong(addr, data)

    override fun write(
        addr: WasmPtr,
        data: ByteArray,
        offset: Int,
        size: Int
    ) = memory.write(addr, data, offset, size)

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