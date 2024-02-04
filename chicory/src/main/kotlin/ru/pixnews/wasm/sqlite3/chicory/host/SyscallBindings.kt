package ru.pixnews.wasm.sqlite3.chicory.host

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes

class SyscallBindings(
    moduleName: String = "env"
) {
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
    val syscallGetcwd: HostFunction = HostFunction(
        { instance: Instance, args: Array<Value> ->
            TODO()
            arrayOf(Value.i32(0))
        },
        moduleName,
        "__syscall_getcwd",
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
        syscallFaccessat,
        syscallFchmod,
        syscallChmod,
        syscallFchown32,
        syscallFtruncate64,
        syscallGetcwd,
        syscallReadlinkat,
        syscallRmdir,
        syscallUnlinkat,
        syscallUtimensat,
    )
}