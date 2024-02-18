package org.example.app.bindings

import org.graalvm.polyglot.Value

class SqliteMemoryBindings(
    private val mainBindings: Value,
    //val memory: Value,
) {
    val malloc = mainBindings.getMember("malloc") // 2815
    val free = mainBindings.getMember("free") // 2816
    val realloc = mainBindings.getMember("realloc") // 2817

    val stackSave = mainBindings.getMember("stackSave") // 2838
    val stackRestore = mainBindings.getMember("stackRestore") // 2839
    val stackAlloc = mainBindings.getMember("stackAlloc") // 2840

    private val emscripten_builtin_memalign = mainBindings.getMember("emscripten_builtin_memalign") // 2819
    private val emscripten_stack_init = mainBindings.getMember("emscripten_stack_init")
    private val emscripten_stack_get_free = mainBindings.getMember("emscripten_stack_get_free")
    private val emscripten_stack_get_base = mainBindings.getMember("emscripten_stack_get_base")
    private val emscripten_stack_get_end = mainBindings.getMember("emscripten_stack_get_end")
    private val emscripten_stack_get_current = mainBindings.getMember("emscripten_stack_get_end")

    private val sqlite3_malloc = mainBindings.getMember("sqlite3_malloc") // 63
    private val sqlite3_free = mainBindings.getMember("sqlite3_free") // 64
    private val sqlite3_realloc = mainBindings.getMember("sqlite3_realloc") // 74
    private val sqlite3_malloc64 = mainBindings.getMember("sqlite3_malloc64") // 73
    private val sqlite3_realloc64 = mainBindings.getMember("sqlite3_realloc64") // 76

    // https://github.com/emscripten-core/emscripten/blob/main/system/lib/README.md
    public fun init() {
        initEmscriptenStack()
    }

//    public fun allocOrThrow(len: UInt): Value {
//        check (len > 0U)
//        val mem = sqlite3_malloc.apply(
//            Size(len).value
//        ).getOrNull(0)
//
//        if (mem.isNull()) throw OutOfMemoryError()
//
//        return mem!!
//    }
//
//    public fun free(value: Value) {
//        sqlite3_free.apply(value)
//    }
//
//    public fun freeSilent(value: Value): Result<Unit> = kotlin.runCatching {
//        free(value)
//    }
//
//    public fun allocNullTerminatedString(string: String): Value {
//        val bytes = string.encodeToByteArray()
//        val mem = allocOrThrow(bytes.size.toUInt() + 1U)
//        memory.writeNullTerminatedString(mem.asWasmAddr(), string)
//        return mem
//    }
//
//    fun readAddr(offset: WasmPtr): Value = memory.readI32(offset)
//
//    fun writeAddr(offset: WasmPtr, addr: Value) {
//        check(addr.type() == ValueType.I32)
//        memory.write(offset, addr)
//    }
//
//    fun readNullTerminatedString(offsetValue: Value): String? = memory.readNullTerminatedString(offsetValue)
//
    private fun initEmscriptenStack() {
        emscripten_stack_init.execute()
        //writeStackCookie()
        //checkStackCookie()
    }
//
//    private fun writeStackCookie() {
//        var max = emscripten_stack_get_end.apply()[0].asInt()
//        check(max.and(0x03) == 0)
//
//        if (max == 0) max = 4
//
//        memory.writeI32(max, 0x02135467)
//        memory.writeI32(max + 4, 0x89BACDFEU.toInt())
//        memory.writeI32(0, 1668509029)
//    }
//
//    private fun checkStackCookie() {
//        var max = emscripten_stack_get_end.apply()[0].asInt()
//        check(max.and(0x03) == 0)
//
//        if (max == 0) max = 4
//
//        val cookie1 = memory.readI32(max).asInt()
//        val cookie2 = memory.readI32(max + 4).asInt()
//
//        check (cookie1 == 0x02135467 && cookie2 == 0x89BACDFEU.toInt()) {
//            "Stack overflow! Stack cookie has been overwritten at ${max.toString(16)}, expected hex dwords " +
//                    "0x89BACDFE and 0x2135467, but received ${cookie2.toString(16)} ${cookie2.toString(16)}"
//        }
//    }
}