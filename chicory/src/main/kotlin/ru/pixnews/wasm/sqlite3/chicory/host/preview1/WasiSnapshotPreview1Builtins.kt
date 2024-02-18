package ru.pixnews.wasm.sqlite3.chicory.host.preview1

import com.dylibso.chicory.runtime.HostFunction
import java.time.Clock
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.ChicoryMemoryImpl
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.argsGet
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.argsSizesGet
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.clockTimeGet
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.environGet
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.environSizesGet
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdClose
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdFdstatGet
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdFdstatSetFlags
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdFilestatGet
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdPread
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdPrestatDirName
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdPrestatGet
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdPwrite
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdRead
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdSeek
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdSync
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.fdWrite
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.pathCreateDirectory
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.pathFilestatGet
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.pathFilestatSetTimes
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.pathLink
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.pathOpen
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.pathReadlink
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.pathRemoveDirectory
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.pathRename
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.pathSymlink
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.pathUnlinkFile
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.randomGet
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.func.schedYield

// https://github.com/WebAssembly/WASI/tree/main
class WasiSnapshotPreview1Builtins(
    memory: ChicoryMemoryImpl,
    fileSystem: FileSystem,
    argsProvider: () -> List<String> = ::emptyList,
    envProvider: () -> Map<String, String> = System::getenv,
    clock: Clock = Clock.systemDefaultZone(),
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
) {
    val functions: List<HostFunction> = listOf(
        argsGet(argsProvider),            // Not yet implemented
        argsSizesGet(argsProvider),       // Not yet implemented
        clockTimeGet(clock),
        environGet(memory, envProvider),
        environSizesGet(envProvider),
        fdClose(fileSystem),
        fdFdstatGet(fileSystem),          // Not yet implemented
        fdFdstatSetFlags(fileSystem),     // Not yet implemented
        fdFilestatGet(fileSystem),        // Not yet implemented
        fdPread(fileSystem),
        fdPrestatDirName(fileSystem),     // Not yet implemented
        fdPrestatGet(fileSystem),         // Not yet implemented
        fdPwrite(fileSystem),
        fdRead(fileSystem),
        fdSeek(fileSystem),
        fdSync(fileSystem),
        fdWrite(fileSystem),
        pathCreateDirectory(fileSystem),  // Not yet implemented
        pathFilestatGet(fileSystem),      // Not yet implemented
        pathFilestatSetTimes(fileSystem), // Not yet implemented
        pathLink(fileSystem),             // Not yet implemented
        pathOpen(fileSystem),             // Not yet implemented
        pathReadlink(fileSystem),         // Not yet implemented
        pathRemoveDirectory(fileSystem),  // Not yet implemented
        pathRename(fileSystem),           // Not yet implemented
        pathSymlink(fileSystem),          // Not yet implemented
        pathUnlinkFile(fileSystem),       // Not yet implemented
        randomGet(fileSystem),            // Not yet implemented
        schedYield(),                     // Not yet implemented
    )
}