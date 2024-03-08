package ru.pixnews.gradle.wasm.ext

import java.util.Locale

internal fun String.capitalizeAscii() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(
        Locale.ROOT
    ) else it.toString()
}
