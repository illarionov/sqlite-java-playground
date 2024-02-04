package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.Value.i32
import com.dylibso.chicory.wasm.types.ValueType

/**
 * Type names used by low-level WASI interfaces.
 *  https://raw.githubusercontent.com/WebAssembly/WASI/main/legacy/preview1/witx/typenames.witx
 */

val U8: ValueType = ValueType.I32
val U16: ValueType = ValueType.I32
val S32: ValueType = ValueType.I32
val U32: ValueType = ValueType.I32
val S64: ValueType = ValueType.I64
val U64: ValueType = ValueType.I64
val Handle: ValueType = ValueType.I32

val WasiSize: ValueType = U32

/**
 * Non-negative file size or length of a region within a file.
 */
val WasiFileSize: ValueType = U64

/**
 * Timestamp in nanoseconds.
 */
val WasiTimestamp: ValueType = U64

/**
 * Identifiers for clocks.
 */
enum class WasiClockId(
    public val id: Value
) {
    /**
     * The clock measuring real time. Time value zero corresponds with 1970-01-01T00:00:00Z.
     */
    REALTIME(0),

    /**
     * The store-wide monotonic clock, which is defined as a clock measuring
     * real time, whose value cannot be adjusted and which cannot have negative
     * clock jumps. The epoch of this clock is undefined. The absolute time
     * value of this clock therefore has no meaning.
     */
    MONOTONIC(1),

    /**
     * The CPU-time clock associated with the current process.
     */
    PROCESS_CPUTIME_ID(2),

    /**
     * The CPU-time clock associated with the current thread.
     */
    THREAD_CPUTIME_ID(3),

    ;

    private constructor(i: Long) : this(i32(i))

    public companion object {
        val tag: ValueType = U32
    }
}

