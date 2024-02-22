package org.example.app.host

import org.example.app.host.memory.WasmHostMemoryImpl
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.nodes.WasmRootNode

open class BaseWasmNode(
    language: WasmLanguage,
    val instance: WasmInstance,
    val functionName: String,
) : WasmRootNode(language, null, null){
    val memory: WasmHostMemoryImpl by lazy(LazyThreadSafetyMode.PUBLICATION) {
        WasmHostMemoryImpl(
            instance.context().memories().memory(0),
            this
        )
    }

    override fun tryInitialize(context: WasmContext) {
        // Copied from WasmBuiltinRootNode.tryInitialize
        context.linker().tryLink(instance)
    }

    override fun getName(): String = "wasm-function:$functionName"
}
