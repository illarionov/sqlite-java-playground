package ru.pixnews.wasm.sqlite3.chicory.host.filesystem

import java.nio.channels.FileChannel
import java.nio.file.Path
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.FdChannel
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno

internal class FileDescriptorMap(
    private val fileSystem: FileSystem,
) {
    private val fds: MutableMap<Int, FdChannel> = mutableMapOf()

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

    public fun remove(fd: Int) {
        val old = fds.remove(fd)
        require (old == null) { "Trying to remove already disposed file descriptor" }
    }

    public fun get(
        fd: Int
    ): FdChannel? = fds[fd]



    @Throws(SysException::class)
    private fun getFreeFd(): Int {
        for (no in MIN_FD .. MAX_FD) {
            if (!fds.containsKey(no)) {
                return no
            }
        }
        throw SysException(Errno.NFILE)
    }

    companion object {
        const val MIN_FD = 4
        const val MAX_FD = 1024
    }
}
