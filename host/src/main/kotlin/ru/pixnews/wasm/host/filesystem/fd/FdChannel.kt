package ru.pixnews.wasm.host.filesystem.fd

import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileChannel
import java.nio.file.Path
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.filesystem.SysException

class FdChannel(
    val fileSystem: FileSystem,
    val fd: Fd,
    val path: Path,
    val channel: FileChannel
)

var FdChannel.position: Long
    get() = try {
        channel.position()
    } catch (ce: ClosedChannelException) {
        throw SysException(Errno.BADF)
    } catch (ioe: IOException) {
        throw SysException(Errno.IO)
    }
    set(newPosition) = try {
        channel.position(newPosition)
        Unit
    } catch (ce: ClosedChannelException) {
        throw SysException(Errno.BADF)
    } catch (ioe: IOException) {
        throw SysException(Errno.IO)
    } catch (iae: IllegalArgumentException) {
        throw SysException(Errno.INVAL, "Negative new position")
    }