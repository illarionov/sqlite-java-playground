package org.example.app.host.emscrypten

import org.example.app.env.NotImplementedNode
import org.example.app.env.SyscallRmdirNode
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmFunction
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.WasmType.F64_TYPE
import org.graalvm.wasm.WasmType.I32_TYPE
import org.graalvm.wasm.WasmType.I64_TYPE
import org.graalvm.wasm.constants.Sizes

val envFunctions: List<EmscriptenEnvHostFunction> = buildList {
    fnVoid("abort", listOf())
    fnVoid("__assert_fail", List(4) { I32_TYPE })
    fn("__syscall_faccessat", List(4) { I32_TYPE })
    fnVoid("_tzset_js", List(3) { I32_TYPE })
    fnVoid("_localtime_js", listOf(I64_TYPE, I32_TYPE))
    fn("emscripten_date_now", listOf(), F64_TYPE)
    fn("_emscripten_get_now_is_monotonic", listOf())
    fn("emscripten_get_now", listOf(), F64_TYPE)
    fn("emscripten_resize_heap", listOf(I32_TYPE))
    fn("__syscall_fchmod", listOf(I32_TYPE, I32_TYPE))
    fn("__syscall_chmod", listOf(I32_TYPE, I32_TYPE))
    fn("__syscall_fchown32", List(3) { I32_TYPE })
    fn("__syscall_fcntl64", List(3) { I32_TYPE })
    fn("__syscall_openat", List(4) { I32_TYPE })
    fn("__syscall_ioctl", List(3) { I32_TYPE })
    fn("__syscall_fstat64", listOf(I32_TYPE, I32_TYPE))
    fn("__syscall_stat64", listOf(I32_TYPE, I32_TYPE))
    fn("__syscall_newfstatat", List(4) { I32_TYPE })
    fn("__syscall_lstat64", listOf(I32_TYPE, I32_TYPE))
    fn("__syscall_ftruncate64", listOf(I32_TYPE, I64_TYPE))
    fn("__syscall_getcwd", listOf(I32_TYPE, I32_TYPE))
    fn("__syscall_mkdirat", List(3) { I32_TYPE })
    fn("_munmap_js", listOf(I32_TYPE, I32_TYPE, I32_TYPE, I32_TYPE, I32_TYPE, I64_TYPE))
    fn("_mmap_js", listOf(I32_TYPE, I32_TYPE, I32_TYPE, I32_TYPE, I64_TYPE, I32_TYPE, I32_TYPE))
    fn("__syscall_readlinkat", List(4) { I32_TYPE })
    fn("__syscall_rmdir", listOf(I32_TYPE))
    fn("__syscall_unlinkat", List(3) { I32_TYPE })
    fn("__syscall_utimensat", List(4) { I32_TYPE })
}

private fun MutableList<EmscriptenEnvHostFunction>.fn(
    name: String,
    paramTypes: List<Byte>,
    retType: Byte = I32_TYPE
) = add(EmscriptenEnvHostFunction(name, paramTypes.toByteArray(), byteArrayOf(retType)))

private fun MutableList<EmscriptenEnvHostFunction>.fnVoid(
    name: String,
    paramTypes: List<Byte>,
) = add(EmscriptenEnvHostFunction(name, paramTypes.toByteArray(), byteArrayOf()))

class EmscriptenEnvHostFunction(
    val name: String,
    val paramTypes: ByteArray,
    val retTypes: ByteArray,
)

fun createSqliteEnvModule(
    context: WasmContext,
    name: String = "env",
): WasmInstance {
    val envModule = WasmModule.create(name, null)
    if (context.contextOptions.supportMemory64()) {
        envModule.allocateExportMemory(
            initSize = 256,
            maxSize = Sizes.MAX_MEMORY_64_DECLARATION_SIZE,
            "memory",
            is64Bit = true,
            isShared = false,
        )
    } else {
        envModule.allocateExportMemory(
            initSize = 256,
            maxSize = 32768,
            "memory",
            is64Bit = false,
            isShared = false,
        )
    }

    val exportedFunctions: Map<String, Int> = envFunctions.associate {
        val f = envModule.declareExportedFunction(it)
        it.name to f.index()
    }

    val envInstance = context.readInstance(envModule)

    envInstance.setTarget(
        exportedFunctions.getValue("__syscall_rmdir"),
        SyscallRmdirNode(context.language(), envInstance).callTarget
    )

    listOf(
        "abort",
        "__assert_fail",
        "__syscall_chmod",
        "__syscall_faccessat",
        "__syscall_fchmod",
        "__syscall_fchown32",
        "__syscall_fcntl64",
        "__syscall_fstat64",
        "__syscall_ftruncate64",
        "__syscall_getcwd",
        "__syscall_ioctl",
        "__syscall_lstat64",
        "__syscall_mkdirat",
        "__syscall_newfstatat",
        "__syscall_openat",
        "__syscall_readlinkat",
        "__syscall_rmdir",
        "__syscall_stat64",
        "__syscall_unlinkat",
        "__syscall_utimensat",
        "_emscripten_get_now_is_monotonic",
        "_localtime_js",
        "_mmap_js",
        "_munmap_js",
        "_tzset_js",
        "emscripten_date_now",
        "emscripten_get_now",
        "emscripten_resize_heap",
    ).forEach { funcName ->
        envInstance.setTarget(
            exportedFunctions.getValue(funcName),
            NotImplementedNode(context.language(), envInstance, funcName).callTarget
        )
    }

    return envInstance
}

fun WasmModule.allocateExportMemory(
    initSize: Long,
    maxSize: Long,
    memoryName: String = "memory",
    is64Bit: Boolean,
    isShared: Boolean
) {
    val index = symbolTable().memoryCount()
    symbolTable().allocateMemory(index, initSize, maxSize, is64Bit, isShared, false)
    symbolTable().exportMemory(index, memoryName)
}

fun WasmModule.declareExportedFunction(params: EmscriptenEnvHostFunction): WasmFunction {
    val typeIdx = symbolTable().allocateFunctionType(
        params.paramTypes,
        params.retTypes,
        false
    )
    return symbolTable().declareExportedFunction(typeIdx, params.name)
}
