package org.example.app.host.emscrypten.func

import com.oracle.truffle.api.frame.VirtualFrame
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage

class EmscriptenGetNowIsMonotonic(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "_emscripten_get_now_is_monotonic",
    private val isMonotonic: Boolean = true,
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int = if (isMonotonic) 1 else 0
}