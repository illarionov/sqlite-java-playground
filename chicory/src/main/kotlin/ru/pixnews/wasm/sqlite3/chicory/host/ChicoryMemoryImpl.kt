package ru.pixnews.wasm.sqlite3.chicory.host

import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import com.dylibso.chicory.runtime.Memory as ChicoryMemory

public class ChicoryMemoryImpl(
    public val memory: ChicoryMemory
) : Memory {
    override fun readI8(addr: WasmPtr): Byte = memory.read(addr)

    override fun readI32(addr: WasmPtr): Int = memory.readI32(addr).asInt()

    override fun writeByte(addr: WasmPtr, data: Byte) = memory.writeByte(addr, data)
    override fun writeI32(addr: WasmPtr, data: Int) = memory.writeI32(addr, data)

    override fun write(
        addr: WasmPtr,
        data: ByteArray,
        offset: Int,
        size: Int) = memory.write(addr, data, offset, size)
}