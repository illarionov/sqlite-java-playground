package org.example.app.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Logger
import org.example.app.ext.asWasmPtr
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.include.sys.StructStat
import ru.pixnews.wasm.host.include.sys.pack
import ru.pixnews.wasm.host.memory.write
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.WasmPtr

class SyscallFstat64(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "__syscall_fstat64",
    private val logger: Logger = Logger.getLogger(SyscallFstat64::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return syscallFstat64(
            Fd(args[0] as Int),
            args.asWasmPtr(1)
        )
    }

    @TruffleBoundary
    private fun syscallFstat64(
        fd: Fd,
        dst: WasmPtr<StructStat>,
    ): Int = try {
        val stat = host.fileSystem.stat(fd).also {
            logger.finest { "fStat64($fd): OK $it" }
        }.pack()
        memory.write(dst, stat)
        Errno.SUCCESS.code
    } catch (e: SysException) {
        logger.finest { "fStat64(${fd}): Error ${e.errNo}" }
        -e.errNo.code
    }
}