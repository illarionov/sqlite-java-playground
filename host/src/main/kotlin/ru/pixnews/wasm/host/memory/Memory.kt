package ru.pixnews.wasm.host.memory

import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

interface Memory {
    fun readI8(addr: WasmPtr): Byte
    fun readI32(addr: WasmPtr): Int

    fun writeByte(addr: WasmPtr, data: Byte)
    fun writeI32(addr: WasmPtr, data: Int)
    fun write(addr: WasmPtr, data: ByteArray, offset: Int, size: Int)
}

fun Memory.readU8(addr: WasmPtr): UByte = readI8(addr).toUByte()
fun Memory.readU32(addr: WasmPtr): UInt = readI32(addr).toUInt()

fun Memory.readPtr(addr: WasmPtr): WasmPtr = readI32(addr)
fun Memory.writePtr(addr: WasmPtr, data: WasmPtr) = writeI32(addr, data)

fun Memory.write(addr: Int, data: ByteArray) = write(addr, data, 0, data.size)

