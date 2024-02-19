package org.example.app.host.memory

import java.io.ByteArrayOutputStream
import java.nio.ByteOrder
import org.graalvm.polyglot.Value
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

class GraalHostMemoryImpl(
    val memory: Value
) : Memory {
    override fun readI8(addr: WasmPtr): Byte {
        return memory.readBufferByte(addr.toLong())
    }

    override fun readI32(addr: WasmPtr): Int {
        return memory.readBufferInt(ByteOrder.LITTLE_ENDIAN, addr.toLong())
    }

    override fun readBytes(addr: WasmPtr, length: Int): ByteArray = ByteArray(length) { offset ->
        memory.readBufferByte(addr + offset.toLong())
    }

    override fun writeByte(addr: WasmPtr, data: Byte) {
        memory.writeBufferByte(addr.toLong(), data)
    }

    override fun writeI32(addr: WasmPtr, data: Int) {
        memory.writeBufferInt(ByteOrder.LITTLE_ENDIAN, addr.toLong(), data)
    }

    override fun writeI64(addr: WasmPtr, data: Long) {
        memory.writeBufferLong(ByteOrder.LITTLE_ENDIAN, addr.toLong(), data)
    }

    override fun write(addr: WasmPtr, data: ByteArray, offset: Int, size: Int) {
        // TODO
        data.forEachIndexed { index, byte ->
            memory.writeBufferByte(addr + index.toLong(), byte)
        }
    }
}