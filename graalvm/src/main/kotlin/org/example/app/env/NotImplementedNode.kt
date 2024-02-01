package org.example.app.env

import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.predefined.WasmBuiltinRootNode

class NotImplementedNode(
    language: WasmLanguage,
    instance: WasmInstance,
    name: String,
) : WasmBuiltinRootNode(language, instance) {
    override fun builtinNodeName(): String = name

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Any {
        error("not implemented")
    }
}