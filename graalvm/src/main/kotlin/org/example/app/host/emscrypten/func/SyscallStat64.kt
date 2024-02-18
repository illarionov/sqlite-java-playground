package org.example.app.host.emscrypten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Logger
import org.example.app.host.BaseWasmRootNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmType.I32_TYPE
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.include.sys.pack
import ru.pixnews.wasm.host.memory.write
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

fun syscallLstat64(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
): BaseWasmRootNode = SyscallStat64(
    language = language,
    instance = instance,
    name = "__syscall_lstat64",
    followSymlinks = false,
    filesystem = host.fileSystem,
)

fun syscallStat64(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
): BaseWasmRootNode = SyscallStat64(
    language = language,
    instance = instance,
    name = "__syscall_stat64",
    followSymlinks = true,
    filesystem = host.fileSystem,
)

private class SyscallStat64 (
    language: WasmLanguage,
    instance: WasmInstance,
    private val name: String,
    private val followSymlinks: Boolean = false,
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(SyscallStat64::class.qualifiedName)
) : BaseWasmRootNode(
    language = language,
    instance = instance,
    functionName = name,
) {

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return stat64(
            args[0] as WasmPtr,
            args[1] as WasmPtr
        )
    }

    @TruffleBoundary
    private fun stat64(
        pathnamePtr: WasmPtr,
        dst: WasmPtr,
    ) : Int {
        var path = ""
        try {
            path = memory.readNullTerminatedString(pathnamePtr)
            val stat = filesystem.stat(
                path = path,
                followSymlinks = followSymlinks
            ).also {
                logger.finest { "$name($path): $it" }
            }.pack()
            memory.write(dst, stat)
        } catch (e: SysException) {
            logger.finest { "$name(`$path`): error ${e.errNo}" }
            return -e.errNo.code
        }

        return 0
    }
}