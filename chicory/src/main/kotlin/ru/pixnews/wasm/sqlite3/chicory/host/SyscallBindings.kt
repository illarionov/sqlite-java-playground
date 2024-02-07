package ru.pixnews.wasm.sqlite3.chicory.host

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallGetcwd

internal const val ENV_MODULE_NAME = "env"

class SyscallBindings(
    moduleName: String = ENV_MODULE_NAME
) {
    val filesystem = FileSystem()

    val assertFail: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "__assert_fail",
        ParamTypes.i32i32i32i32,
        listOf(),
    )
    val syscallFaccessat: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "__syscall_faccessat",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val syscallFchmod: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "__syscall_fchmod",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val syscallChmod: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "__syscall_chmod",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val syscallFchown32: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "__syscall_fchown32",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val syscallFtruncate64: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "__syscall_ftruncate64",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val syscallReadlinkat: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "__syscall_readlinkat",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val syscallRmdir: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "__syscall_rmdir",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val syscallUnlinkat: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "__syscall_unlinkat",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )
    val syscallUtimensat: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "__syscall_utimensat",
        ParamTypes.i32i32,
        ParamTypes.i32,
    )

    val functions: List<HostFunction> = listOf(
        assertFail,
        syscallFaccessat,
        syscallFchmod,
        syscallChmod,
        syscallFchown32,
        syscallFtruncate64,
        syscallGetcwd(filesystem),
        syscallReadlinkat,
        syscallRmdir,
        syscallUnlinkat,
        syscallUtimensat,
    )
}