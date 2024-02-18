package org.example.app.host

import org.example.app.host.emscrypten.func.NotImplementedNode
import org.example.app.host.emscrypten.func.notImplementedFunctionNodeFactory
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.filesystem.FileSystem

typealias NodeFactory = (
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
) -> BaseWasmRootNode

class HostFunction(
    val name: String,
    val type: HostFunctionType,
    val nodeFactory: NodeFactory = notImplementedFunctionNodeFactory(name)
) {
    constructor(
        name: String,
        paramTypes: List<Byte>,
        retTypes: List<Byte> = listOf(),
        nodeFactory: NodeFactory = notImplementedFunctionNodeFactory(name)
    ) : this(name, HostFunctionType(paramTypes, retTypes), nodeFactory)
}

data class HostFunctionType(
    val params: List<Byte>,
    val returnTypes: List<Byte> = listOf(),
)