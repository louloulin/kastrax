package ai.kastrax.codebase.search

import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * 搜索结果高亮配置
 *
 * @property contextLines 上下文行数
 * @property maxHighlights 最大高亮数量
 * @property highlightPrefix 高亮前缀
 * @property highlightSuffix 高亮后缀
 * @property truncateLineLength 截断行长度
 */
data class SearchResultHighlighterConfig(
    val contextLines: Int = 2,
    val maxHighlights: Int = 3,
    val highlightPrefix: String = "**",
    val highlightSuffix: String = "**",
    val truncateLineLength: Int = 120
)

/**
 * 高亮信息
 *
 * @property lineNumber 行号
 * @property columnStart 列开始
 * @property columnEnd 列结束
 * @property lineContent 行内容
 * @property beforeContext 前置上下文
 * @property afterContext 后置上下文
 */
data class HighlightInfo(
    val lineNumber: Int,
    val columnStart: Int,
    val columnEnd: Int,
    val lineContent: String,
    val beforeContext: List<String> = emptyList(),
    val afterContext: List<String> = emptyList()
)

/**
 * 高亮结果
 *
 * @property element 代码元素
 * @property highlights 高亮信息列表
 * @property score 分数
 */
data class HighlightResult(
    val element: CodeElement,
    val highlights: List<HighlightInfo>,
    val score: Double
)

/**
 * 搜索结果高亮器
 *
 * 为搜索结果添加高亮信息
 *
 * @property config 配置
 */
