package org.example.app.host.emscrypten

import org.example.app.host.BaseWasmRootNode
import org.example.app.host.Host
import org.example.app.host.HostFunction
import org.example.app.host.HostFunctionType
import org.example.app.host.emscrypten.func.NotImplementedNode
import org.example.app.host.emscrypten.func.syscallLstat64
import org.example.app.host.emscrypten.func.syscallStat64
import org.graalvm.wasm.SymbolTable
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmFunction
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.WasmType.F64_TYPE
import org.graalvm.wasm.WasmType.I32_TYPE
import org.graalvm.wasm.WasmType.I64_TYPE
import org.graalvm.wasm.constants.Sizes

internal const val ENV_MODULE_NAME = "env"

object EmscriptenEnvBindings {
    val envFunctions: List<HostFunction> = buildList {
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
        fn(
            name = "__syscall_stat64",
            paramTypes = listOf(I32_TYPE, I32_TYPE),
            retType = I32_TYPE,
            nodeFactory = ::syscallStat64
        )
        fn(
            name = "__syscall_lstat64",
            paramTypes = listOf(I32_TYPE, I32_TYPE),
            retType = I32_TYPE,
            nodeFactory = ::syscallLstat64
        )
        fn("__syscall_newfstatat", List(4) { I32_TYPE })
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

    private fun MutableList<HostFunction>.fn(
        name: String,
        paramTypes: List<Byte>,
        retType: Byte = I32_TYPE,
        nodeFactory: (
            language: WasmLanguage,
            instance: WasmInstance,
            host: Host,
        ) -> BaseWasmRootNode = { language, instance, _ -> NotImplementedNode(language, instance, name) }
    ) = add(HostFunction(name, paramTypes, listOf(retType), nodeFactory))

    private fun MutableList<HostFunction>.fnVoid(
        name: String,
        paramTypes: List<Byte>,
        nodeFactory: (
            language: WasmLanguage,
            instance: WasmInstance,
            host: Host,
        ) -> BaseWasmRootNode = { language, instance, _ -> NotImplementedNode(language, instance, name) }
    ) = add(HostFunction(name, paramTypes, emptyList(),  nodeFactory))

    fun setupEnvBindings(
        context: WasmContext,
        host: Host,
        name: String = ENV_MODULE_NAME,
    ): WasmInstance {
        val envModule = WasmModule.create(name, null)

        setupMemory(context, envModule)

        val functionTypes: Map<HostFunctionType, Int> = allocateFunctionTypes(envModule)
        val exportedFunctions: Map<String, WasmFunction> = declareExportedFunctions(envModule, functionTypes)
        val envInstance: WasmInstance = context.readInstance(envModule)

        envFunctions.forEach { f: HostFunction ->
            val node = f.nodeFactory(context.language(), envInstance, host)
            val exportedIndex = exportedFunctions.getValue(f.name).index()
            envInstance.setTarget(exportedIndex, node.callTarget)
        }

        return envInstance
    }

    private fun setupMemory(
        context: WasmContext,
        envModule: WasmModule,
    ) {
        val minSize = 256L
        val maxSize: Long
        val is64Bit: Boolean
        if (context.contextOptions.supportMemory64()) {
            maxSize = Sizes.MAX_MEMORY_64_DECLARATION_SIZE
            is64Bit = true
        } else {
            maxSize = 32768
            is64Bit = false
        }

        envModule.symbolTable().apply {
            val memoryIndex = memoryCount()
            allocateMemory(memoryIndex, minSize, maxSize, is64Bit, false, false)
            exportMemory(memoryIndex, "memory")
        }
    }

    private fun allocateFunctionTypes(
        symbolTable: SymbolTable,
        functions: List<HostFunction> = envFunctions
    ): Map<HostFunctionType, Int> {
        val functionTypeMap: MutableMap<HostFunctionType, Int> = mutableMapOf()
        functions.forEach { f ->
            val type: HostFunctionType = f.type
            functionTypeMap.getOrPut(type) {
                val typeIdx = symbolTable.allocateFunctionType(
                    type.params.toByteArray(),
                    type.returnTypes.toByteArray(),
                    false
                )
                typeIdx
            }
        }
        return functionTypeMap
    }

    private fun declareExportedFunctions(
        symbolTable: SymbolTable,
        functionTypes: Map<HostFunctionType, Int>,
        functions: List<HostFunction> = envFunctions,
    ): Map<String, WasmFunction> {
        return functions.associate { f ->
            val typeIdx = functionTypes.getValue(f.type)
            val functionIdx = symbolTable.declareExportedFunction(typeIdx, f.name)
            f.name to functionIdx
        }
    }
}