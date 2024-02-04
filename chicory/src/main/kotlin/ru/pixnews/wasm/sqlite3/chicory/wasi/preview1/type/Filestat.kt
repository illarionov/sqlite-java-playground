package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

/**
 * File attributes.
 */
public data class Filestat(
    /**
     * Device ID of device containing the file.
     */
    val dev: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Device, // (field $dev $device)

    /**
     * File serial number.
     */
    val ino: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Inode, // (field $ino $inode)

    /**
     * File type.
     */
    val fileType: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Filetype, // (field $filetype $filetype)

    /**
     * Number of hard links to the file.
     */
    val nlink: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Linkcount, // (field $nlink $linkcount)

    /**
     * For regular files, the file size in bytes. For symbolic links, the length in bytes of the pathname contained
     * in the symbolic link.
     */
    val size: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.FileSize, // (field $size $filesize)

    /**
     * Last data access timestamp.
     *
     * This can be 0 if the underlying platform doesn't provide suitable
     * timestamp for this file.
     */
    val atim: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Timestamp, // (field $atim $timestamp)

    /**
     * Last data modification timestamp.
     *
     * This can be 0 if the underlying platform doesn't provide suitable
     * timestamp for this file.
     */
    val mtim: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Timestamp, // (field $mtim $timestamp)

    /**
     * Last file status change timestamp.
     *
     * This can be 0 if the underlying platform doesn't provide suitable
     * timestamp for this file.
     */
    val ctim: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Timestamp, // (field $ctim $timestamp)
)