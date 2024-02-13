package ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model

import java.nio.channels.FileChannel
import java.nio.file.Path
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem

class FdChannel(
    val fileSystem: FileSystem,
    val fd: Int,
    val path: Path,
    val channel: FileChannel
)