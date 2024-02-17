package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8

/**
 * The type of a file descriptor or file.
 */
public enum class Filetype(
    val id: Int
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

    public companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = U8
    }
}