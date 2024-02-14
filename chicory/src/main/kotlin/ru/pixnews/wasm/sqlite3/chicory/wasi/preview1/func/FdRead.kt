package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import com.dylibso.chicory.wasm.types.ValueType.I32
import java.nio.ByteBuffer
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.ext.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.ext.WasmPtr
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.SysException
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Iovec
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Size
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.pointer

fun fdRead(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = HostFunction(
    FdRead(filesystem),
    moduleName,
    "fd_read",
    listOf(
        Fd.valueType, // Fd
        IovecArray.pointer, // iov
        I32, // iov_cnt
        I32.pointer, // pNum
    ),
    ParamTypes.i32,
)

private class FdRead(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(FdRead::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val fd = Fd(args[0].asInt())
        val pIov = args[1].asWasmAddr()
        val iovCnt = args[2].asInt()
        val pNum = args[3].asWasmAddr()

        val memory = instance.memory()
        val ioVecs = readIovecs(memory, pIov, iovCnt)
        val bbufs = ioVecs.toByteBuffers(memory)

        val errNo = try {
            val readBytes = filesystem.read(fd, bbufs)
            ioVecs.iovecList.forEachIndexed { idx, vec ->
                val bbuf: ByteBuffer = bbufs[idx]
                bbuf.flip()
                if (bbuf.limit() != 0) {
                    require(bbuf.hasArray())
                    memory.write(vec.buf.asWasmAddr(), bbuf.array(), 0, bbuf.limit())
                }
            }

            memory.writeI32(pNum, readBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            e.errNo
        }

        return arrayOf(Value.i32(errNo.code.toLong()))
    }

    private fun readIovecs(
        memory: Memory,
        pIov: WasmPtr,
        iovCnt: Int
    ): IovecArray {
         val iovecs = MutableList(iovCnt) { idx ->
            val pIovec = pIov + 8 * idx
            Iovec(
                buf = memory.readI32(pIovec),
                bufLen = Size(memory.readI32(pIovec + 4))
            )
        }
        return IovecArray(iovecs)
    }

    private fun IovecArray.toByteBuffers(
        memory: Memory
    ): List<ByteBuffer> = this.iovecList.map { iovec ->
        ByteBuffer.allocate(iovec.bufLen.value.asInt())
    }
}