package ru.pixnews.wasm.sqlite3.chicory.host

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.WasiType

enum class PosixErrno(
    public val code: Int,
) {
    /**
     * No error occurred. System call completed successfully.
     */
    SUCCESS(0),

    EPERM(1),
    ENOENT(2),
    ESRCH(3),
    EINTR(4),
    EIO(5),
    ENXIO(6),
    E2BIG(7),
    ENOEXEC(8),
    EBADF(9),
    ECHILD(10),
    EAGAIN(11),
    ENOMEM(12),
    EACCES(13),
    EFAULT(14),
    ENOTBLK(15),
    EBUSY(16),
    EEXIST(17),
    EXDEV(18),
    ENODEV(19),
    ENOTDIR(20),
    EISDIR(21),
    EINVAL(22),
    ENFILE(23),
    EMFILE(24),
    ENOTTY(25),
    ETXTBSY(26),
    EFBIG(27),

    /**
     * No space left on device.
     */
    ENOSPC(28),
    ESPIPE(29),
    EROFS(30),
    EMLINK(31),
    EPIPE(32),
    EDOM(33),
    ERANGE(34),

    EADV(68),
    ;

    public val value: Value get() = Value.i32(code.toLong())

    public companion object : WasiType {
        override val valueType: ValueType = ValueType.I32

        fun fromErrNoCode(code: Int): Errno? = Errno.entries.firstNotNullOfOrNull { if (it.code == code) it else null }
    }
}