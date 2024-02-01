package ru.pixnews.wasm.sqlite3.chicory

import com.dylibso.chicory.runtime.Module
import com.dylibso.chicory.wasm.types.Value
import kotlin.time.measureTimedValue
import ru.pixnews.sqlite3.wasm.Sqlite3Wasm

fun main() {
    testFactorial()
}

private fun testSqlite() {
//    val (sqlite3Bindings, evalDuration) = measureTimedValue {
//        val wasmContext: Context = Context.newBuilder("wasm")
//            .allowAllAccess(true)
//            //.option("wasm.Builtins", "wasi_snapshot_preview1")
//            .build()
//        wasmContext.initialize("wasm")

//        val webAssembly = wasmContext
//            .polyglotBindings
//            .getMember("WebAssembly")
//            .`as`(WebAssembly::class.java)
//            ?: error("Can not get WebAssembly instance")

//        wasmContext.enter()
//        try {
//            val instanceContext = WasmContext.get(null)
//            createSqliteEnvModule(instanceContext)
//
//            val wasiInstance = WasiSnapshotPreview1BuiltinsModule().createInstance(
//                instanceContext.language(),
//                instanceContext,
//                "wasi_snapshot_preview1"
//            )
//            instanceContext.register(wasiInstance)
//
//        } finally {
//            wasmContext.leave()
//        }
//
//        val sqliteSource: Source = run {
//            val sqliteUrl = Sqlite3Wasm.Emscripten.sqlite3_346
//            Source.newBuilder("wasm", sqliteUrl).build()
//        }
//        wasmContext.eval(sqliteSource)
//
//        val wasmMainBindings = wasmContext.getBindings("wasm")
//
//        println("keys: ${wasmMainBindings.memberKeys}")
//
//        SqliteBindings(
//            wasmContext.getBindings("wasm").getMember("env"),
//            wasmContext.getBindings("wasm")
//                .getMember("main")
//        )
//    }
//    println("wasm: binding = ${sqlite3Bindings.sqlite3_initialize}. duration: $evalDuration")
//
//
//    SqliteBasicDemo1(sqlite3Bindings).run()

}

private fun testFactorial() {
    val (factorialFunc, evalDuration) = measureTimedValue {
        val factorialModule = Sqlite3Wasm.factorialWasm.openStream().use {
            Module.builder(it)
                .build()
        }
        val instance = factorialModule.instantiate()

        instance.export("fac")
    }
    val (result, resultDuration) = measureTimedValue {
        factorialFunc.apply(Value.i64(20L))[0].asLong()
    }

    println("wasm: factorial(20) = $result. duration: $evalDuration / $resultDuration")
}