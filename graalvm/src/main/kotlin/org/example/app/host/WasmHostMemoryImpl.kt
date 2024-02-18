package org.example.app.host

import com.oracle.truffle.api.nodes.Node
import java.io.ByteArrayInputStream
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

class WasmHostMemoryImpl(
    val memory: WasmMemory,
    private val node: Node?
) : Memory {
    public constructor(instance: WasmInstance, node: Node?) : this(
        instance.memory(0), node
    )

    public constructor(context: WasmContext, node: Node?) : this(
        context.memories().memory(0), node
    )

    override fun readI8(addr: WasmPtr): Byte {
        return memory.load_i32_8u(node, addr.toLong()).toByte()
    }

    override fun readI32(addr: WasmPtr): Int {
        return memory.load_i32(node, addr.toLong())
    }

    override fun writeByte(addr: WasmPtr, data: Byte) {
        memory.store_i32_8(node, addr.toLong(), data)
    }

    override fun writeI32(addr: WasmPtr, data: Int) {
        memory.store_i32(node, addr.toLong(), data)
    }

    override fun write(addr: WasmPtr, data: ByteArray, offset: Int, size: Int) {
        memory.copyFromStream(
            node,
            ByteArrayInputStream(data),
            addr,
            data.size
        )
    }

    fun readNullTerminatedString(
        offset: WasmPtr
    ): String = memory.readString(offset, null)

    fun writeNullTerminatedString(
        offset: WasmPtr,
        value: String,
    ): Int = memory.writeString(node, value, offset)
}