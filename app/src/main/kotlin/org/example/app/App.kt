package org.example.app

import kotlin.time.measureTimedValue
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Language
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.WasmOptions
import org.graalvm.wasm.WasmType
import org.graalvm.wasm.WasmType.I32_TYPE
import org.graalvm.wasm.api.Dictionary
import org.graalvm.wasm.api.WebAssembly
import org.graalvm.wasm.constants.Sizes
import org.graalvm.wasm.constants.Sizes.MAX_MEMORY_64_DECLARATION_SIZE
import org.graalvm.wasm.constants.Sizes.MAX_MEMORY_DECLARATION_SIZE

private object App

fun main() {
    testSqlite()
}


private fun testSqlite() {
    val (sqlite3Initialize, evalDuration) = measureTimedValue {
        val wasmContext: Context = Context.newBuilder("wasm")
            .allowAllAccess(true)
            .option("wasm.Builtins", "wasi_snapshot_preview1")
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

        val main = wasmContext.getBindings("wasm").getMember("main")

        main.getMember("sqlite3_initialize")
    }
    println("wasm: binding = $sqlite3Initialize. duration: $evalDuration")

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