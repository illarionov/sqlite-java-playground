package org.example.app.host.preview1.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.wasi.preview1.ext.WasiEnvironmentFunc
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

class EnvironGet(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "environ_get",
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return environGet(args[0] as WasmPtr, args[1] as WasmPtr)
    }

    @TruffleBoundary
    private fun environGet(
        environPAddr: WasmPtr,
        environBufAddr: WasmPtr,
    ): Int {
        return WasiEnvironmentFunc.environGet(
            envProvider = host.systemEnvProvider,
            memory = memory,
            environPAddr = environPAddr,
            environBufAddr = environBufAddr
        ).code
    }
}