package org.example.app.host

import org.example.app.host.emscrypten.func.notImplementedFunctionNodeFactory
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage

typealias NodeFactory = (
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String,
) -> BaseWasmRootNode

class HostFunction(
    val name: String,
    val type: HostFunctionType,
    val nodeFactory: NodeFactory = notImplementedFunctionNodeFactory
) {
    constructor(
        name: String,
        paramTypes: List<Byte>,
        retTypes: List<Byte> = listOf(),
        nodeFactory: NodeFactory = notImplementedFunctionNodeFactory
    ) : this(name, HostFunctionType(paramTypes, retTypes), nodeFactory)
}

data class HostFunctionType(
    val params: List<Byte>,
    val returnTypes: List<Byte> = listOf(),
)