package org.example.app.host.emscrypten.func

import java.util.function.Function

class SyscallRmdirFunction : Function<Array<Any>, Any> {

    override fun apply(args: Array<Any>): Int {
        // TODO: errno?
        return -1
    }

    companion object {
        const val SYSCALL_RMDIR = "__syscall_rmdir"
    }
}