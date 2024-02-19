package org.example.app.host.emscrypten

import org.example.app.ext.allocateFunctionTypes
import org.example.app.ext.declareExportedFunctions
import org.example.app.host.Host
import org.example.app.host.HostFunction
import org.example.app.host.HostFunctionType
import org.example.app.host.emscrypten.func.Abort
import org.example.app.host.emscrypten.func.AssertFail
import org.example.app.host.emscrypten.func.EmscriptenDateNow
import org.example.app.host.emscrypten.func.EmscriptenGetNow
import org.example.app.host.emscrypten.func.EmscriptenGetNowIsMonotonic
import org.example.app.host.emscrypten.func.EmscriptenResizeHeap
import org.example.app.host.emscrypten.func.SyscallFchown32
import org.example.app.host.emscrypten.func.SyscallFstat64
import org.example.app.host.emscrypten.func.SyscallGetcwd
import org.example.app.host.emscrypten.func.SyscallOpenat
import org.example.app.host.emscrypten.func.SyscallUnlinkat
import org.example.app.host.emscrypten.func.syscallLstat64
import org.example.app.host.emscrypten.func.syscallStat64
import org.example.app.host.fn
import org.example.app.host.fnVoid
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmFunction
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.constants.Sizes
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.F64
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I64

object EmscriptenEnvBindings {
    private const val ENV_MODULE_NAME = "env"

    private val envFunctions: List<HostFunction> = buildList {
        fnVoid(
            name = "abort",
            paramTypes = listOf(),
            nodeFactory = { language, instance, _, functionName -> Abort(language, instance, functionName) }
        )
        fnVoid(
            name = "__assert_fail",
            paramTypes = List(4) { I32 },
            nodeFactory = ::AssertFail
        )
        fn(
            name = "emscripten_date_now",
            paramTypes = listOf(),
            retType = F64,
            nodeFactory = ::EmscriptenDateNow
        )
        fn(
            name = "emscripten_get_now",
            paramTypes = listOf(),
            retType = F64,
            nodeFactory = ::EmscriptenGetNow
        )
        fn(
            name = "_emscripten_get_now_is_monotonic",
            paramTypes = listOf(),
            retType = I32,
            nodeFactory = ::EmscriptenGetNowIsMonotonic
        )
        fn(
            name = "emscripten_resize_heap",
            paramTypes = listOf(I32),
            retType = I32,
            nodeFactory = ::EmscriptenResizeHeap
        )
        fnVoid("_localtime_js", listOf(I64, I32))
        fn("_mmap_js", listOf(I32, I32, I32, I32, I64, I32, I32))
        fn("_munmap_js", listOf(I32, I32, I32, I32, I32, I64))
        //fnVoid("_localtime_js", listOf(I32, I32))
        //fn("_mmap_js", listOf(I32, I32, I32, I32, I32, I32, I32))
        //fn("_munmap_js", listOf(I32, I32, I32, I32, I32, I32))
        fn("__syscall_chmod", listOf(I32, I32))
        fn("__syscall_faccessat", List(4) { I32 })
        fn("__syscall_fchmod", listOf(I32, I32))
        fn(
            name = "__syscall_fchown32",
            paramTypes = List(3) { I32 },
            retType = I32,
            nodeFactory = ::SyscallFchown32
        )
        fn("__syscall_fcntl64", List(3) { I32 })
        fn("__syscall_fstat64", listOf(I32, I32), I32, ::SyscallFstat64)
        fn("__syscall_ftruncate64", listOf(I32, I64))
        fn(
            name = "__syscall_getcwd",
            paramTypes = listOf(I32, I32),
            retType = I32,
            nodeFactory = ::SyscallGetcwd
        )
        fn("__syscall_ioctl", List(3) { I32 })
        fn("__syscall_mkdirat", List(3) { I32 })
        fn("__syscall_newfstatat", List(4) { I32 })
        fn(
            name = "__syscall_openat",
            paramTypes = List(4) { I32 },
            retType = I32,
            nodeFactory = ::SyscallOpenat
        )
        fn("__syscall_readlinkat", List(4) { I32 })
        fn("__syscall_rmdir", listOf(I32))
        fn(
            name = "__syscall_stat64",
            paramTypes = listOf(I32, I32),
            retType = I32,
            nodeFactory = ::syscallStat64
        )
        fn(
            name = "__syscall_lstat64",
            paramTypes = listOf(I32, I32),
            retType = I32,
            nodeFactory = ::syscallLstat64
        )
        fn(
            name = "__syscall_unlinkat",
            paramTypes = List(3) { I32 },
            retType = I32,
            nodeFactory = ::SyscallUnlinkat
        )
        fn("__syscall_utimensat", List(4) { I32 })
        fnVoid("_tzset_js", List(3) { I32 })
    }

    fun setupEnvBindings(
        context: WasmContext,
        host: Host,
        name: String = ENV_MODULE_NAME,
    ): WasmInstance {
        val envModule = WasmModule.create(name, null)

        setupMemory(context, envModule)

        val functionTypes: Map<HostFunctionType, Int> = allocateFunctionTypes(envModule, envFunctions)
        val exportedFunctions: Map<String, WasmFunction> = declareExportedFunctions(
            envModule,
            functionTypes,
            envFunctions
        )
        val envInstance: WasmInstance = context.readInstance(envModule)

        envFunctions.forEach { f: HostFunction ->
            val node = f.nodeFactory(context.language(), envInstance, host, f.name)
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
}