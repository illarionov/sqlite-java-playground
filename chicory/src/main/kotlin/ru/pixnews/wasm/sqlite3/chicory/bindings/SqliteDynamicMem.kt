package ru.pixnews.wasm.sqlite3.chicory.bindings

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.isNull
import ru.pixnews.wasm.sqlite3.chicory.ext.writeNullTerminatedString
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Size

class SqliteDynamicMem(
    public val memory: Memory,
    private val runtimeInstance: Instance,
) {
    val malloc = runtimeInstance.export("malloc") // 2815
    val free = runtimeInstance.export("free") // 2816
    val realloc = runtimeInstance.export("realloc") // 2817
    val stackSave = runtimeInstance.export("stackSave") // 2838
    val stackRestore = runtimeInstance.export("stackRestore") // 2839
    // val stackAlloc = runtimeInstance.export("stackAlloc") // 2840
    // val emscripten_builtin_memalign = runtimeInstance.export("emscripten_builtin_memalign") // 2819

    val sqlite3_malloc = runtimeInstance.export("sqlite3_malloc") // 63
    val sqlite3_free = runtimeInstance.export("sqlite3_free") // 64
    val sqlite3_realloc = runtimeInstance.export("sqlite3_realloc") // 74
    val sqlite3_malloc64 = runtimeInstance.export("sqlite3_malloc64") // 73
    val sqlite3_realloc64 = runtimeInstance.export("sqlite3_realloc64") // 76

    public fun allocOrThrow(len: UInt): Value {
        check (len > 0U)
        val mem = sqlite3_malloc.apply(
            Size(len).value
        ).getOrNull(0)

        if (mem.isNull()) throw OutOfMemoryError()

        return mem!!
    }

    public fun free(value: Value) {
        sqlite3_free.apply(value)
    }

    public fun allocNullTerminatedString(string: String): Value {
        val bytes = string.encodeToByteArray()
        val mem = allocOrThrow(bytes.size.toUInt() + 1U)
        memory.writeNullTerminatedString(mem.asWasmAddr(), string)
        return mem
    }

}