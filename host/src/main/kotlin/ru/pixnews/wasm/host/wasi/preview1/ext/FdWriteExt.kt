package ru.pixnews.wasm.host.wasi.preview1.ext

import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.wasi.preview1.type.CioVec
import ru.pixnews.wasm.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.Size
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

object FdWriteExt {
    fun readCiovecs(
        memory: Memory,
        pCiov: WasmPtr,
        ciovCnt: Int
    ): CiovecArray {
        val iovecs = MutableList(ciovCnt) { idx ->
            val pCiovec = pCiov + 8 * idx
            CioVec(
                buf = memory.readI32(pCiovec),
                bufLen = Size(memory.readI32(pCiovec + 4).toUInt())
            )
        }
        return CiovecArray(iovecs)
    }

}