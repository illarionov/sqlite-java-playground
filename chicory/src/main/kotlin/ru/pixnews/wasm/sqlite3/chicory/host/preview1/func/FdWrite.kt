package ru.pixnews.wasm.sqlite3.chicory.host.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Level
import java.util.logging.Logger
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.host.filesystem.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.host.wasi.preview1.ext.FdWriteExt.readCiovecs
import ru.pixnews.wasm.host.memory.WasiMemoryWriter
import ru.pixnews.wasm.host.wasi.preview1.type.CioVec
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.host.memory.UnsafeWasiMemoryWriter
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WasiHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.wasiHostFunction

fun fdWrite(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdWrite(memory, filesystem, moduleName, "fd_write", CHANGE_POSITION)

fun fdPwrite(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdWrite(memory, filesystem, moduleName, "fd_pwrite", DO_NOT_CHANGE_POSITION)

private fun fdWrite(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String,
    fieldName: String,
    strategy: ReadWriteStrategy
): HostFunction = wasiHostFunction(
    funcName = fieldName,
    paramTypes = listOf(
        Fd.wasmValueType, // Fd
        IovecArray.pointer, // ciov
        I32, // ciov_cnt
        I32.pointer, // pNum
    ),
    moduleName = moduleName,
    handle = FdWrite(memory, filesystem, strategy)
)

private class FdWrite(
    private val memory: Memory,
    private val filesystem: FileSystem,
    private val strategy: ReadWriteStrategy,
    private val logger: Logger = Logger.getLogger(FdWrite::class.qualifiedName)
) : WasiHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Errno {
        val fd = Fd(args[0].asInt())
        val pCiov = args[1].asWasmAddr<CioVec>()
        val cIovCnt = args[2].asInt()
        val pNum = args[3].asWasmAddr<Long>()

        val cioVecs = readCiovecs(memory, pCiov, cIovCnt)
        return try {
            val channel = filesystem.getStreamByFd(fd)
            val writtenBytes = memory.writeToChannel(channel, strategy, cioVecs)
            memory.writeI32(pNum, writtenBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "write() error" }
            e.errNo
        }
    }

}
