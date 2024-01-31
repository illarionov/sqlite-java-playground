package org.example.app

import java.util.function.Function as JavaFunction

class SyscallRmdirFunction : JavaFunction<Array<Any>, Any> {

    override fun apply(args: Array<Any>): Int {
        // TODO: errno?
        return -1
    }

    companion object {
        const val SYSCALL_RMDIR = "__syscall_rmdir"
    }
}