public enum class WasiErrno(
    public val id: Value,
) {
    /**
     * No error occurred. System call completed successfully.
     */
    SUCCESS(0),

    /*
     * Argument list too long.
     */
    TOO_BIG(1),

    /**
     * Permission denied.
     */
    ACCES(2),

    /**
     * Address in use.
     */
    ADDRINUSE(3),

    /**
     * Address not available.
     */
    ADDRNOTAVAIL(4),

    /**
     * Address family not supported.
     */
    AFNOSUPPORT(5),

    /**
     * Resource unavailable, or operation would block.
     */
    AGAIN(6),

    /**
     * Connection already in progress.
     */
    ALREADY(7),

    /**
     * Bad file descriptor.
     */
    BADF(8),

    /**
     * Bad message.
     */
    BADMSG(9),

    /**
     * Device or resource busy.
     */
    BUSY(10),

    /**
     * Operation canceled.
     */
    CANCELED(11),

    /**
     * No child processes.
     */
    CHILD(12),

    /**
     * Connection aborted.
     */
    CONNABORTED(13),

    /**
     * Connection refused.
     */
    CONNREFUSED(14),

    /**
     * Connection reset.
     */
    CONNRESET(15),

    /**
     * Resource deadlock would occur.
     */
    DEADLK(16),

    /**
     * Destination address required.
     */
    DESTADDRREQ(17),

    /**
     * Mathematics argument out of domain of function.
     */
    DOM(18),

    /**
     * Reserved.
     */
    DQUOT(19),

    /**
     * File exists.
     */
    EXIST(20),

    /**
     * Bad address.
     */
    FAULT(21),

    /**
     * File too large.
     */
    FBIG(22),

    /**
     * Host is unreachable.
     */
    HOSTUNREACH(23),

    /**
     * Identifier removed.
     */
    IDRM(24),

    /**
     * Illegal byte sequence.
     */
    ILSEQ(25),

    /**
     * Operation in progress.
     */
    INPROGRESS(26),

    /**
     * Interrupted function.
     */
    INTR(27),

    /**
     * Invalid argument.
     */
    INVAL(28),

    /**
     * I/O error.
     */
    IO(29),

    /**
     * Socket is connected.
     */
    ISCONN(30),

    /**
     * Is a directory.
     */
    ISDIR(31),

    /**
     * Too many levels of symbolic links.
     */
    LOOP(32),

    /**
     * File descriptor value too large.
     */
    MFILE(33),

    /**
     * Too many links.
     */
    MLINK(34),

    /**
     * Message too large.
     */
    MSGSIZE(35),

    /**
     * Reserved.
     */
    MULTIHOP(36),

    /**
     * Filename too long.
     */
    NAMETOOLONG(37),

    /**
     * Network is down.
     */
    NETDOWN(38),

    /**
     * Connection aborted by network.
     */
    NETRESET(39),

    /**
     * Network unreachable.
     */
    NETUNREACH(40),

    /**
     * Too many files open in system.
     */
    NFILE(41),

    /**
     * No buffer space available.
     */
    NOBUFS(42),

    /**
     * No such device.
     */
    NODEV(43),

    /**
     * No such file or directory.
     */
    NOENT(44),

    /**
     * Executable file format error.
     */
    NOEXEC(45),

    /**
     * No locks available.
     */
    NOLCK(46),

    /**
     * Reserved.
     */
    NOLINK(47),

    /**
     * Not enough space.
     */
    NOMEM(48),

    /**
     * No message of the desired type.
     */
    NOMSG(49),

    /**
     * Protocol not available.
     */
    NOPROTOOPT(50),

    /**
     * No space left on device.
     */
    NOSPC(51),

    /**
     * Function not supported.
     */
    NOSYS(52),

    /**
     * The socket is not connected.
     */
    NOTCONN(53),

    /**
     * Not a directory or a symbolic link to a directory.
     */
    NOTDIR(54),

    /**
     * Directory not empty.
     */
    NOTEMPTY(55),

    /**
     * State not recoverable.
     */
    NOTRECOVERABLE(56),

    /**
     * Not a socket.
     */
    NOTSOCK(57),

    /**
     * Not supported, or operation not supported on socket.
     */
    NOTSUP(58),

    /**
     * Inappropriate I/O control operation.
     */
    NOTTY(59),

    /**
     * No such device or address.
     */
    NXIO(60),

    /**
     * Value too large to be stored in data type.
     */
    OVERFLOW(61),

    /**
     * Previous owner died.
     */
    OWNERDEAD(62),

    /**
     * Operation not permitted.
     */
    PERM(63),

    /**
     * Broken pipe.
     */
    PIPE(64),

    /**
     * Protocol error.
     */
    PROTO(65),

    /**
     * Protocol not supported.
     */
    PROTONOSUPPORT(66),

    /**
     * Protocol wrong type for socket.
     */
    PROTOTYPE(67),

    /**
     * Result too large.
     */
    RANGE(68),

    /**
     * Read-only file system.
     */
    ROFS(69),

    /**
     * Invalid seek.
     */
    SPIPE(70),

    /**
     * No such process.
     */
    SRCH(71),

    /**
     * Reserved.
     */
    STALE(72),

    /**
     * Connection timed out.
     */
    TIMEDOUT(73),

    /**
     * Text file busy.
     */
    TXTBSY(74),

    /**
     * Cross-device link.
     */
    XDEV(75),

    /**
     * Extension: Capabilities insufficient.
     */
    NOTCAPABLE(76),

    ;

    private constructor(i: Long) : this(i32(i))

    public companion object {
        val tag: ValueType = U16
    }
}

