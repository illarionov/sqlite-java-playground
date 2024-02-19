package org.example.app.host.preview1.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.wasi.preview1.type.Errno

class SchedYield(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "sched_yield",
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        return schedYield()
    }

    @TruffleBoundary
    private fun schedYield(
    ): Int {
        Thread.yield()
        return Errno.SUCCESS.code
    }
}