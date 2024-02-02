package ru.pixnews.wasm.sqlite3.chicory.sqlite3

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType.I32
import com.dylibso.chicory.wasm.types.ValueType.I64

class WasiSnapshotPreview1Builtins(
    moduleName: String = "wasi_snapshot_preview1"
) {
    private val i32 = listOf(I32)
    private val i32i32 = listOf(I32, I32)
    private val i32i32i32 = listOf(I32, I32, I32)
    private val i32i32i32i32 = listOf(I32, I32, I32, I32)
    private val i32i32i32i32i32 = List(5) { I32 }

    val argsSizesGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "args_sizes_get",
        i32i32,
        i32,
    )
    val argsGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "args_get",
        i32i32,
        i32,
    )
    val environSizesGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "environ_sizes_get",
        i32i32,
        i32,
    )
    val environGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "environ_get",
        i32i32,
        i32,
    )
    val clockTimeGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "clock_time_get",
        listOf(I32, I64, I32),
        i32,
    )
    val fdWrite: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_write",
        listOf(I32, I32, I32, I32),
        i32,
    )
    val fdRead: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_read",
        i32i32i32i32,
        i32,
    )
    val fdClose: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_close",
        i32,
        i32,
    )
    val fdSeek: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_seek",
        listOf(I32, I64, I32, I32),
        i32,
    )
    val fdFdstatGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_fdstat_get",
        i32i32,
        i32,
    )
    val fdFdstatSetFlags: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_fdstat_set_flags",
        i32i32,
        i32,
    )
    val fdPrestatGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_prestat_get",
        i32i32,
        i32,
    )
    val fdPrestatDirName: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_prestat_dir_name",
        i32i32i32,
        i32,
    )
    val fdSync: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_sync",
        i32,
        i32,
    )
    val fdFilestatGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "fd_filestat_get",
        i32i32,
        i32,
    )
    val pathOpen: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_open",
        listOf(I32, I32, I32, I32, I32, I64, I64, I32, I32),
        i32,
    )
    val pathCreateDirectory: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_create_directory",
        i32i32i32,
        i32,
    )
    val pathRemoveDirectory: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_remove_directory",
        i32i32i32,
        i32
    )
    val pathFilestatSetTimes: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_filestat_set_times",
        listOf(I32, I32, I32, I32, I64, I64, I32),
        i32
    )
    val pathLink: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_link",
        List(7) { I32 },
        i32
    )
    val pathRename: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_rename",
        List(6) { I32 },
        i32
    )
    val pathSymlink: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_symlink",
        i32i32i32i32i32,
        i32
    )
    val pathUnlinkFile: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_unlink_file",
        i32i32i32,
        i32
    )
    val pathReadlink: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_readlink",
        i32i32i32i32i32,
        i32
    )
    val pathFilestatGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "path_filestat_get",
        i32i32i32i32i32,
        i32
    )
    val schedYield: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "sched_yield",
        listOf(),
        i32
    )
    val randomGet: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            arrayOf(Value.i32(0))
        },
        moduleName,
        "random_get",
        i32i32,
        i32
    )

    val functions: List<HostFunction> = listOf(
        argsSizesGet,
        argsGet,
        environSizesGet,
        environGet,
        clockTimeGet,
        fdWrite,
        fdRead,
        fdClose,
        fdSeek,
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