@JvmInline
public value class WasiRights(
    public val mask: Value
) {

    public enum class Flags(
        public val mask: ULong
    ) {
        /**
         * The right to invoke `fd_datasync`.
         *
         * If `path_open` is set, includes the right to invoke
         *  `path_open` with `fdflags::dsync`.
         */
        FD_DATASYNC(0),

        /**
         * The right to invoke `fd_read` and `sock_recv`.
         *
         *  If `rights::fd_seek` is set, includes the right to invoke `fd_pread`.
         */
        FD_READ(1),

        /**
         * The right to invoke `fd_seek`. This flag implies `rights::fd_tell`.
         */
        FD_SEEK(2),

        /**
         * The right to invoke `fd_fdstat_set_flags`
         */
        FD_FDSTAT_SET_FLAGS(3),

        /**
         * The right to invoke `fd_sync`.
         *
         * If `path_open` is set, includes the right to invoke
         * `path_open` with `fdflags::rsync` and `fdflags::dsync`.
         */
        FD_SYNC(4),

        /**
         * The right to invoke `fd_seek` in such a way that the file offset
         * remains unaltered (i.e., `whence::cur` with offset zero), or to
         * invoke `fd_tell`.
         */
        FD_TELL(5),

        /**
         * The right to invoke `fd_write` and `sock_send`.
         *
         * If `rights::fd_seek` is set, includes the right to invoke `fd_pwrite`.
         */
        FD_WRITE(6),

        /**
         * The right to invoke `fd_advise`.
         */
        FD_ADVISE(7),

        /**
         * The right to invoke `fd_allocate`.
         */
        FD_ALLOCATE(8),

        /**
         * The right to invoke `path_create_directory`.
         */
        PATH_CREATE_DIRECTORY(9),

        /**
         * If `path_open` is set, the right to invoke `path_open` with `oflags::creat`.
         */
        PATH_CREATE_FILE(10),

        /**
         * The right to invoke `path_link` with the file descriptor as the
         * source directory.
         */
        PATH_LINK_SOURCE(11),

        /**
         * The right to invoke `path_link` with the file descriptor as the
         * target directory.
         */
        PATH_LINK_TARGET(12),

        /**
         * The right to invoke `path_open`.
         */
        PATH_OPEN(13),

        /**
         * The right to invoke `fd_readdir`.
         */
        FD_READDIR(14),

        /**
         * The right to invoke `path_readlink`.
         */
        PATH_READLINK(15),

        /**
         * The right to invoke `path_rename` with the file descriptor as the source directory.
         */
        PATH_RENAME_SOURCE(16),

        /**
         * The right to invoke `path_rename` with the file descriptor as the target directory.
         */
        PATH_RENAME_TARGET(17),

        /**
         * The right to invoke `path_filestat_get`.
         */
        PATH_FILESTAT_GET(18),

        /**
         * The right to change a file's size.
         * If `path_open` is set, includes the right to invoke `path_open` with `oflags::trunc`.
         * Note: there is no function named `path_filestat_set_size`. This follows POSIX design,
         * which only has `ftruncate` and does not provide `ftruncateat`.
         * While such function would be desirable from the API design perspective, there are virtually
         * no use cases for it since no code written for POSIX systems would use it.
         * Moreover, implementing it would require multiple syscalls, leading to inferior performance.
         */
        PATH_FILESTAT_SET_SIZE(19),

        /**
         * The right to invoke `path_filestat_set_times`.
         */
        PATH_FILESTAT_SET_TIMES(20),

        /**
         * The right to invoke `fd_filestat_get`.
         */
        FD_FILESTAT_GET(21),

        /**
         * The right to invoke `fd_filestat_set_size`.
         */
        FD_FILESTAT_SET_SIZE(22),

        /**
         * The right to invoke `fd_filestat_set_times`.
         */
        FD_FILESTAT_SET_TIMES(23),

        /**
         * The right to invoke `path_symlink`.
         */
        PATH_SYMLINK(24),

        /**
         * The right to invoke `path_remove_directory`.
         */
        PATH_REMOVE_DIRECTORY(25),

        /**
         * The right to invoke `path_unlink_file`.
         */
        PATH_UNLINK_FILE(26),

        /**
         * If `rights::fd_read` is set, includes the right to invoke `poll_oneoff` to subscribe to `eventtype::fd_read`.
         * If `rights::fd_write` is set, includes the right to invoke `poll_oneoff` to subscribe to `eventtype::fd_write`.
         */
        POLL_FD_READWRITE(27),

        /**
         * The right to invoke `sock_shutdown`.
         */
        SOCK_SHUTDOWN(28),

        /**
         * The right to invoke `sock_accept`.
         */
        SOCK_ACCEPT(29),

        ;

        private constructor(bit: Int) : this(1UL.shl(bit))

        public companion object {
            val tag: ValueType = U16
        }
    }
}

val WasiFd = Handle

/**
 * A region of memory for scatter/gather reads.
 *
 * @param buf The address of the buffer to be filled.
 * @param bufLen The length of the buffer to be filled.
 */
