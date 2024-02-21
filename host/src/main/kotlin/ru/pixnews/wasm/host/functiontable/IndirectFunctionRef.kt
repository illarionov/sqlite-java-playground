package ru.pixnews.wasm.host.functiontable

sealed interface IndirectFunctionRef{
    val ref: Int

    @JvmInline
    public value class FuncRef(
        override val ref: Int
    ) : IndirectFunctionRef

    @JvmInline
    public value class ExternalRef(
        override val ref: Int
    ) : IndirectFunctionRef
}