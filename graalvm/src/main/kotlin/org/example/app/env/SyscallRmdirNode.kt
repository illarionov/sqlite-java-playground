package org.example.app.env

import com.oracle.truffle.api.frame.VirtualFrame
import java.io.IOException
import java.nio.file.DirectoryNotEmptyException
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.predefined.WasmBuiltinRootNode

class SyscallRmdirNode(
    language: WasmLanguage,
    instance: WasmInstance
) : WasmBuiltinRootNode(language, instance) {
    override fun builtinNodeName(): String = "__syscall_rmdir"

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Any {
        val args = frame.arguments
        return rmDir(context, args[0] as Int)
    }

    private fun rmDir(
        context: WasmContext,
        pathAddress: Int,
    ): Int {
        val path = memory().readString(pathAddress, null)
        // TODO: errno
        try {
            val hostDir = context.environment().getPublicTruffleFile(path).getCanonicalFile()
            hostDir.delete()
        } catch (securityException: SecurityException) {
            return -1
        } catch (noSuchFile: NoSuchFileException) {
            return -1
        } catch (exception: DirectoryNotEmptyException) {
            return -1
        } catch (ioException: IOException) {
            return -1
        } catch (generalException: Exception) {
            return -1
        }
        return 0
    }
}