data class WasiIovec(
    val buf: Value, // (@witx const_pointer u8))
    val bufLen: Value // (field $buf_len $size)
)

/**
 * A region of memory for scatter/gather writes.
 *
 * @param buf The address of the buffer to be written.
 * @param bufLen The length of the buffer to be written.
 */
data class WasiCioVec(
    val buf: Value, // (@witx const_pointer u8))
    val bufLen: Value // (field $buf_len $size)
)

// (typename $iovec_array (list $iovec))
@JvmInline
value class WasiIovecArray(
    val iovecList: List<WasiIovec>
)

// (typename $ciovec_array (list $ciovec))
@JvmInline
value class WasiCiovecArray(
    val ciovecList: List<WasiIovec>
)

// Relative offset within a file.
val WasiFileDelta = ValueType.I32

/**
 * The position relative to which to set the offset of the file descriptor.
 */
public enum class Whence(
    public val id: Value
) {
    /**
     * Seek relative to start-of-file.
     */
    SET(0),

    /**
     * Seek relative to current position.
     */
    CUR(1),

    /**
     * Seek relative to end-of-file.
     */
    END(2),

    ;

    private constructor(id: Long) : this(i32(id))

    public companion object {
        val tag: ValueType = U8
    }
}

/**
 * A reference to the offset of a directory entry.
 *
 * The value 0 signifies the start of the directory.
 */
val WasiDircookie = U64

/**
 * The type for the `dirent::d_namlen` field of `dirent` struct.
 */
val WasiDirnamlen = U32

/**
 * File serial number that is unique within its file system.
 */
val WasiInode = U64

public enum class WasiFiletype(
    val id: Value
) {
    /**
     * The type of the file descriptor or file is unknown or is different from any of the other types specified.
     */
    UNKNOWN(0),

    /**
     * The file descriptor or file refers to a block device inode.
     */
    BLOCK_DEVICE(1),

    /**
     * The file descriptor or file refers to a character device inode.
     */
    CHARACTER_DEVICE(2),

    /**
     * The file descriptor or file refers to a directory inode.
     */
    DIRECTORY(3),

    /**
     * The file descriptor or file refers to a regular file inode.
     */
    REGULAR_FILE(4),

    /**
     * The file descriptor or file refers to a datagram socket.
     */
    SOCKET_DGRAM(5),

    /**
     * The file descriptor or file refers to a byte-stream socket.
     */
    SOCKET_STREAM(6),

    /**
     * The file refers to a symbolic link inode.
     */
    SYMBOLIC_LINK(7),

    ;

    private constructor(id: Long) : this(i32(id))

    public companion object {
        val tag: ValueType = U8
    }
}

/**
 * A directory entry.
 *
 * @param dNext The offset of the next directory entry stored in this directory.
 * @param dIno The serial number of the file referred to by this directory entry.
 * @param dNamlen The length of the name of the directory entry.
 * @param dType The type of the file referred to by this directory entry.
 */
data class WasiDirent(
    val dNext: Value, // (field $d_next $dircookie)
    val dIno: Value, // (field $d_ino $inode)
    val dNamlen: Value, // The length of the name of the directory entry.
    val dType: Value, // (field $d_type $filetype)
)

/**
 * File or memory access pattern advisory information.
 */
public enum class WasiAdvice(
    val id: Value
) {
    /**
     * The application has no advice to give on its behavior with respect to the specified data.
     */
    NORMAL(0),

    /**
     * The application expects to access the specified data sequentially from lower offsets to higher offsets.
     */
    SEQUENTIAL(1),

    /**
     * The application expects to access the specified data in a random order.
     */
    RANDOM(2),

    /**
     * The application expects to access the specified data in the near future.
     */
    WILLNEED(3),

    /**
     * The application expects that it will not access the specified data in the near future.
     */
    DONTNEED(4),

    /**
     * The application expects to access the specified data once and then not reuse it thereafter.
     */
    NOREUSE(5),

    ;

    private constructor(id: Long) : this(i32(id))

    companion object {
        val tag: ValueType = U8
    }
}

/**
 * File descriptor flags.
 */
