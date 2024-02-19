package org.example.app.host.preview1.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Level
import java.util.logging.Logger
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.host.wasi.preview1.ext.FdWriteExt
import ru.pixnews.wasm.host.memory.WasiMemoryWriter
import ru.pixnews.wasm.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

fun fdWrite(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "fd_write",
) : BaseWasmNode = FdWrite(language, instance, host, CHANGE_POSITION, functionName)

fun fdPwrite(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "fd_pwrite",
): BaseWasmNode = FdWrite(language, instance, host, DO_NOT_CHANGE_POSITION, functionName)

private class FdWrite(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    private val strategy: ReadWriteStrategy,
    functionName: String = "fd_write",
    private val logger: Logger = Logger.getLogger(FdWrite::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return fdWrite(
            Fd(args[0] as Int),
            args[1] as WasmPtr,
            args[2] as Int,
            args[3] as WasmPtr
        )
    }

    @TruffleBoundary
    private fun fdWrite(
        fd: Fd,
        pCiov: WasmPtr,
        cIovCnt: Int,
        pNum: WasmPtr
    ): Int {
        val cioVecs: CiovecArray = FdWriteExt.readCiovecs(memory, pCiov, cIovCnt)
        return try {
            val channel = host.fileSystem.getStreamByFd(fd)
            val writtenBytes = memory.writeToChannel(channel, strategy, cioVecs)
            memory.writeI32(pNum, writtenBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "write() error" }
            e.errNo
        }.code
    }
}