package org.example.app.host.emscrypten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.emscrypten.AssertionFailed
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

class AssertFail(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "__assert_fail",
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Nothing {
        val args = frame.arguments
        assertFail(
            args[0] as WasmPtr,
            args[1] as WasmPtr,
            args[2] as Int,
            args[3] as WasmPtr
        )
    }

    @TruffleBoundary
    private fun assertFail(
        condition: WasmPtr,
        filename: WasmPtr,
        line: Int,
        func: WasmPtr
    ) : Nothing {
        throw AssertionFailed(
            condition = memory.readNullTerminatedString(condition),
            filename = memory.readNullTerminatedString(filename),
            line = line,
            func = memory.readNullTerminatedString(func)
        )
    }
}