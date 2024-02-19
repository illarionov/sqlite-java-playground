package org.example.app.host

import java.time.Clock
import ru.pixnews.wasm.host.filesystem.FileSystem

class Host(
    val systemEnvProvider: () -> Map<String, String>,
    val commandArgsProvider: () -> List<String>,
    val fileSystem: FileSystem,
    val clock: Clock,
)