package org.example.app.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Logger
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd

class SyscallFtruncate64(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "__syscall_ftruncate64",
    private val logger: Logger = Logger.getLogger(SyscallFtruncate64::class.qualifiedName)
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return syscallFtruncate64(
            args[0] as Int,
            (args[1] as Long).toULong(),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun syscallFtruncate64(
        fd: Int,
        length: ULong,
    ): Int = try {
        host.fileSystem.ftruncate(Fd(fd), length)
        Errno.SUCCESS.code
    } catch (e: SysException) {
        logger.finest { "ftruncate64($fd, $length): Error ${e.errNo}" }
        -e.errNo.code
    }
}