package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.wasm.types.ValueType

internal val ValueType.pointer: ValueType
        get() = ValueType.I32
