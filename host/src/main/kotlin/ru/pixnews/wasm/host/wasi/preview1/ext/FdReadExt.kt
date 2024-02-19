package ru.pixnews.wasm.host.wasi.preview1.ext

import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.wasi.preview1.type.Iovec
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.Size
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

object FdReadExt {

    fun readIovecs(
        memory: Memory,
        pIov: WasmPtr,
        iovCnt: Int
    ): IovecArray {
        val iovecs = MutableList(iovCnt) { idx ->
            val pIovec = pIov + 8 * idx
            Iovec(
                buf = memory.readI32(pIovec),
                bufLen = Size(memory.readI32(pIovec + 4).toUInt())
            )
        }
        return IovecArray(iovecs)
    }


}