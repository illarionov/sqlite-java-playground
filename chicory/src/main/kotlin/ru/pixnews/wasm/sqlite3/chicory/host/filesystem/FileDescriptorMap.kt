package ru.pixnews.wasm.sqlite3.chicory.host.filesystem

import java.nio.channels.FileChannel
import java.nio.file.Path
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.FdChannel
import ru.pixnews.wasm.sqlite3.host.filesystem.SysException

internal class FileDescriptorMap(
    private val fileSystem: FileSystem,
) {
    private val fds: MutableMap<Fd, FdChannel> = mutableMapOf()

    fun add(
        path: Path,
        channel: FileChannel
    ): FdChannel {
        val fd = getFreeFd()
        return FdChannel(
            fileSystem = fileSystem,
            fd = fd,
            path = path,
            channel = channel,
        ).also {
            val old = fds.putIfAbsent(fd, it)
            require(old == null) { "File descriptor $fd already been allocated" }
        }
    }

    public fun remove(fd: Fd): FdChannel {
        return fds.remove(fd) ?: throw SysException(Errno.BADF, "Trying to remove already disposed file descriptor")
    }

    public fun get(
        fd: Fd
    ): FdChannel? = fds[fd]

    @Throws(SysException::class)
    private fun getFreeFd(): Fd {
        for (no in MIN_FD .. MAX_FD) {
            if (!fds.containsKey(Fd(no))) {
                return Fd(no)
            }
        }
        throw SysException(Errno.NFILE)
    }

    companion object {
        const val MIN_FD = 4
        const val MAX_FD = 1024
    }
}
