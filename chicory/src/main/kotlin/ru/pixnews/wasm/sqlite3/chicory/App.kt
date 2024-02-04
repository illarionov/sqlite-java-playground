package ru.pixnews.wasm.sqlite3.chicory

import com.dylibso.chicory.runtime.HostGlobal
import com.dylibso.chicory.runtime.HostImports
import com.dylibso.chicory.runtime.HostMemory
import com.dylibso.chicory.runtime.HostTable
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.runtime.Module
import com.dylibso.chicory.wasm.types.MemoryLimits
import com.dylibso.chicory.wasm.types.Value
import kotlin.time.measureTimedValue
import ru.pixnews.sqlite3.wasm.Sqlite3Wasm
import ru.pixnews.wasm.sqlite3.chicory.sqlite3.Sqlite3CApi
import ru.pixnews.wasm.sqlite3.chicory.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite3.chicory.host.SyscallBindings
import ru.pixnews.wasm.sqlite3.chicory.host.WasiSnapshotPreview1Builtins

fun main() {
    //testFactorial()
    testSqlite()
}

const val INITIAL_MEMORY_PAGES = 16_777_216 / 65536
const val MAX_MEMORY_PAGES = 4_294_967_296 / 65536

private fun testSqlite() {
    val (bindings, evalDuration) = measureTimedValue {
        val sqlite3Module = Sqlite3Wasm.Emscripten.sqlite3_346.openStream().use {
            Module.builder(it)
                .build()
        }
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
        val fsWasiBuildins = WasiSnapshotPreview1Builtins()
        val envSyscallBindings = SyscallBindings()

        val hostImports = HostImports(
            (envSyscallBindings.functions
                    + fsWasiBuildins.functions).toTypedArray(),
            arrayOf<HostGlobal>(),
            hostMemory,
            arrayOf<HostTable>()
        )
        val instance: Instance = sqlite3Module.instantiate(hostImports)

        val bingings = SqliteBindings(
            hostMemory.memory(),
            instance
        )

        bingings
    }
    val api = Sqlite3CApi(bindings)

    val (result, resultDuration) = measureTimedValue {
        api.version
    }

    println("wasm: sqlite3_libversion_number = $result. duration: $evalDuration / $resultDuration")
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