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
import ru.pixnews.wasm.host.wasi.preview1.ext.DefaultWasiMemoryReader
import ru.pixnews.wasm.host.wasi.preview1.ext.FdReadExt.readIovecs
import ru.pixnews.wasm.host.wasi.preview1.ext.WasiMemoryReader
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

fun fdRead(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "fd_read",
) : BaseWasmNode = FdRead(language, instance, host, CHANGE_POSITION, functionName)

fun fdPread(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "fd_pread",
): BaseWasmNode = FdRead(language, instance, host, DO_NOT_CHANGE_POSITION, functionName)

private class FdRead(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    strategy: ReadWriteStrategy,
    functionName: String = "fd_read",
    private val logger: Logger = Logger.getLogger(FdRead::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    private val memoryReader: WasiMemoryReader = DefaultWasiMemoryReader(host.fileSystem, strategy)

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return fdRead(
            Fd(args[0] as Int),
            args[1] as WasmPtr,
            args[2] as Int,
            args[3] as WasmPtr
        )
    }

    @TruffleBoundary
    private fun fdRead(
        fd: Fd,
        pIov: WasmPtr,
        iovCnt: Int,
        pNum: WasmPtr
    ): Int {
        val ioVecs: IovecArray = readIovecs(memory, pIov, iovCnt)
        return try {
            val readBytes = memoryReader.read(memory, fd, ioVecs)
            memory.writeI32(pNum, readBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "read() error" }
            e.errNo
        }.code
    }
}