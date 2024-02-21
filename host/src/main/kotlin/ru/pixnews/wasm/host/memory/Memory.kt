package ru.pixnews.wasm.host.memory

import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.host.WasmPtr

interface Memory {
    fun readI8(addr: WasmPtr<*>): Byte
    fun readI32(addr: WasmPtr<*>): Int
    fun readBytes(addr: WasmPtr<*>, length: Int): ByteArray

    fun writeByte(addr: WasmPtr<*>, data: Byte)
    fun writeI32(addr: WasmPtr<*>, data: Int)
    fun writeI64(addr: WasmPtr<*>, data: Long)
    fun write(addr: WasmPtr<*>, data: ByteArray, offset: Int, size: Int)

    fun readFromChannel(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray,
    ): ULong

    fun writeToChannel(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        cioVecs: CiovecArray,
    ): ULong
}

fun Memory.readU8(addr: WasmPtr<*>): UByte = readI8(addr).toUByte()
fun Memory.readU32(addr: WasmPtr<*>): UInt = readI32(addr).toUInt()

fun <P: Any> Memory.readPtr(addr: WasmPtr<*>): WasmPtr<P> = WasmPtr(readI32(addr))
fun Memory.writePtr(addr: WasmPtr<*>, data: WasmPtr<*>) = writeI32(addr, data.addr)

fun Memory.write(addr: WasmPtr<*>, data: ByteArray) = write(addr, data, 0, data.size)

