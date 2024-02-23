package org.example.app.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
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

class SyscallFchown32(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "__syscall_fchown32",
    private val logger: Logger = Logger.getLogger(SyscallFchown32::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return syscallFchown32(
            args[0] as Int,
            args[1] as Int,
            args[2] as Int,
        )
    }

    @TruffleBoundary
    private fun syscallFchown32(
        fd: Int,
        owner: Int,
        group: Int
    ): Int = try {
        host.fileSystem.chown(Fd(fd), owner, group)
        Errno.SUCCESS.code
    } catch (e: SysException) {
        logger.finest { "chown($fd, $owner, $group): Error ${e.errNo}" }
        -e.errNo.code
    }
}