class SearchResultHighlighter(
    private val config: SearchResultHighlighterConfig = SearchResultHighlighterConfig()
) {
    /**
     * 高亮搜索结果
     *
     * @param results 检索结果列表
     * @param query 查询字符串
     * @return 高亮结果列表
     */
    fun highlight(results: List<RetrievalResult>, query: String): List<HighlightResult> {
        logger.info { "为 ${results.size} 个搜索结果添加高亮信息" }

        val queryTerms = query.lowercase().split(Regex("\\s+"))
            .filter { it.length > 1 } // 过滤掉太短的词

        if (queryTerms.isEmpty()) {
            return results.map { HighlightResult(it.element, emptyList(), it.score) }
        }

        return results.map { result ->
            try {
                val element = result.element
                val filePath = element.location.filePath
                val highlights = findHighlights(filePath, element, queryTerms)
                HighlightResult(element, highlights, result.score)
            } catch (e: Exception) {
                logger.error(e) { "为搜索结果添加高亮信息时发生错误: ${e.message}" }
                HighlightResult(result.element, emptyList(), result.score)
            }
        }
    }

    /**
     * 查找高亮
     *
     * @param filePath 文件路径
     * @param element 代码元素
     * @param queryTerms 查询词列表
     * @return 高亮信息列表
     */
    private fun findHighlights(
        filePath: String,
        element: CodeElement,
        queryTerms: List<String>
    ): List<HighlightInfo> {
        try {
            // 读取文件内容
            val path = Paths.get(filePath)
            if (!Files.exists(path)) {
                logger.warn { "文件不存在: $filePath" }
                return emptyList()
            }

            val fileContent = Files.readString(path)
            val lines = fileContent.lines()

            // 确定搜索范围
            val startLine = maxOf(0, element.location.startLine - 1)
            val endLine = minOf(lines.size - 1, element.location.endLine - 1)

            // 查找匹配
            val highlights = mutableListOf<HighlightInfo>()
            for (i in startLine..endLine) {
                val line = lines[i]
                val lineLower = line.lowercase()

                for (term in queryTerms) {
                    var index = lineLower.indexOf(term)
                    while (index >= 0) {
                        // 获取上下文
                        val beforeContext = (i - config.contextLines until i)
                            .filter { it >= 0 }
                            .map { lines[it] }
                        val afterContext = (i + 1..i + config.contextLines)
                            .filter { it < lines.size }
                            .map { lines[it] }

                        // 截断行内容
                        val truncatedLine = if (line.length > config.truncateLineLength) {
                            val start = maxOf(0, index - 40)
                            val end = minOf(line.length, index + term.length + 40)
                            if (start > 0) "..." + line.substring(start, end) + "..." else line.substring(start, end) + "..."
                        } else {
                            line
                        }

                        // 创建高亮信息
                        highlights.add(
                            HighlightInfo(
                                lineNumber = i + 1,
                                columnStart = index,
                                columnEnd = index + term.length,
                                lineContent = truncatedLine,
                                beforeContext = beforeContext,
                                afterContext = afterContext
                            )
                        )

                        // 查找下一个匹配
                        index = lineLower.indexOf(term, index + 1)

                        // 限制高亮数量
                        if (highlights.size >= config.maxHighlights) {
                            return highlights
                        }
                    }
                }
            }

            return highlights
        } catch (e: Exception) {
            logger.error(e) { "查找高亮时发生错误: ${e.message}" }
            return emptyList()
        }
    }

    /**
     * 生成高亮文本
     *
     * @param highlightResult 高亮结果
     * @return 高亮文本
     */
    fun generateHighlightedText(highlightResult: HighlightResult): String {
        val sb = StringBuilder()

        // 添加元素信息
        val element = highlightResult.element
        sb.appendLine("${element.type.name} ${element.name} (${element.location.filePath}:${element.location.startLine})")
        sb.appendLine()

        // 添加高亮信息
        for (highlight in highlightResult.highlights) {
            // 添加前置上下文
            for (context in highlight.beforeContext) {
                sb.appendLine("  ${context}")
            }

            // 添加高亮行
            val line = highlight.lineContent
            val start = highlight.columnStart
            val end = highlight.columnEnd

            if (start >= 0 && end <= line.length) {
                val prefix = line.substring(0, start)
                val match = line.substring(start, end)
                val suffix = line.substring(end)

                sb.append("* ")
                sb.append(prefix)
                sb.append(config.highlightPrefix)
                sb.append(match)
                sb.append(config.highlightSuffix)
                sb.appendLine(suffix)
            } else {
                sb.appendLine("* ${line}")
            }

            // 添加后置上下文
            for (context in highlight.afterContext) {
                sb.appendLine("  ${context}")
            }

            sb.appendLine()
        }

        return sb.toString()
    }

    /**
     * 生成HTML高亮文本
     *
     * @param highlightResult 高亮结果
     * @return HTML高亮文本
     */
    fun generateHighlightedHtml(highlightResult: HighlightResult): String {
        val sb = StringBuilder()

        // 添加元素信息
        val element = highlightResult.element
        sb.appendLine("<div class=\"search-result\">")
        sb.appendLine("  <div class=\"result-header\">")
        sb.appendLine("    <span class=\"result-type\">${element.type.name}</span>")
        sb.appendLine("    <span class=\"result-name\">${element.name}</span>")
        sb.appendLine("    <span class=\"result-location\">${element.location.filePath}:${element.location.startLine}</span>")
        sb.appendLine("  </div>")

        // 添加高亮信息
        sb.appendLine("  <div class=\"result-highlights\">")
        for (highlight in highlightResult.highlights) {
            sb.appendLine("    <div class=\"highlight\">")

            // 添加前置上下文
            if (highlight.beforeContext.isNotEmpty()) {
                sb.appendLine("      <div class=\"context before-context\">")
                for (context in highlight.beforeContext) {
                    sb.appendLine("        <div class=\"context-line\">${escapeHtml(context)}</div>")
                }
                sb.appendLine("      </div>")
            }

            // 添加高亮行
            val line = highlight.lineContent
            val start = highlight.columnStart
            val end = highlight.columnEnd

            sb.appendLine("      <div class=\"highlight-line\">")
            if (start >= 0 && end <= line.length) {
                val prefix = line.substring(0, start)
                val match = line.substring(start, end)
                val suffix = line.substring(end)

                sb.append("        ")
                sb.append(escapeHtml(prefix))
                sb.append("<span class=\"highlight-match\">")
                sb.append(escapeHtml(match))
                sb.append("</span>")
                sb.appendLine(escapeHtml(suffix))
            } else {
                sb.appendLine("        ${escapeHtml(line)}")
            }
            sb.appendLine("      </div>")

            // 添加后置上下文
            if (highlight.afterContext.isNotEmpty()) {
                sb.appendLine("      <div class=\"context after-context\">")
                for (context in highlight.afterContext) {
                    sb.appendLine("        <div class=\"context-line\">${escapeHtml(context)}</div>")
                }
                sb.appendLine("      </div>")
            }

            sb.appendLine("    </div>")
        }
        sb.appendLine("  </div>")
        sb.appendLine("</div>")

        return sb.toString()
    }

    /**
     * 转义HTML
     *
     * @param text 文本
     * @return 转义后的文本
     */
    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
