package org.example.app.ext

import org.example.app.host.Host
import org.example.app.host.HostFunction
import org.example.app.host.HostFunctionType
import org.graalvm.wasm.SymbolTable
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmFunction
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.host.WasmValueType

internal fun setupWasmModuleFunctions(
    context: WasmContext,
    host: Host,
    module: WasmModule,
    functions: List<HostFunction>
): WasmInstance {
    val functionTypes: Map<HostFunctionType, Int> = allocateFunctionTypes(module, functions)
    val exportedFunctions: Map<String, WasmFunction> = declareExportedFunctions(module, functionTypes, functions)

    val moduleInstance: WasmInstance = context.readInstance(module)

    functions.forEach { f: HostFunction ->
        val node = f.nodeFactory(context.language(), moduleInstance, host, f.name)
        val exportedIndex = exportedFunctions.getValue(f.name).index()
        moduleInstance.setTarget(exportedIndex, node.callTarget)
    }
    return moduleInstance
}

internal fun allocateFunctionTypes(
    symbolTable: SymbolTable,
    functions: List<HostFunction>
): Map<HostFunctionType, Int> {
    val functionTypeMap: MutableMap<HostFunctionType, Int> = mutableMapOf()
    functions.forEach { f ->
        val type: HostFunctionType = f.type
        functionTypeMap.getOrPut(type) {
            val typeIdx = symbolTable.allocateFunctionType(
                type.params.toTypesByteArray(),
                type.returnTypes.toTypesByteArray(),
                false
            )
            typeIdx
        }
    }
    return functionTypeMap
}

internal fun List<WasmValueType>.toTypesByteArray(): ByteArray = ByteArray(this.size) {
    requireNotNull(this[it].opcode).toByte()
}

fun declareExportedFunctions(
    symbolTable: SymbolTable,
    functionTypes: Map<HostFunctionType, Int>,
    functions: List<HostFunction>
): Map<String, WasmFunction> {
    return functions.associate { f ->
        val typeIdx = functionTypes.getValue(f.type)
        val functionIdx = symbolTable.declareExportedFunction(typeIdx, f.name)
        f.name to functionIdx
    }
}