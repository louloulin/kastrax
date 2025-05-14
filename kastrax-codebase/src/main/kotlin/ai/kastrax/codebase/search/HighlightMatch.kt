package ai.kastrax.codebase.search

/**
 * 高亮匹配
 *
 * @property lineNumber 行号
 * @property lineContent 行内容
 * @property startColumn 开始列
 * @property endColumn 结束列
 * @property matchText 匹配文本
 */
data class HighlightMatch(
    val lineNumber: Int,
    val lineContent: String,
    val startColumn: Int,
    val endColumn: Int,
    val matchText: String
)
