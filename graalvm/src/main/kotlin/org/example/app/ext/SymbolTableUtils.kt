package org.example.app.ext

import org.example.app.host.HostFunction
import org.example.app.host.HostFunctionType
import org.graalvm.wasm.SymbolTable
import org.graalvm.wasm.WasmFunction
import ru.pixnews.wasm.host.WasmValueType

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

private fun List<WasmValueType>.toTypesByteArray(): ByteArray = ByteArray(this.size) {
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