package org.example.app.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage

class EmscriptenGetNow(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "emscripten_get_now",
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Double {
        return emscriptenGetNow()
    }

    @CompilerDirectives.TruffleBoundary
    private fun emscriptenGetNow() : Double = System.nanoTime() / 1_000_000.0
}