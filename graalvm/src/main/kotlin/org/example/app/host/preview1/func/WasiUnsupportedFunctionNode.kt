package org.example.app.host.preview1.func

import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.predefined.WasmBuiltinRootNode
import org.graalvm.wasm.predefined.wasi.types.Errno

class WasiUnsupportedFunctionNode(
    language: WasmLanguage,
    instance: WasmInstance,
    private val name: String
) : WasmBuiltinRootNode(
    language, instance
) {
    override fun builtinNodeName(): String = name

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext?): Any {
        return Errno.Nosys // error code for function not supported;
    }
}