@JvmInline
public value class WasiFdFlags(
    val mask: Value,
) {
    public enum class Flags(
        val mask: ULong
    ) {
        /**
         * Append mode: Data written to the file is always appended to the file's end.
         */
        APPEND(0),

        /**
         * Write according to synchronized I/O data integrity completion. Only the data stored in the file is synchronized.
         */
        DSYNC(1),

        /**
         * Non-blocking mode.
         */
        NONBLOCK(2),

        /**
         * Synchronized read I/O operations.
         */
        RSYNC(3),

        /**
         * Write according to synchronized I/O file integrity completion. In
         * addition to synchronizing the data stored in the file, the implementation
         * may also synchronously update the file's metadata.
         */
        SYNC(4)

        ;

        private constructor(bit: Int) : this(1UL.shl(bit))

        public companion object {
            val tag: ValueType = U16
        }
    }
}

/**
 * File descriptor attributes.
 *
 * @param fsFiletype File type.
 * @param fsFlags File descriptor flags.
 * @param fsRightsBase Rights that apply to this file descriptor.
 * @param fsRightsInheriting Maximum set of rights that may be installed on new file descriptors that are created
 * through this file descriptor, e.g., through `path_open`.
 */
public data class WasiFdStat(
    val fsFiletype: Value, // (field $fs_filetype $filetype)
    val fsFlags: Value, // (field $fs_flags $fdflags)
    val fsRightsBase: Value, // (field $fs_rights_base $rights)
    val fsRightsInheriting: Value // (field $fs_rights_inheriting $rights)
)

public val WasiDevice = U64

@JvmInline
public value class WasiFstflags(
    val mask: Value
) {
    public enum class Fstflags(
        val mask: ULong,
    ) {
        /**
         * Adjust the last data access timestamp to the value stored in `filestat::atim`.
         */
        ATIM(0),

        /**
         * Adjust the last data access timestamp to the time of clock `clockid::realtime`.
         */
        ATIM_NOW(1),

        /**
         * Adjust the last data modification timestamp to the value stored in `filestat::mtim`.
         */
        MTIM(2),

        /**
         * Adjust the last data modification timestamp to the time of clock `clockid::realtime`.
         */
        MTIM_NOW(3),

        ;

        private constructor(bit: Int) : this(1UL.shl(bit))

        public companion object {
            val tag: ValueType = U16
        }
    }
}

/**
 * Flags determining the method of how paths are resolved.
 */
@JvmInline
public value class WasiLookupFlags(
    val mask: Value
) {

    public enum class Fstflags(
        val mask: ULong,
    ) {

        /**
         * As long as the resolved path corresponds to a symbolic link, it is expanded.
         */
        SYMLINK_FOLLOW(0),

        ;

        private constructor(bit: Int) : this(1UL.shl(bit))

        public companion object {
            val tag: ValueType = U32
        }
    }
}

/**
 * Open flags used by `path_open`.
 */
@JvmInline
public value class WasiOflags(
    val mask: Value,
) {
    public enum class Oflags(
        val mask: ULong
    ) {
        /**
         * Create file if it does not exist.
         */
        CREAT(0),

        /**
         * Fail if not a directory.
         */
        DIRECTORY(1),

        /**
         * Fail if file already exists.
         */
        EXCL(2),

        /**
         * Truncate file to size 0.
         */
        TRUNC(3),

        ;

        private constructor(bit: Int) : this(1UL.shl(bit))

        public companion object {
            val tag: ValueType = U16
        }
    }
}

public val WasiLinkcount = U64

/**
 * File attributes.
 */
public data class WasiFilestat(
    /**
     * Device ID of device containing the file.
     */
    val dev: Value, // (field $dev $device)

    /**
     * File serial number.
     */
    val ino: Value, // (field $ino $inode)

    /**
     * File type.
     */
    val fileType: Value, // (field $filetype $filetype)

    /**
     * Number of hard links to the file.
     */
    val nlink: Value, // (field $nlink $linkcount)

    /**
     * For regular files, the file size in bytes. For symbolic links, the length in bytes of the pathname contained
     * in the symbolic link.
     */
    val size: Value, // (field $size $filesize)

    /**
     * Last data access timestamp.
     *
     * This can be 0 if the underlying platform doesn't provide suitable
     * timestamp for this file.
     */
    val atim: Value, // (field $atim $timestamp)

    /**
     * Last data modification timestamp.
     *
     * This can be 0 if the underlying platform doesn't provide suitable
     * timestamp for this file.
     */
    val mtim: Value, // (field $mtim $timestamp)

    /**
     * Last file status change timestamp.
     *
     * This can be 0 if the underlying platform doesn't provide suitable
     * timestamp for this file.
     */
    val ctim: Value, // (field $ctim $timestamp)
)

