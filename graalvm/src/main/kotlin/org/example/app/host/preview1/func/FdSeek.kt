package org.example.app.host.preview1.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Level
import java.util.logging.Logger
import org.example.app.ext.asWasmPtr
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.host.filesystem.fd.position
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.wasi.preview1.type.Whence

class FdSeek(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "fd_seek",
    private val logger: Logger = Logger.getLogger(FdSeek::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return fdSeek(
            Fd(args[0] as Int),
            args[1] as Long,
            args[2] as Int,
            args.asWasmPtr(3),
        )
    }

    @TruffleBoundary
    private fun fdSeek(
        fd: Fd,
        offset: Long,
        whenceInt: Int,
        pNewOffset: WasmPtr<Long>,
    ): Int {
        val whence = Whence.fromIdOrNull(whenceInt) ?: return Errno.INVAL.code
        return try {
            val channel: FdChannel = host.fileSystem.getStreamByFd(fd)
            host.fileSystem.seek(channel, offset, whence)

            val newPosition = channel.position

            memory.writeI64(pNewOffset, newPosition)

            Errno.SUCCESS
        } catch (sysException: SysException) {
            logger.log(Level.INFO, sysException) { "fdSeek() error" }
            sysException.errNo
        }.code
    }
}