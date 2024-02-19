package org.example.app.host.memory

import com.oracle.truffle.api.nodes.Node
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

class WasmHostMemoryImpl(
    val memory: WasmMemory,
    private val node: Node?
) : Memory {
    override fun readI8(addr: WasmPtr): Byte {
        return memory.load_i32_8u(node, addr.toLong()).toByte()
    }

    override fun readI32(addr: WasmPtr): Int {
        return memory.load_i32(node, addr.toLong())
    }

    override fun readBytes(addr: WasmPtr, length: Int): ByteArray {
        val bous = ByteArrayOutputStream(length)
        memory.copyToStream(node, bous, addr, length)
        return bous.toByteArray()
    }

    override fun writeByte(addr: WasmPtr, data: Byte) {
        memory.store_i32_8(node, addr.toLong(), data)
    }

    override fun writeI32(addr: WasmPtr, data: Int) {
        memory.store_i32(node, addr.toLong(), data)
    }

    override fun writeI64(addr: WasmPtr, data: Long) {
        memory.store_i64(node, addr.toLong(), data)
    }

    override fun write(addr: WasmPtr, data: ByteArray, offset: Int, size: Int) {
        memory.initialize(data, offset, addr.toLong(), size)
    }

    fun readNullTerminatedString(
        offset: WasmPtr
    ): String = memory.readString(offset, null)
}