package ru.pixnews.wasm.host.emscrypten

class AssertionFailed(
    val condition: String?,
    val filename: String?,
    val line: Int,
    val func: String?,
) : RuntimeException(
    formatErrMsg(
        condition,
        filename,
        line,
        func
    )
) {
    private companion object {
        fun formatErrMsg(
            condition: String?,
            filename: String?,
            line: Int,
            func: String?,
        ): String = buildString {
            append("Assertion failed: ")
            append(condition ?: "``")
            append(",  at ")
            listOf(
                filename ?: "unknown filename",
                line.toString(),
                func ?: "unknown function"
            ).joinTo(this, ", ", prefix = "[", postfix = "]")
        }
    }
}