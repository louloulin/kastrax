package ai.kastrax.codebase.search

import ai.kastrax.codebase.retrieval.model.RetrievalResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * 搜索命令配置
 *
 * @property defaultSearchType 默认搜索类型
 * @property defaultLimit 默认限制结果数量
 * @property defaultMinScore 默认最小分数
 * @property contextLines 上下文行数
 * @property formatAsMarkdown 是否格式化为 Markdown
 * @property includeHighlights 是否包含高亮
 * @property includePagination 是否包含分页
 */
data class SearchCommandConfig(
    val defaultSearchType: SearchType = SearchType.HYBRID,
    val defaultLimit: Int = 10,
    val defaultMinScore: Double = 0.5,
    val contextLines: Int = 2,
    val formatAsMarkdown: Boolean = true,
    val includeHighlights: Boolean = true,
    val includePagination: Boolean = false
)

/**
 * 搜索命令
 *
 * 用于 AI 助手集成的搜索命令
 *
 * @property searchFacade 搜索门面
 * @property config 配置
 */
class SearchCommand(
    private val searchFacade: SearchFacade,
    private val config: SearchCommandConfig = SearchCommandConfig()
) {
    /**
     * 执行搜索
     *
     * @param query 查询字符串
     * @param searchType 搜索类型
     * @param basePath 基础路径
     * @param options 搜索选项
     * @return 搜索结果
     */
    suspend fun execute(
        query: String,
        searchType: SearchType = config.defaultSearchType,
        basePath: String? = null,
        options: Map<String, Any> = emptyMap()
    ): String = withContext(Dispatchers.Default) {
        logger.info { "执行搜索命令: $query (类型: $searchType)" }

        try {
            // 确定搜索路径
            val paths = if (basePath != null) {
                listOf(Paths.get(basePath))
            } else {
                emptyList()
            }

            // 创建搜索请求
            val request = SearchRequest(
                query = query,
                paths = paths,
                type = searchType,
                options = options + mapOf(
                    "limit" to (options["limit"] as? Int ?: config.defaultLimit),
                    "minScore" to (options["minScore"] as? Double ?: config.defaultMinScore),
                    "contextLines" to (options["contextLines"] as? Int ?: config.contextLines)
                )
            )

            // 执行搜索
            val response = searchFacade.search(request)

            // 格式化结果
            return@withContext formatResults(response)
        } catch (e: Exception) {
            logger.error(e) { "搜索命令执行失败: ${e.message}" }
            return@withContext "搜索失败: ${e.message}"
        }
    }

    /**
     * 格式化结果
     *
     * @param response 搜索响应
     * @return 格式化后的结果
     */
    private fun formatResults(response: SearchResponse): String {
        val results = response.results
        val highlightResults = response.highlightResults
        val pagedResult = response.pagedResult

        // 如果没有结果，返回提示信息
        if (results.isEmpty()) {
            return "未找到匹配的结果。"
        }

        val sb = StringBuilder()

        // 添加标题
        sb.appendLine("搜索结果: ${response.query}")
        sb.appendLine("找到 ${results.size} 个匹配项")
        sb.appendLine()

        // 格式化为 Markdown
        if (config.formatAsMarkdown) {
            // 添加高亮结果
            if (config.includeHighlights && highlightResults.isNotEmpty() && resultHighlighter != null) {
                for (highlightResult in highlightResults) {
                    sb.appendLine("### ${highlightResult.element.name} (${highlightResult.element.type})")
                    sb.appendLine("文件: ${highlightResult.element.location.filePath}")
                    sb.appendLine("位置: 第 ${highlightResult.element.location.startLine} 行")
                    sb.appendLine("相关度: ${String.format("%.2f", highlightResult.score)}")
                    sb.appendLine()
                    sb.appendLine("```")
                    sb.appendLine(resultHighlighter.generateHighlightedText(highlightResult))
                    sb.appendLine("```")
                    sb.appendLine()
                }
            } else {
                // 添加普通结果
                for (result in results) {
                    sb.appendLine("### ${result.element.name} (${result.element.type})")
                    sb.appendLine("文件: ${result.element.location.filePath}")
                    sb.appendLine("位置: 第 ${result.element.location.startLine} 行")
                    sb.appendLine("相关度: ${String.format("%.2f", result.score)}")
                    sb.appendLine()
                    sb.appendLine("```")
                    sb.appendLine(result.element.content)
                    sb.appendLine("```")
                    sb.appendLine()
                }
            }

            // 添加分页信息
            if (config.includePagination && pagedResult != null && resultPaginator != null) {
                sb.appendLine("---")
                sb.appendLine(resultPaginator.generatePaginationInfo(pagedResult))
            }
        } else {
            // 纯文本格式
            for (result in results) {
                sb.appendLine("${result.element.name} (${result.element.type})")
                sb.appendLine("文件: ${result.element.location.filePath}")
                sb.appendLine("位置: 第 ${result.element.location.startLine} 行")
                sb.appendLine("相关度: ${String.format("%.2f", result.score)}")
                sb.appendLine()
                sb.appendLine(result.element.content)
                sb.appendLine()
            }
        }

        return sb.toString()
    }

    // 高亮器和分页器
    private val resultHighlighter = if (config.includeHighlights) SearchResultHighlighter() else null
    private val resultPaginator = if (config.includePagination) SearchResultPaginator() else null
}
