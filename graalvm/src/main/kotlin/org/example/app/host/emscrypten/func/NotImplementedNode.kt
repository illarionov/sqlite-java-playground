package org.example.app.host.emscrypten.func

import com.oracle.truffle.api.frame.VirtualFrame
import org.example.app.host.BaseWasmRootNode
import org.example.app.host.Host
import org.example.app.host.NodeFactory
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.predefined.WasmBuiltinRootNode


val notImplementedFunctionNodeFactory: NodeFactory = { language, instance, _, name -> NotImplementedNode(language, instance, name) }

class NotImplementedNode(
    language: WasmLanguage,
    instance: WasmInstance,
    private val name: String,
) : BaseWasmRootNode(language, instance, name) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Any {
        error("`$name`not implemented")
    }
}