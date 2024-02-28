package org.example.app.host

import java.time.Clock
import ru.pixnews.wasm.host.filesystem.FileSystem

class Host(
    val systemEnvProvider: () -> Map<String, String> = System::getenv,
    val commandArgsProvider: () -> List<String> = ::emptyList,
    val fileSystem: FileSystem = FileSystem(),
    val clock: Clock = Clock.systemDefaultZone(),

)