/**
 * User-provided value that may be attached to objects that is retained when
 * extracted from the implementation.
 */
val WasiUserdata = U64


/**
 * Type of a subscription to an event or its occurrence.
 */
public enum class WasiEventtype(
    public val id: Value
) {
    /**
     * The time value of clock `subscription_clock::id` has
     * reached timestamp `subscription_clock::timeout`.
     */
    CLOCK(0),

    /**
     * File descriptor `subscription_fd_readwrite::file_descriptor` has data
     * available for reading. This event always triggers for regular files.
     */
    FD_READ(1),

    /**
     * File descriptor `subscription_fd_readwrite::file_descriptor` has capacity
     * available for writing. This event always triggers for regular files.
     */
    FD_WRITE(2),

    ;

    private constructor(id: Long) : this(i32(id))

    companion object {
        val tag: ValueType = U8
    }
}

/**
 * The state of the file descriptor subscribed to with
 * `eventtype::fd_read` or `eventtype::fd_write`.
 */
@JvmInline
public value class WasiEventrwflags(
    val mask: Value,
) {

    public enum class Eventrwflags(
        val mask: ULong
    ) {
        /**
         * The peer of this socket has closed or disconnected.
         */
        FD_READWRITE_HANGUP(0),

        ;

        private constructor(bit: Int) : this(1UL.shl(bit))

        public companion object {
            val tag: ValueType = U16
        }
    }
}

/**
 * The contents of an `event` when type is `eventtype::fd_read` or
 * `eventtype::fd_write`.
 *
 * @param nbytes The number of bytes available for reading or writing.
 * @param flags The state of the file descriptor.
 */
public data class WasiEventFdReadwrite(
    val nbytes: Value, // (field $nbytes $filesize)
    val flags: Value, // field $flags $eventrwflags)
)

/**
 * An event that occurred.
 *
 * @param userdata User-provided value that got attached to `subscription::userdata`.
 * @param error (field $error $errno)
 * @param type The type of event that occured
 * @param fd_readwrite The contents of the event, if it is an `eventtype::fd_read` or `eventtype::fd_write`.
 * `eventtype::clock` events ignore this field.
 */
public data class WasiEvent(
    val userdata: Value, // (field $userdata $userdata)
    val error: WasiErrno, // (field $error $errno)
    val type: WasiEventtype, // (field $type $eventtype)
    val fdReadwrite: Value, // (field $fd_readwrite $event_fd_readwrite)
)

/**
 * Flags determining how to interpret the timestamp provided in `subscription_clock::timeout`.
 */
@JvmInline
public value class WasiSubclockflags(
    val mask: Value
) {
    public enum class Subclockflags(
        val mask: ULong,
    ) {

        /**
         * If set, treat the timestamp provided in
         * `subscription_clock::timeout` as an absolute timestamp of clock
         * `subscription_clock::id`. If clear, treat the timestamp
         * provided in `subscription_clock::timeout` relative to the
         * current time value of clock `subscription_clock::id`.
         */
        SUBSCRIPTION_CLOCK_ABSTIME(0)

        ;

        private constructor(bit: Int) : this(1UL.shl(bit))

        public companion object {
            val tag: ValueType = U16
        }
    }
}

/**
 * The contents of a `subscription` when type is `eventtype::clock`.
 *
 * @param id The clock against which to compare the timestamp.
 * @param timeout The absolute or relative timestamp.
 * @param precision The amount of time that the implementation may wait additionally to coalesce with other events.
 * @param flags Flags specifying whether the timeout is absolute or relative
 */
