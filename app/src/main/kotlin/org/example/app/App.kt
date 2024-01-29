package org.example.app

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

private object App

private val wasmContext = Context.newBuilder("wasm")
    //.allowAllAccess(true)
    //.option("wasm.Builtins", "wasi_snapshot_preview1")
    .build()
private val factorialSource = run {
    val factorialWasmUrl = requireNotNull(App::class.java.getResource("factorial.wasm"))
    Source.newBuilder("wasm", factorialWasmUrl).build()
}

fun main() {
    wasmContext.eval(factorialSource)
    val factorial: Value = wasmContext.getBindings("wasm")
        .getMember("main")
        .getMember("fac")
    println("wasm: factorial(20) = " + factorial.execute(20L))
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