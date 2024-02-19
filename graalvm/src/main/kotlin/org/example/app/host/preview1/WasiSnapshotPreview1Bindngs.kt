package org.example.app.host.preview1

import org.example.app.ext.allocateFunctionTypes
import org.example.app.ext.declareExportedFunctions
import org.example.app.host.Host
import org.example.app.host.HostFunction
import org.example.app.host.HostFunctionType
import org.example.app.host.fn
import org.graalvm.wasm.SymbolTable
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmFunction
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.WasmType.I64_TYPE
import org.graalvm.wasm.constants.Sizes
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I64

object WasiSnapshotPreview1Bindngs {
    internal const val WASI_SNAPSHOT_PREVIEW1 = "wasi_snapshot_preview1"

    private val preview1Functions: List<HostFunction> = buildList {
        fn(
            name = "args_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "args_sizes_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "clock_time_get",
            paramTypes = listOf(I32, I64, I32),
            // nodeFactory =
        )
        fn(
            name = "environ_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "environ_sizes_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_close",
            paramTypes = listOf(I32),
            // nodeFactory =
        )
        fn(
            name = "fd_fdstat_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_fdstat_set_flags",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_filestat_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_prestat_dir_name",
            paramTypes = listOf(I32, I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_prestat_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_read",
            paramTypes = List(4) { I32 },
            // nodeFactory =
        )
        fn(
            name = "fd_pread",
            paramTypes = List(4) { I32 },
            // nodeFactory =
        )
        fn(
            name = "fd_seek",
            paramTypes = listOf(I32, I64, I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_sync",
            paramTypes = listOf(I32),
            // nodeFactory =
        )
        fn(
            name = "fd_write",
            paramTypes = List(4) { I32 },
            // nodeFactory =
        )
        fn(
            name = "fd_pwrite",
            paramTypes = List(4) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_create_directory",
            paramTypes = List(3) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_filestat_get",
            paramTypes = List(5) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_filestat_set_times",
            paramTypes = listOf(I32, I32, I32, I32, I64, I64, I32),
            // nodeFactory =
        )
        fn(
            name = "path_link",
            paramTypes = List(7) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_open",
            paramTypes = listOf(
                I32,
                I32,
                I32,
                I32,
                I32,
                I64,
                I64,
                I32,
                I32,
            ),
            // nodeFactory =
        )
        fn(
            name = "path_readlink",
            paramTypes = List(5) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_remove_directory",
            paramTypes = listOf(I32, I32, I32),
            // nodeFactory =
        )
        fn(
            name = "path_rename",
            paramTypes = List(6) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_symlink",
            paramTypes = List(5) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_unlink_file",
            paramTypes = listOf(I32, I32, I32),
            // nodeFactory =
        )
        fn(
            name = "random_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "sched_yield",
            paramTypes = listOf(),
            // nodeFactory =
        )
    }

    fun setupWasiSnapshotPreview1Bindngs(
        context: WasmContext,
        host: Host,
        name: String = WASI_SNAPSHOT_PREVIEW1
    ): WasmInstance {
        val wasiModule = WasmModule.create(name, null)
        importMemory(wasiModule, context)

        val functionTypes: Map<HostFunctionType, Int> = allocateFunctionTypes(
            wasiModule,
            preview1Functions
        )
        val exportedFunctions: Map<String, WasmFunction> = declareExportedFunctions(
            wasiModule,
            functionTypes,
            preview1Functions
        )
        val envInstance: WasmInstance = context.readInstance(wasiModule)

        preview1Functions.forEach { f: HostFunction ->
            val node = f.nodeFactory(context.language(), envInstance, host, f.name)
            val exportedIndex = exportedFunctions.getValue(f.name).index()
            envInstance.setTarget(exportedIndex, node.callTarget)
        }

        return envInstance
    }

    private fun importMemory(
        symbolTable: SymbolTable,
        context: WasmContext
    ) {
        val minSize = 0L
        val maxSize: Long
        val is64Bit: Boolean
        if (context.contextOptions.supportMemory64()) {
            maxSize = Sizes.MAX_MEMORY_64_DECLARATION_SIZE
            is64Bit = true
        } else {
            maxSize = 32768
            is64Bit = false
        }

        val index = symbolTable.memoryCount()
        symbolTable.importMemory(
            "env",
            "memory",
            index,
            minSize,
            maxSize,
            is64Bit,
            false,
            false
        )
    }

}