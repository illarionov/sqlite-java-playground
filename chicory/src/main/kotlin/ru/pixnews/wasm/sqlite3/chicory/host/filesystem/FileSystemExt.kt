package ru.pixnews.wasm.sqlite3.chicory.host.filesystem

import java.nio.file.Path
import kotlin.io.path.pathString
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.Fcntl.AT_FDCWD
import ru.pixnews.wasm.sqlite3.host.filesystem.SysException

internal fun FileSystem.resolveAbsolutePath(
    dirFd: Int,
    path: String,
    allowEmpty: Boolean = false,
): Path {
    return resolveAbsolutePath(dirFd, javaFs.getPath(path), allowEmpty)
}

internal fun FileSystem.resolveAbsolutePath(
    dirFd: Int,
    path: Path,
    allowEmpty: Boolean = false,
): Path {
    if (path.isAbsolute) return path

    val root: Path = if (dirFd == AT_FDCWD) {
        getCwdPath()
    } else {
        try {
            getPathByFd(Fd(dirFd))
        } catch (e: SysException) {
            throw SysException(Errno.BADF, "File descriptor $dirFd is not open")
        }
    }

    if (path.pathString.isEmpty() && !allowEmpty) {
        throw SysException(Errno.NOENT)
    }
    return root.resolve(path)
}
