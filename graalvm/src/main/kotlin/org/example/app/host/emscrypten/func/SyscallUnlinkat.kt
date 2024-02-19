package org.example.app.host.emscrypten.func

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
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

class SyscallUnlinkat(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "__syscall_unlinkat",
    private val logger: Logger = Logger.getLogger(SyscallUnlinkat::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return syscallUnlinkat(
            args[0] as Int,
            args[1] as WasmPtr,
            (args[2] as Int).toUInt(),
        )
    }

    @TruffleBoundary
    private fun syscallUnlinkat(
        dirfd: Int,
        pathnamePtr: WasmPtr,
        flags: UInt,
    ): Int {
        val errNo = try {
            val path = memory.readNullTerminatedString(pathnamePtr)
            host.fileSystem.unlinkAt(dirfd, path, flags)
            Errno.SUCCESS
        } catch (e: SysException) {
            e.errNo
        }
        return -errNo.code
    }
}