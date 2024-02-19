package org.example.app.host

import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.WasmValueType
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32

class HostFunction(
    val name: String,
    val type: HostFunctionType,
    val nodeFactory: NodeFactory = notImplementedFunctionNodeFactory
) {
    constructor(
        name: String,
        paramTypes: List<WasmValueType>,
        retTypes: List<WasmValueType> = listOf(),
        nodeFactory: NodeFactory = notImplementedFunctionNodeFactory
    ) : this(name, HostFunctionType(paramTypes, retTypes), nodeFactory)
}

data class HostFunctionType(
    val params: List<WasmValueType>,
    val returnTypes: List<WasmValueType> = listOf(),
)

typealias NodeFactory = (
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String,
) -> BaseWasmNode

internal fun MutableList<HostFunction>.fn(
    name: String,
    paramTypes: List<WasmValueType>,
    retType: WasmValueType = I32,
    nodeFactory: NodeFactory = notImplementedFunctionNodeFactory
) = add(HostFunction(name, paramTypes, listOf(retType), nodeFactory))

internal fun MutableList<HostFunction>.fnVoid(
    name: String,
    paramTypes: List<WasmValueType>,
    nodeFactory: NodeFactory = notImplementedFunctionNodeFactory
) = add(HostFunction(name, paramTypes, emptyList(), nodeFactory))
