package org.example.app.ext

import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmTable

internal inline fun <R> Context.withWasmContext(
    code: (wasmContext: WasmContext) -> R
): R = try {
    enter()
    code(WasmContext.get(null))
} finally {
    leave()
}

internal val WasmContext.functionTable: WasmTable
    get() = this.tables().table(0)