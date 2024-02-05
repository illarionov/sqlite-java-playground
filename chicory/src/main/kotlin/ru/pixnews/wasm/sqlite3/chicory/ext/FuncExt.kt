package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.wasm.types.Value

internal const val WASI_SNAPSHOT_PREVIEW1 = "wasi_snapshot_preview1"

internal typealias WasmAddr = Int

internal fun Value.asWasmAddr(): WasmAddr = asInt()
