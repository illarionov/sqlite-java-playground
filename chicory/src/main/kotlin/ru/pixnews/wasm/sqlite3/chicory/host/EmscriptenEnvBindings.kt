package ru.pixnews.wasm.sqlite3.chicory.host

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.time.Clock
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.func.abortFunc
import ru.pixnews.wasm.sqlite3.chicory.host.func.assertFail
import ru.pixnews.wasm.sqlite3.chicory.host.func.emscriptenDateNow
import ru.pixnews.wasm.sqlite3.chicory.host.func.emscriptenGetNow
import ru.pixnews.wasm.sqlite3.chicory.host.func.emscriptenGetNowIsMonotonic
import ru.pixnews.wasm.sqlite3.chicory.host.func.emscriptenResizeHeap
import ru.pixnews.wasm.sqlite3.chicory.host.func.localtimeJs
import ru.pixnews.wasm.sqlite3.chicory.host.func.mmapJs
import ru.pixnews.wasm.sqlite3.chicory.host.func.munmapJs
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallChmod
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallFaccessat
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallFchmod
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallFchown32
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallFcntl64
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallFstat64
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallFtruncate64
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallGetcwd
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallIoctl
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallLstat64
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallMkdirat
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallNewfstatat
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallOpenat
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallReadlinkat
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallRmdir
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallStat64
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallUnlinkat
import ru.pixnews.wasm.sqlite3.chicory.host.func.syscallUtimensat
import ru.pixnews.wasm.sqlite3.chicory.host.func.tzsetJs

internal const val ENV_MODULE_NAME = "env"

class EmscriptenEnvBindings(
    filesystem: FileSystem,
    clock: Clock = Clock.systemDefaultZone(),
    moduleName: String = ENV_MODULE_NAME,
) {
    val functions: List<HostFunction> = listOf(
        abortFunc(),
        assertFail(),
        emscriptenDateNow(clock),
        emscriptenGetNow(),
        emscriptenGetNowIsMonotonic(),
        emscriptenResizeHeap(),
        localtimeJs(),                  // Not yet implemented
        mmapJs(filesystem),             // Not yet implemented
        munmapJs(filesystem),           // Not yet implemented
        syscallChmod(filesystem),       // Not yet implemented
        syscallFaccessat(filesystem),   // Not yet implemented
        syscallFchmod(filesystem),      // Not yet implemented
        syscallFchown32(filesystem),
        syscallFcntl64(filesystem),     // Not yet implemented
        syscallFstat64(filesystem),
        syscallFtruncate64(filesystem), // Not yet implemented
        syscallGetcwd(filesystem),
        syscallIoctl(filesystem),       // Not yet implemented
        syscallLstat64(filesystem),
        syscallMkdirat(filesystem),     // Not yet implemented
        syscallNewfstatat(filesystem),  // Not yet implemented
        syscallOpenat(filesystem),
        syscallReadlinkat(filesystem),  // Not yet implemented
        syscallRmdir(filesystem),       // Not yet implemented
        syscallStat64(filesystem),
        syscallUnlinkat(filesystem),
        syscallUtimensat(filesystem),   // Not yet implemented
        tzsetJs(),                      // Not yet implemented
    )
}