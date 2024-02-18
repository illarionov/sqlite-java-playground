package org.example.app.host.memory

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

    override fun writeByte(addr: WasmPtr, data: Byte) {
        memory.writeBufferByte(addr.toLong(), data)
    }

    override fun writeI32(addr: WasmPtr, data: Int) {
        memory.writeBufferInt(ByteOrder.LITTLE_ENDIAN, addr.toLong(), data)
    }

    override fun write(addr: WasmPtr, data: ByteArray, offset: Int, size: Int) {
        // TODO
        data.forEachIndexed { index, byte ->
            memory.writeBufferByte(addr + index.toLong(), byte)
        }
    }
}