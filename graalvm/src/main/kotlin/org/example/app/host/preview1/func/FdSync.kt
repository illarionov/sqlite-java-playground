package org.example.app.host.preview1.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Level
import java.util.logging.Logger
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd

class FdSync(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "fd_sync",
    private val logger: Logger = Logger.getLogger(FdSync::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return fdSync(Fd(args[0] as Int))
    }

    @TruffleBoundary
    private fun fdSync(
        fd: Fd
    ): Int {
        return try {
            host.fileSystem.sync(fd, metadata = true)
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "sync() error" }
            e.errNo
        }.code
    }
}