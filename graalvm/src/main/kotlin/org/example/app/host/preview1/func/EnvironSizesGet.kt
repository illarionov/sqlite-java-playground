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

class EnvironSizesGet(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "environ_sizes_get",
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return environSizesGet(args[0] as WasmPtr, args[1] as WasmPtr)
    }

    @TruffleBoundary
    private fun environSizesGet(
        environCountAddr: WasmPtr,
        environSizeAddr: WasmPtr,
    ): Int {
        return WasiEnvironmentFunc.environSizesGet(
            envProvider = host.systemEnvProvider,
            memory = memory,
            environCountAddr = environCountAddr,
            environSizeAddr = environSizeAddr
        ).code
    }
}