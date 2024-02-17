package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8

/**
 * File or memory access pattern advisory information.
 */
public enum class Advice(
    val id: Int
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

    companion object : WasiTypename {
        override val webAssemblyValueType: WebAssemblyValueType = U8
    }
}