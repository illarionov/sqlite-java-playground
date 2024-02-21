package ru.pixnews.wasm.sqlite3.chicory.bindings

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.host.memory.readNullableNullTerminatedString
import ru.pixnews.wasm.host.memory.writeNullTerminatedString
import ru.pixnews.wasm.host.memory.writePtr
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.sqlite3.chicory.ext.asValue
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.isNull
import ru.pixnews.wasm.sqlite3.chicory.host.memory.ChicoryMemoryImpl

class SqliteMemoryBindings(
    val memory: ChicoryMemoryImpl,
    runtimeInstance: Instance,
) {
    private val malloc = runtimeInstance.export("malloc") // 2815
    private val free = runtimeInstance.export("free") // 2816
    private val realloc = runtimeInstance.export("realloc") // 2817

    private val stackAlloc = runtimeInstance.export("stackAlloc") // 2840
    private val stackSave = runtimeInstance.export("stackSave") // 2838
    private val stackRestore = runtimeInstance.export("stackRestore") // 2839

    private val emscripten_builtin_memalign = runtimeInstance.export("emscripten_builtin_memalign") // 2819
    private val emscripten_stack_init = runtimeInstance.export("emscripten_stack_init")
    private val emscripten_stack_get_free = runtimeInstance.export("emscripten_stack_get_free")
    private val emscripten_stack_get_base = runtimeInstance.export("emscripten_stack_get_base")
    private val emscripten_stack_get_end = runtimeInstance.export("emscripten_stack_get_end")
    private val emscripten_stack_get_current = runtimeInstance.export("emscripten_stack_get_end")

    private val sqlite3_malloc = runtimeInstance.export("sqlite3_malloc") // 63
    private val sqlite3_free = runtimeInstance.export("sqlite3_free") // 64
    private val sqlite3_realloc = runtimeInstance.export("sqlite3_realloc") // 74
    private val sqlite3_malloc64 = runtimeInstance.export("sqlite3_malloc64") // 73
    private val sqlite3_realloc64 = runtimeInstance.export("sqlite3_realloc64") // 76

    // https://github.com/emscripten-core/emscripten/blob/main/system/lib/README.md
    fun init() {
        initEmscriptenStack()
    }

    fun <P: Any?> allocOrThrow(len: UInt): WasmPtr<P> {
        check (len > 0U)
        val mem = sqlite3_malloc.apply(
            Value.i32(len.toLong())
        ).getOrNull(0)

        if (mem.isNull()) throw OutOfMemoryError()

        return mem!!.asWasmAddr()
    }

    fun free(pointer: WasmPtr<*>) {
        sqlite3_free.apply(pointer.asValue())
    }

    fun freeSilent(value: WasmPtr<*>): Result<Unit> = kotlin.runCatching {
        free(value)
    }

    fun allocNullTerminatedString(string: String): WasmPtr<Byte> {
        val bytes = string.encodeToByteArray()
        val mem = allocOrThrow<Byte>(bytes.size.toUInt() + 1U)
        memory.writeNullTerminatedString(mem, string)
        return mem
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any, P: WasmPtr<T>> readAddr(offset: WasmPtr<P>): P = WasmPtr<T>(memory.readI32(offset)) as P

    fun writeAddr(offset: WasmPtr<*>, addr: Value) {
        check(addr.type() == ValueType.I32)
        memory.writePtr(offset, addr.asWasmAddr<Unit>())
    }

    fun readNullTerminatedString(offsetValue: WasmPtr<Byte>): String? = memory.readNullableNullTerminatedString(offsetValue)

    private fun initEmscriptenStack() {
        emscripten_stack_init.apply()
        writeStackCookie()
        checkStackCookie()
    }

    private fun writeStackCookie() {
        var max = emscripten_stack_get_end.apply()[0].asInt()
        check(max.and(0x03) == 0)

        if (max == 0) max = 4

        memory.writeI32(WasmPtr<Int>(max), 0x02135467)
        memory.writeI32(WasmPtr<Int>(max + 4), 0x89BACDFEU.toInt())
        memory.writeI32(WasmPtr<Int>(0), 1668509029)
    }

    private fun checkStackCookie() {
        var max = emscripten_stack_get_end.apply()[0].asInt()
        check(max.and(0x03) == 0)

        if (max == 0) max = 4

        val cookie1 = memory.readI32(WasmPtr<Int>(max))
        val cookie2 = memory.readI32(WasmPtr<Int>(max + 4))

        check (cookie1 == 0x02135467 && cookie2 == 0x89BACDFEU.toInt()) {
            "Stack overflow! Stack cookie has been overwritten at ${max.toString(16)}, expected hex dwords " +
                    "0x89BACDFE and 0x2135467, but received ${cookie2.toString(16)} ${cookie2.toString(16)}"
        }
    }
}