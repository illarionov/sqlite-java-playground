package org.example.app.host.emscrypten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import java.nio.file.Path
import java.util.logging.Logger
import org.example.app.host.BaseWasmRootNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.filesystem.resolveAbsolutePath
import ru.pixnews.wasm.host.include.Fcntl
import ru.pixnews.wasm.host.include.oMaskToString
import ru.pixnews.wasm.host.include.sMaskToString
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

class SyscallOpenat(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "__syscall_openat",
    private val logger: Logger = Logger.getLogger(SyscallOpenat::class.qualifiedName)
): BaseWasmRootNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        val mode = if (args.lastIndex == 3) {
            memory.readI32(args[3] as WasmPtr).toUInt()
        } else {
            0U
        }

        val fdOrErrno = openAt(
            dirfd = args[0] as Int,
            pathnamePtr = args[1] as WasmPtr,
            flags = (args[2] as Int).toUInt(),
            mode = mode,
        )
        return fdOrErrno
    }

    // XXX: copy of chikory version
    @TruffleBoundary
    private fun openAt(
        dirfd: Int,
        pathnamePtr: WasmPtr,
        flags: UInt,
        mode: UInt
    ): Int {
        val fs = host.fileSystem
        val path = memory.readNullTerminatedString(pathnamePtr)
        val absolutePath = fs.resolveAbsolutePath(dirfd, path)

        return try {
            val fd = fs.open(absolutePath, flags, mode).fd
            logger.finest { formatCallString(dirfd, path, absolutePath, flags, mode, fd) }
            fd.fd
        } catch (e: SysException) {
            logger.finest {
                formatCallString(dirfd, path, absolutePath, flags, mode, null) +
                        "openAt() error ${e.errNo}"
            }
            -e.errNo.code
        }
    }

    private fun formatCallString(
        dirfd: Int,
        path: String,
        absolutePath: Path,
        flags: UInt,
        mode: UInt,
        fd: Fd?
    ): String = "openAt() dirfd: " +
            "$dirfd, " +
            "path: `$path`, " +
            "full path: `$absolutePath`, " +
            "flags: 0${flags.toString(8)} (${Fcntl.oMaskToString(flags)}), " +
            "mode: ${Fcntl.sMaskToString(mode)}" +
            if (fd != null) ": $fd" else ""
}