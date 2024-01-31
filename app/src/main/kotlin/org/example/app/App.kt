package org.example.app

import kotlin.time.measureTimedValue
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Source
import org.graalvm.wasm.WasmContext

private object App

fun main() {
    testSqlite()
}


private fun testSqlite() {
    val (sqlite3Bindings, evalDuration) = measureTimedValue {
        val wasmContext: Context = Context.newBuilder("wasm")
            .allowAllAccess(true)
            //.option("wasm.Builtins", "wasi_snapshot_preview1")
            .build()
        wasmContext.initialize("wasm")

//        val webAssembly = wasmContext
//            .polyglotBindings
//            .getMember("WebAssembly")
//            .`as`(WebAssembly::class.java)
//            ?: error("Can not get WebAssembly instance")

        wasmContext.enter()
        try {
            val instanceContext = WasmContext.get(null)
            createSqliteEnvModule(instanceContext)

            val wasiInstance = WasiSnapshotPreview1BuiltinsModule().createInstance(
                instanceContext.language(),
                instanceContext,
                "wasi_snapshot_preview1"
            )
            instanceContext.register(wasiInstance)

        } finally {
            wasmContext.leave()
        }

        val sqliteSource: Source = run {
            val sqliteUrl = requireNotNull(App::class.java.getResource("sqlite3_3450000.wasm"))
            Source.newBuilder("wasm", sqliteUrl).build()
        }
        wasmContext.eval(sqliteSource)

        val wasmMainBindings = wasmContext.getBindings("wasm")

        println("keys: ${wasmMainBindings.memberKeys}")

        SqliteBindings(wasmContext.getBindings("wasm").getMember("main"))
    }
    println("wasm: binding = ${sqlite3Bindings.sqlite3_initialize}. duration: $evalDuration")

}

public class SqliteEnv(
) {
    @HostAccess.Export
    fun __syscall_rmdir(path: String): Int {
        return 0
    }
}

private fun testFactorial() {
    val (factorial, evalDuration) = measureTimedValue {
        val wasmContext = Context.newBuilder("wasm")
            //.allowAllAccess(true)
            //.option("wasm.Builtins", "wasi_snapshot_preview1")
            .build()
        val factorialSource = run {
            val factorialWasmUrl = requireNotNull(App::class.java.getResource("factorial.wasm"))
            Source.newBuilder("wasm", factorialWasmUrl).build()
        }

        wasmContext.eval(factorialSource)
        wasmContext.getBindings("wasm")
            .getMember("main")
            .getMember("fac")
    }
    val (result, resultDuration) = measureTimedValue {
        factorial.execute(20L)
    }
    println("wasm: factorial(20) = $result. duration: $evalDuration / $resultDuration")
}