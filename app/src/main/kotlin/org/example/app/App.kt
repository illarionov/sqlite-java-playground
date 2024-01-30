package org.example.app

import kotlin.time.measureTimedValue
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value

private object App

fun main() {
    testSqlite()
}

private fun testSqlite() {
    val (sqlite3Initialize, evalDuration) = measureTimedValue {
        val wasmContext = Context.newBuilder("wasm")
            .allowAllAccess(true)
            .option("wasm.Builtins", "wasi_snapshot_preview1")
            //.option("wasm.Builtins", "emscripten")
            .build()
        val sqliteSource = run {
            //val factorialWasmUrl = requireNotNull(App::class.java.getResource("sqlite3_3450000.wasm"))
            val factorialWasmUrl = requireNotNull(App::class.java.getResource("sqlite3-wasi-sdk.wasm"))
            Source.newBuilder("wasm", factorialWasmUrl).build()
        }
        wasmContext.eval(sqliteSource)

        val wasmMainBindings = wasmContext.getBindings("wasm")

        println("keys: ${wasmMainBindings.memberKeys}")

        wasmContext.getBindings("wasm")
            .getMember("main")
            .getMember("sqlite3_initialize")
    }
    println("wasm: binding = $sqlite3Initialize. duration: $evalDuration")

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

private fun test2() {
    Context.newBuilder().allowAllAccess(true).build().use { context ->
        val languages = context.engine.languages.keys
        for (id in languages) {
            println("Initializing language $id")
            context.initialize(id)
            when (id) {
                "python" -> context.eval("python", "print('Hello Python!')")
                "js" -> context.eval("js", "print('Hello JavaScript!');")
                "ruby" -> context.eval("ruby", "puts 'Hello Ruby!'")
                "wasm" -> {
                    context.eval(Source.newBuilder("wasm", App::class.java.getResource("factorial.wasm")).build())
                    val factorial: Value = context.getBindings("wasm").getMember("main").getMember("fac")
                    println("wasm: factorial(20) = " + factorial.execute(20L))
                }

                "java" -> {
                    val out: Value = context
                        .getBindings("java")
                        .getMember("java.lang.System")
                        .getMember("out")
                    out.invokeMember("println", "Hello Espresso Java!")
                }
            }
        }
    }
}