public data class WasiSubscriptionClock(
    val id: Value, // (field $id $clockid)
    val timeout: Value, // (field $timeout $timestamp)
    val precision: Value, // (field $precision $timestamp)
    val flags: Value, // (field $flags $subclockflags)
)

/**
 * The contents of a `subscription` when type is type is `eventtype::fd_read` or `eventtype::fd_write`.
 *
 * @param fileDescriptor The file descriptor on which to wait for it to become ready for reading or writing.
 */
public data class WasiSubscriptionFdReadwrite(
    val fileDescriptor: Value // (field $file_descriptor $fd)
)

/**
 * The contents of a `subscription`.
 */
public sealed class WasiSubscriptionU(
    open val eventType: WasiEventtype
) {
    public data class Clock(
        val subscriptionClock: Value
    ) : WasiSubscriptionU(WasiEventtype.CLOCK) {
    }

    public data class FdRead(
        val subscription_fd_readwrite: Value
    ) : WasiSubscriptionU(WasiEventtype.FD_READ) {
    }

    public data class FdWrite(
        val subscription_fd_readwrite: Value
    ) : WasiSubscriptionU(WasiEventtype.FD_WRITE) {
    }
}

/**
 * Subscription to an event.
 *
 * @param userdata User-provided value that is attached to the subscription in the  implementation and returned
 * through `event::userdata`.
 * @param u The type of the event to which to subscribe, and its contents
 */
public data class Subscription(
    val userdata: Value, // (field $userdata $userdata)
    val u: WasiSubscriptionU, // (field $u $subscription_u)
)

public val wasiExitCode = U32

/**
 * Signal condition.
 */
public enum class WasiSignal(
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

    private constructor(i: Long) : this(i32(i))

    public companion object {
        val tag: ValueType = U8
    }
}


/**
 * Flags provided to `sock_recv`.
 */
@JvmInline
public value class WasiRiflags(
    val mask: Value
) {
    public enum class RiFlags(
        val mask: ULong
    ) {

        /**
         * Returns the message without removing it from the socket's receive queue.
         */
        RECV_PEEK(0),

        /**
         * On byte-stream sockets, block until the full amount of data can be returned.
         */
        RECV_WAITALL(1),

        ;

        private constructor(bit: Int) : this(1UL.shl(bit))

        public companion object {
            val tag: ValueType = U16
        }
    }
}

/**
 * Flags returned by `sock_recv`.
 */
@JvmInline
public value class WasiRoflags(
    val mask: Value
) {
    public enum class RoFlags(
        val mask: ULong
    ) {
        /**
         * Returned by `sock_recv`: Message data has been truncated.
         */
        RECV_DATA_TRUNCATED(0)

        ;

        private constructor(bit: Int) : this(1UL.shl(bit))

        public companion object {
            val tag: ValueType = U16
        }
    }

}

/**
 * Flags provided to `sock_send`. As there are currently no flags
 * defined, it must be set to zero.
 */
public val WasiSiFlags = U16

/**
 * Which channels on a socket to shut down.
 */
@JvmInline
public value class WasiSdflags(
    val mask: Value
) {
    public enum class Sdflags(
        val mask: ULong
    ) {
        /**
         * Disables further receive operations.
         */
        RD(0),

        /**
         * Disables further send operations.
         */
        WR(1)

        ;

        private constructor(bit: Int) : this(1UL.shl(bit))

        public companion object {
            val tag: ValueType = U8
        }
    }
}

/**
 * Identifiers for preopened capabilities.
 */
public enum class WasiPreopentype(
    val value: Value,
) {
    /**
     * A pre-opened directory.
     */
    DIR(0)

    ;

    private constructor(i: Long) : this(i32(i))

    public companion object {
        val tag: ValueType = U8
    }
}

/**
 * The contents of a `prestat` when type is `preopentype::dir`.
 */
public data class WasiPrestatDir(

    /**
     * The length of the directory name for use with `fd_prestat_dir_name`.
     */
    public val prNameLen: Value // (field $pr_name_len $size)
)

/**
 * Information about a pre-opened capability.
 */
public sealed class WasiPrestat(
    public open val tag: WasiPreopentype
) {
    public data class Dir(
        val prestatDir: Value
    ) : WasiPrestat(WasiPreopentype.DIR)
}
