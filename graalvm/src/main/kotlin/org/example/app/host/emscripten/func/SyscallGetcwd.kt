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
import ru.pixnews.wasm.host.memory.encodeToNullTerminatedByteArray
import ru.pixnews.wasm.host.memory.write
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.WasmPtr

class SyscallGetcwd(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "__syscall_getcwd",
    private val logger: Logger = Logger.getLogger(SyscallGetcwd::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return syscallGetcwd(
            args.asWasmPtr(0),
            args[1] as Int,
        )
    }

    @TruffleBoundary
    private fun syscallGetcwd(
        dst: WasmPtr<Byte>,
        size: Int
    ): Int {
        logger.finest { "getCwd(dst: $dst size: $size)" }
        if (size == 0) return -Errno.INVAL.code

        val path = host.fileSystem.getCwd()
        val pathBytes: ByteArray = path.encodeToNullTerminatedByteArray()

        if (size < pathBytes.size) {
            return -Errno.RANGE.code
        }
        memory.write(dst, pathBytes)

        return pathBytes.size
    }
}