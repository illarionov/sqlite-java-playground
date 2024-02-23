package org.example.app.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.example.app.ext.asWasmPtr
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.emscrypten.AssertionFailed
import ru.pixnews.wasm.host.WasmPtr

class AssertFail(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "__assert_fail",
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Nothing {
        val args = frame.arguments
        assertFail(
            args.asWasmPtr(0),
            args.asWasmPtr(1),
            args[2] as Int,
            args.asWasmPtr(3),
        )
    }

    @TruffleBoundary
    private fun assertFail(
        condition: WasmPtr<Byte>,
        filename: WasmPtr<Byte>,
        line: Int,
        func: WasmPtr<Byte>
    ) : Nothing {
        throw AssertionFailed(
            condition = memory.readNullTerminatedString(condition),
            filename = memory.readNullTerminatedString(filename),
            line = line,
            func = memory.readNullTerminatedString(func)
        )
    }
}