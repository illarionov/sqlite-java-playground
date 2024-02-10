package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType

/**
 * Signal condition.
 */
public enum class Signal(
    public val value: Value
) {
    /**
     * No signal. Note that POSIX has special semantics for `kill(pid, 0)`,
     * so this value is reserved.
     */
    NONE(0),

    /**
     * Hangup.
     * Action: Terminates the process.
     */
    HUP(1),

    /**
     * Terminate interrupt signal.
     * Action: Terminates the process.
     */
    INT(2),

    /**
     * Terminal quit signal.
     * Action: Terminates the process.
     */
    QUIT(3),

    /**
     * Illegal instruction.
     * Action: Terminates the process.
     */
    ILL(4),

    /**
     * Trace/breakpoint trap.
     * Action: Terminates the process.
     */
    TRAP(5),

    /**
     * Process abort signal.
     * Action: Terminates the process.
     */
    ABRT(6),

    /**
     * Access to an undefined portion of a memory object.
     * Action: Terminates the process.
     */
    BUS(7),

    /**
     * Erroneous arithmetic operation.
     * Action: Terminates the process.
     */
    FPE(8),

    /**
     * Kill.
     * Action: Terminates the process.
     */
    KILL(9),

    /**
     * User-defined signal 1.
     * Action: Terminates the process.
     */
    USR1(10),

    /**
     * Invalid memory reference.
     * Action: Terminates the process.
     */
    SEGV(11),

    /**
     * User-defined signal 2.
     * Action: Terminates the process.
     */
    USR2(12),

    /**
     * Write on a pipe with no one to read it.
     *  Action: Ignored.
     */
    PIPE(13),

    /**
     * Alarm clock.
     * Action: Terminates the process.
     */
    ALRM(14),

    /**
     * Termination signal.
     * Action: Terminates the process.
     */
    TERM(15),

    /**
     * Child process terminated, stopped, or continued.
     *  Action: Ignored.
     */
    CHLD(16),

    /**
     * Continue executing, if stopped.
     * Action: Continues executing, if stopped.
     */
    CONT(17),

    /**
     * Stop executing.
     * Action: Stops executing.
     */
    STOP(18),

    /**
     * Terminal stop signal.
     * Action: Stops executing.
     */
    TSTP(19),

    /**
     * Background process attempting read.
     * Action: Stops executing.
     */
    TTIN(20),

    /**
     * Background process attempting write.
     * Action: Stops executing.
     */
    TTOU(21),

    /**
     * High bandwidth data is available at a socket.
     * Action: Ignored.
     *
     */
    URG(22),

    /**
     * CPU time limit exceeded.
     * Action: Terminates the process.
     */
    XCPU(23),

    /**
     * File size limit exceeded.
     * Action: Terminates the process.
     */
    XFSZ(24),

    /**
     * Virtual timer expired.
     * Action: Terminates the process.
     */
    VTALRM(25),

    /**
     * Profiling timer expired.
     * Action: Terminates the process.
     */
    PROF(26),

    /**
     * Window changed.
     * Action: Ignored.
     */
    WINCH(27),

    /**
     * I/O possible.
     * Action: Terminates the process.
     */
    POLL(28),

    /**
     * Power failure.
     *  Action: Terminates the process.
     */
    PWR(29),

    /**
     * Bad system call.
     *  Action: Terminates the process.
     */
    SYS(30),

    ;

    private constructor(i: Long) : this(Value.i32(i))

    public companion object : WasiType {
        override val valueType: ValueType = U8
    }
}