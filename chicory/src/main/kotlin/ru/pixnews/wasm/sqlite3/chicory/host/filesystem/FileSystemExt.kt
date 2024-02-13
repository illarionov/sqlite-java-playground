package ru.pixnews.wasm.sqlite3.chicory.host.filesystem

import java.nio.file.Path
import kotlin.io.path.pathString
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno

internal const val AT_FDCWD = -100

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
            getPathByFd(dirFd)
        } catch (e: SysException) {
            throw SysException(Errno.BADF, "File descriptor ${dirFd} is not open")
        }
    }

    if (path.pathString.isEmpty() && !allowEmpty) {
        throw SysException(Errno.NOENT)
    }
    return root.resolve(path)
}
