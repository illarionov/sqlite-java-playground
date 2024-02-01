package ru.pixnews.wasm.sqlite3.chicory

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.HostGlobal
import com.dylibso.chicory.runtime.HostImports
import com.dylibso.chicory.runtime.HostMemory
import com.dylibso.chicory.runtime.HostTable
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.runtime.Module
import com.dylibso.chicory.wasm.types.MemoryLimits
import com.dylibso.chicory.wasm.types.Value
import kotlin.time.measureTimedValue
import ru.pixnews.sqlite3.wasm.Sqlite3Wasm

fun main() {
    //testFactorial()
    testSqlite()
}

const val INITIAL_MEMORY_PAGES = 16_777_216 / 65536
const val MAX_MEMORY_PAGES = 4_294_967_296 / 65536

private fun testSqlite() {
    val (libversionNumberFunc, evalDuration) = measureTimedValue {
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
        val hostImports = HostImports(
            arrayOf<HostFunction>(),
            arrayOf<HostGlobal>(),
            hostMemory,
            arrayOf<HostTable>()
        )
        val instance = sqlite3Module.instantiate(hostImports)

        instance.export("sqlite3_libversion_number")
    }
    val (result, resultDuration) = measureTimedValue {
        libversionNumberFunc.apply()[0].asLong()
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