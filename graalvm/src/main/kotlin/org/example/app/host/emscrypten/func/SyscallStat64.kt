package org.example.app.host.emscrypten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Logger
import org.example.app.ext.asWasmPtr
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.include.sys.StructStat
import ru.pixnews.wasm.host.include.sys.pack
import ru.pixnews.wasm.host.memory.write
import ru.pixnews.wasm.host.WasmPtr

fun syscallLstat64(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "__syscall_lstat64",
): BaseWasmNode = SyscallStat64(
    language = language,
    instance = instance,
    functionName = functionName,
    followSymlinks = false,
    filesystem = host.fileSystem,
)

fun syscallStat64(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "__syscall_stat64",
): BaseWasmNode = SyscallStat64(
    language = language,
    instance = instance,
    functionName = functionName,
    followSymlinks = true,
    filesystem = host.fileSystem,
)

private class SyscallStat64 (
    language: WasmLanguage,
    instance: WasmInstance,
    functionName: String,
    private val followSymlinks: Boolean = false,
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(SyscallStat64::class.qualifiedName)
) : BaseWasmNode(
    language = language,
    instance = instance,
    functionName = functionName,
) {

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return stat64(
            args.asWasmPtr(0),
            args.asWasmPtr(1),
        )
    }

    @TruffleBoundary
    private fun stat64(
        pathnamePtr: WasmPtr<Byte>,
        dst: WasmPtr<StructStat>,
    ) : Int {
        var path = ""
        try {
            path = memory.readNullTerminatedString(pathnamePtr)
            val stat = filesystem.stat(
                path = path,
                followSymlinks = followSymlinks
            ).also {
                logger.finest { "$functionName($path): $it" }
            }.pack()
            memory.write(dst, stat)
        } catch (e: SysException) {
            logger.finest { "$functionName(`$path`): error ${e.errNo}" }
            return -e.errNo.code
        }

        return 0
    }
}