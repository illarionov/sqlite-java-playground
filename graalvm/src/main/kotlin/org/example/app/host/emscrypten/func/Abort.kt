package org.example.app.host.emscrypten.func

import com.oracle.truffle.api.frame.VirtualFrame
import org.example.app.host.BaseWasmNode
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage

class Abort(
    language: WasmLanguage,
    instance: WasmInstance,
    functionName: String = "abort",
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        error("native code called abort()")
    }
}