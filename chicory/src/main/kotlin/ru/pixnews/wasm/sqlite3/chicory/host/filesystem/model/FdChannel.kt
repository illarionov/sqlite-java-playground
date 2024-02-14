package ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model

import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileChannel
import java.nio.file.Path
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.SysException
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Fd

class FdChannel(
    val fileSystem: FileSystem,
    val fd: Fd,
    val path: Path,
    val channel: FileChannel
)

val FdChannel.position: Long
    get() = try {
        channel.position()
    } catch (ce: ClosedChannelException) {
        throw SysException(Errno.BADF)
    } catch (ioe: IOException) {
        throw SysException(Errno.IO)
    }