package org.example.app.host

import java.time.Clock
import ru.pixnews.wasm.host.filesystem.FileSystem

class Host(
    val fileSystem: FileSystem,
    val clock: Clock,
)