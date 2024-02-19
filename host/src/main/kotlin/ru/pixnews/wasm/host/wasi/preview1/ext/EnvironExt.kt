package ru.pixnews.wasm.host.wasi.preview1.ext

import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.memory.encodedNullTerminatedStringLength
import ru.pixnews.wasm.host.memory.writeNullTerminatedString
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

object WasiEnvironmentFunc {
    fun environSizesGet(
        envProvider: () -> Map<String, String>,
        memory: Memory,
        environCountAddr: WasmPtr,
        environSizeAddr: WasmPtr,
    ): Errno {
        val env = envProvider()
        val count = env.size
        val dataLength = env.entries.sumOf { it.encodeEnvToWasi().encodedNullTerminatedStringLength() }

        memory.writeI32(environCountAddr, count)
        memory.writeI32(environSizeAddr, dataLength)
        return Errno.SUCCESS
    }

    fun environGet(
        envProvider: () -> Map<String, String>,
        memory: Memory,
        environPAddr: WasmPtr,
        environBufAddr: WasmPtr,
    ): Errno {
        var pp = environPAddr
        var bufP = environBufAddr

        envProvider()
            .entries
            .map { it.encodeEnvToWasi() }
            .forEach { envString ->
                memory.writeI32(pp, bufP)
                pp += 4
                bufP += memory.writeNullTerminatedString(bufP, envString)
            }

        return Errno.SUCCESS
    }

    // TODO: sanitize `=`?
    fun Map.Entry<String, String>.encodeEnvToWasi() = "${key}=${value}"
}
