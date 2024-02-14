package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func.fdSeek
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func.environGet
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func.environSizesGet
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func.fdClose
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func.fdRead

// https://github.com/WebAssembly/WASI/tree/main
class WasiSnapshotPreview1Builtins(
    fileSystem: FileSystem,
    moduleName: String = "wasi_snapshot_preview1",
) {
    val argsSizesGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "args_sizes_get",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val argsGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "args_get",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )

    val clockTimeGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "clock_time_get",
        listOf(ValueType.I32, ValueType.I64, ValueType.I32),
        ParamTypes.i32,
    )
    val fdWrite: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_write",
        listOf(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
        ParamTypes.i32,
    )
    val fdFdstatGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_fdstat_get",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val fdFdstatSetFlags: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_fdstat_set_flags",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val fdPrestatGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_prestat_get",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val fdPrestatDirName: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_prestat_dir_name",
        ParamTypes.i32i32i32,
        ParamTypes.i32,
    )
    val fdSync: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_sync",
        ParamTypes.i32,
        ParamTypes.i32,
    )
    val fdFilestatGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_filestat_get",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val pathOpen: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_open",
        listOf(
            ValueType.I32,
            ValueType.I32,
            ValueType.I32,
            ValueType.I32,
            ValueType.I32,
            ValueType.I64,
            ValueType.I64,
            ValueType.I32,
            ValueType.I32
        ),
        ParamTypes.i32,
    )
    val pathCreateDirectory: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_create_directory",
        ParamTypes.i32i32i32,
        ParamTypes.i32,
    )
    val pathRemoveDirectory: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_remove_directory",
        ParamTypes.i32i32i32,
        ParamTypes.i32
    )
    val pathFilestatSetTimes: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_filestat_set_times",
        listOf(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I64, ValueType.I64, ValueType.I32),
        ParamTypes.i32
    )
    val pathLink: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_link",
        List(7) { ValueType.I32 },
        ParamTypes.i32
    )
    val pathRename: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_rename",
        List(6) { ValueType.I32 },
        ParamTypes.i32
    )
    val pathSymlink: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_symlink",
        ParamTypes.i32i32i32i32i32,
        ParamTypes.i32
    )
    val pathUnlinkFile: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_unlink_file",
        ParamTypes.i32i32i32,
        ParamTypes.i32
    )
    val pathReadlink: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_readlink",
        ParamTypes.i32i32i32i32i32,
        ParamTypes.i32
    )
    val pathFilestatGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_filestat_get",
        ParamTypes.i32i32i32i32i32,
        ParamTypes.i32
    )
    val schedYield: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "sched_yield",
        listOf(),
        ParamTypes.i32
    )
    val randomGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "random_get",
        ParamTypes.i32i32,
        ParamTypes.i32
    )

    val functions: List<HostFunction> = listOf(
        argsSizesGet,
        argsGet,
        environSizesGet(),
        environGet(),
        clockTimeGet,
        fdWrite,
        fdRead(fileSystem),
        fdClose(fileSystem),
        fdSeek(fileSystem),
        fdFdstatGet,
        fdFdstatSetFlags,
        fdPrestatGet,
        fdPrestatDirName,
        fdSync,
        fdFilestatGet,
        pathOpen,
        pathCreateDirectory,
        pathRemoveDirectory,
        pathFilestatSetTimes,
        pathLink,
        pathRename,
        pathSymlink,
        pathUnlinkFile,
        pathReadlink,
        pathFilestatGet,
        schedYield,
        randomGet,
    )
}