package org.example.app.host

import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage

val notImplementedFunctionNodeFactory: NodeFactory = { language, instance, _, name ->
    NotImplementedBaseWasmNode(language, instance, name)
}

private class NotImplementedBaseWasmNode(
    language: WasmLanguage,
    instance: WasmInstance,
    private val name: String,
) : BaseWasmNode(language, instance, name) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Any {
        error("`$name`not implemented")
    }
}