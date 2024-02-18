package ru.pixnews.wasm.sqlite3.chicory

import SqliteBasicDemo1
import com.dylibso.chicory.runtime.HostGlobal
import com.dylibso.chicory.runtime.HostImports
import com.dylibso.chicory.runtime.HostMemory
import com.dylibso.chicory.runtime.HostTable
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.runtime.Module
import com.dylibso.chicory.wasm.types.MemoryLimits
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.LogManager
import kotlin.time.measureTimedValue
import ru.pixnews.sqlite3.wasm.Sqlite3Wasm
import ru.pixnews.wasm.sqlite3.chicory.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.EmscriptenEnvBindings
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.ChicoryMemoryImpl
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WasiSnapshotPreview1Builtins

object App

fun main() {
    App::class.java.getResource("logging.properties")!!.openStream().use {
        LogManager.getLogManager().readConfiguration(it)
    }
    //testFactorial()
    testSqlite()
}

const val INITIAL_MEMORY_PAGES = 16_777_216 / 65536
const val MAX_MEMORY_PAGES = 4_294_967_296 / 65536

private fun testSqlite() {
    val (bindings, evalDuration) = measureTimedValue {
        val sqlite3Module = Sqlite3Wasm.Emscripten.sqlite3_346_o0_debug.openStream().use {
            Module.builder(it)
                .build()
        }
        val hostImports = setupHostImports()
        val instance: Instance = sqlite3Module.instantiate(hostImports)

        val memory = ChicoryMemoryImpl(hostImports.memory(0).memory())

        val bingings = SqliteBindings(memory, instance)

        bingings
    }
    println("wasm: init duration: $evalDuration")

    val demo1 = SqliteBasicDemo1(bindings)
    demo1.run()
}

private fun setupHostImports() : HostImports {
    val filesystem = FileSystem()
    val fsWasiBuildins = WasiSnapshotPreview1Builtins(filesystem)
    val emscriptenEnvBindings = EmscriptenEnvBindings(filesystem)

    val hostMemory = HostMemory(
        /* moduleName = */ "env",
        /* fieldName = */ "memory",
        /* memory = */ Memory(
            MemoryLimits(
                INITIAL_MEMORY_PAGES,
                MAX_MEMORY_PAGES
            )
        )
    )

    return HostImports(
        (emscriptenEnvBindings.functions
                + fsWasiBuildins.functions).toTypedArray(),
        arrayOf<HostGlobal>(),
        hostMemory,
        arrayOf<HostTable>()
    )
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