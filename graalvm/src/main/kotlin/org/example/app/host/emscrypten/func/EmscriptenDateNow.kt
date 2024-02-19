package org.example.app.host.emscrypten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage

class EmscriptenDateNow(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "emscripten_date_now",
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Double {
        return emscriptenDateNow()
    }

    @TruffleBoundary
    private fun emscriptenDateNow() : Double {
        return host.clock.millis().toDouble()
    }
}