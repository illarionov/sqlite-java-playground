package ru.pixnews.wasm.sqlite3.chicory.host

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.time.Clock
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallFstat64
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallGetcwd
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallLstat64
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallOpenat
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallStat64

internal const val ENV_MODULE_NAME = "env"

class SyscallBindings(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
) {
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
        HostFunction(
            { _: Instance, _: Array<Value> ->
                error("native code called abort()")
            },
            moduleName,
            "abort",
            listOf(),
            listOf(),
        ),
        HostFunction(
            { _: Instance, _: Array<Value> ->
                val millis = Clock.systemDefaultZone().millis()
                arrayOf(Value.fromDouble(millis.toDouble()))
            },
            moduleName,
            "emscripten_date_now",
            listOf(),
            listOf(ValueType.F64),
        ),
        HostFunction(
            { _: Instance, _: Array<Value> -> arrayOf(Value.i32(1)) },
            moduleName,
            "_emscripten_get_now_is_monotonic",
            listOf(),
            ParamTypes.i32,
        ),
        HostFunction(
            { _: Instance, _: Array<Value> ->
                val ts = System.nanoTime() / 1_000_000.0
                arrayOf(Value.fromDouble(ts))
            },
            moduleName,
            "emscripten_get_now",
            listOf(),
            listOf(ValueType.F64),
        ),

        HostFunction(
            { instance: Instance, args: Array<Value> -> TODO() },
            moduleName,
            "__syscall_fcntl64",
            ParamTypes.i32i32i32,
            ParamTypes.i32,
        ),
        HostFunction(
            { instance: Instance, args: Array<Value> -> TODO() },
            moduleName,
            "__syscall_ioctl",
            ParamTypes.i32i32i32,
            ParamTypes.i32,
        ),

        HostFunction(
            { instance: Instance, args: Array<Value> -> TODO() },
            moduleName,
            "__syscall_newfstatat",
            ParamTypes.i32i32i32i32,
            ParamTypes.i32,
        ),
        HostFunction(
            { instance: Instance, args: Array<Value> -> TODO() },
            moduleName,
            "__syscall_mkdirat",
            ParamTypes.i32i32i32,
            ParamTypes.i32,
        ),
        HostFunction(
            { instance: Instance, args: Array<Value> -> TODO() },
            moduleName,
            "_localtime_js",
            listOf(ValueType.I64, ValueType.I32),
            listOf(),
        ),
        HostFunction(
            { instance: Instance, args: Array<Value> -> TODO() },
            moduleName,
            "_munmap_js",
            listOf(
                ValueType.I32,
                ValueType.I32,
                ValueType.I32,
                ValueType.I32,
                ValueType.I32,
                ValueType.I64,
            ),
            ParamTypes.i32,
        ),
        HostFunction(
            { instance: Instance, args: Array<Value> -> TODO() },
            moduleName,
            "_mmap_js",
            listOf(
                ValueType.I32,
                ValueType.I32,
                ValueType.I32,
                ValueType.I32,
                ValueType.I64,
                ValueType.I32,
                ValueType.I32,
            ),
            ParamTypes.i32,
        ),
        HostFunction(
            { instance: Instance, args: Array<Value> -> TODO() },
            moduleName,
            "_tzset_js",
            ParamTypes.i32i32i32,
            listOf(),
        ),
        HostFunction(
            { instance: Instance, args: Array<Value> -> TODO() },
            moduleName,
            "emscripten_resize_heap",
            ParamTypes.i32,
            ParamTypes.i32,
        ),
        assertFail,
        syscallFaccessat,
        syscallFchmod,
        syscallChmod,
        syscallFchown32,
        syscallFtruncate64,
        syscallOpenat(filesystem),
        syscallStat64(filesystem),
        syscallFstat64(filesystem),
        syscallLstat64(filesystem),
        syscallGetcwd(filesystem),
        syscallReadlinkat,
        syscallRmdir,
        syscallUnlinkat,
        syscallUtimensat,
    )
}