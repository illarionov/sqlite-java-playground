package ru.pixnews.wasm.sqlite3.chicory.host.emscrypten

import com.dylibso.chicory.runtime.HostFunction
import java.time.Clock
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.emscriptenDateNow
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.emscriptenGetNow
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.emscriptenGetNowIsMonotonic
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.emscriptenResizeHeap
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.localtimeJs
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.mmapJs
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.munmapJs
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallChmod
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallFaccessat
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallFchmod
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallFchown32
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallFcntl64
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallFstat64
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallFtruncate64
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallGetcwd
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallIoctl
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallLstat64
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallMkdirat
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallNewfstatat
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallOpenat
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallReadlinkat
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallRmdir
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallStat64
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallUnlinkat
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.syscallUtimensat
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.tzsetJs

internal const val ENV_MODULE_NAME = "env"

class EmscriptenEnvBindings(
    filesystem: FileSystem,
    clock: Clock = Clock.systemDefaultZone(),
    moduleName: String = ENV_MODULE_NAME,
) {
    val functions: List<HostFunction> = listOf(
        ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.abortFunc(),
        ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func.assertFail(),
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