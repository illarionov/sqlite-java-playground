package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

import com.dylibso.chicory.wasm.types.ValueType

/**
 * File attributes.
 */
public data class Filestat(
    /**
     * Device ID of device containing the file.
     */
    val dev: Device, // (field $dev $device)

    /**
     * File serial number.
     */
    val ino: Inode, // (field $ino $inode)

    /**
     * File type.
     */
    val fileType: Filetype, // (field $filetype $filetype)

    /**
     * Number of hard links to the file.
     */
    val nlink: Linkcount, // (field $nlink $linkcount)

    /**
     * For regular files, the file size in bytes. For symbolic links, the length in bytes of the pathname contained
     * in the symbolic link.
     */
    val size: FileSize, // (field $size $filesize)

    /**
     * Last data access timestamp.
     *
     * This can be 0 if the underlying platform doesn't provide suitable
     * timestamp for this file.
     */
    val atim: Timestamp, // (field $atim $timestamp)

    /**
     * Last data modification timestamp.
     *
     * This can be 0 if the underlying platform doesn't provide suitable
     * timestamp for this file.
     */
    val mtim: Timestamp, // (field $mtim $timestamp)

    /**
     * Last file status change timestamp.
     *
     * This can be 0 if the underlying platform doesn't provide suitable
     * timestamp for this file.
     */
    val ctim: Timestamp, // (field $ctim $timestamp)
) {
    public companion object : WasiType {
        public override val valueType: ValueType = ValueType.I32
    }
}