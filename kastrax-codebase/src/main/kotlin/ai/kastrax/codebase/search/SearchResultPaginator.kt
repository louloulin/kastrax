package ai.kastrax.codebase.search

import ai.kastrax.codebase.retrieval.model.RetrievalResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 分页配置
 *
 * @property defaultPageSize 默认页大小
 * @property maxPageSize 最大页大小
 */
data class PaginationConfig(
    val defaultPageSize: Int = 10,
    val maxPageSize: Int = 100
)

/**
 * 分页结果
 *
 * @property items 当前页项目
 * @property totalItems 总项目数
 * @property pageNumber 页码
 * @property pageSize 页大小
 * @property totalPages 总页数
 * @property hasNext 是否有下一页
 * @property hasPrevious 是否有上一页
 */
data class PagedResult<T>(
    val items: List<T>,
    val totalItems: Int,
    val pageNumber: Int,
    val pageSize: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * 搜索结果分页器
 *
 * 对搜索结果进行分页处理
 *
 * @property config 配置
 */
class SearchResultPaginator(
    private val config: PaginationConfig = PaginationConfig()
) {
    /**
     * 分页
     *
     * @param results 结果列表
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    fun <T> paginate(
        results: List<T>,
        pageNumber: Int = 1,
        pageSize: Int = config.defaultPageSize
    ): PagedResult<T> {
        logger.debug { "对 ${results.size} 个结果进行分页，页码: $pageNumber, 页大小: $pageSize" }

        // 验证参数
        val validPageNumber = maxOf(1, pageNumber)
        val validPageSize = minOf(config.maxPageSize, maxOf(1, pageSize))

        // 计算分页信息
        val totalItems = results.size
        val totalPages = if (totalItems == 0) 1 else (totalItems + validPageSize - 1) / validPageSize
        val actualPageNumber = minOf(validPageNumber, totalPages)

        // 计算起始和结束索引
        val startIndex = (actualPageNumber - 1) * validPageSize
        val endIndex = minOf(startIndex + validPageSize, totalItems)

        // 获取当前页项目
        val pageItems = if (startIndex < totalItems) {
            results.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return PagedResult(
            items = pageItems,
            totalItems = totalItems,
            pageNumber = actualPageNumber,
            pageSize = validPageSize,
            totalPages = totalPages,
            hasNext = actualPageNumber < totalPages,
            hasPrevious = actualPageNumber > 1
        )
    }

    /**
     * 分页检索结果
     *
     * @param results 检索结果列表
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @return 分页检索结果
     */
    fun paginateRetrievalResults(
        results: List<RetrievalResult>,
        pageNumber: Int = 1,
        pageSize: Int = config.defaultPageSize
    ): PagedResult<RetrievalResult> {
        return paginate(results, pageNumber, pageSize)
    }

    /**
     * 分页高亮结果
     *
     * @param results 高亮结果列表
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @return 分页高亮结果
     */
    fun paginateHighlightResults(
        results: List<HighlightResult>,
        pageNumber: Int = 1,
        pageSize: Int = config.defaultPageSize
    ): PagedResult<HighlightResult> {
        return paginate(results, pageNumber, pageSize)
    }

    /**
     * 生成分页信息
     *
     * @param pagedResult 分页结果
     * @return 分页信息
     */
    fun generatePaginationInfo(pagedResult: PagedResult<*>): String {
        val sb = StringBuilder()
        sb.appendLine("页码: ${pagedResult.pageNumber}/${pagedResult.totalPages}")
        sb.appendLine("项目: ${pagedResult.items.size}/${pagedResult.totalItems}")
        sb.appendLine("页大小: ${pagedResult.pageSize}")
        sb.append("导航: ")

        // 添加上一页链接
        if (pagedResult.hasPrevious) {
            sb.append("[上一页(${pagedResult.pageNumber - 1})] ")
        } else {
            sb.append("[上一页(-)] ")
        }

        // 添加页码链接
        val pageNumbers = generatePageNumbers(pagedResult.pageNumber, pagedResult.totalPages)
        for (page in pageNumbers) {
            if (page == pagedResult.pageNumber) {
                sb.append("[${page}] ")
            } else if (page == -1) {
                sb.append("... ")
            } else {
                sb.append("${page} ")
            }
        }

        // 添加下一页链接
        if (pagedResult.hasNext) {
            sb.append("[下一页(${pagedResult.pageNumber + 1})]")
        } else {
            sb.append("[下一页(-)]")
        }

        return sb.toString()
    }

    /**
     * 生成HTML分页信息
     *
     * @param pagedResult 分页结果
     * @param baseUrl 基础URL
     * @return HTML分页信息
     */
    fun generateHtmlPaginationInfo(pagedResult: PagedResult<*>, baseUrl: String): String {
        val sb = StringBuilder()
        sb.appendLine("<div class=\"pagination\">")
        sb.appendLine("  <div class=\"pagination-info\">")
        sb.appendLine("    <span>页码: ${pagedResult.pageNumber}/${pagedResult.totalPages}</span>")
        sb.appendLine("    <span>项目: ${pagedResult.items.size}/${pagedResult.totalItems}</span>")
        sb.appendLine("    <span>页大小: ${pagedResult.pageSize}</span>")
        sb.appendLine("  </div>")
        sb.appendLine("  <div class=\"pagination-nav\">")

        // 添加上一页链接
        if (pagedResult.hasPrevious) {
            sb.appendLine("    <a href=\"${baseUrl}&page=${pagedResult.pageNumber - 1}&pageSize=${pagedResult.pageSize}\" class=\"pagination-link\">上一页</a>")
        } else {
            sb.appendLine("    <span class=\"pagination-link disabled\">上一页</span>")
        }

        // 添加页码链接
        val pageNumbers = generatePageNumbers(pagedResult.pageNumber, pagedResult.totalPages)
        for (page in pageNumbers) {
            if (page == pagedResult.pageNumber) {
                sb.appendLine("    <span class=\"pagination-link current\">${page}</span>")
            } else if (page == -1) {
                sb.appendLine("    <span class=\"pagination-link ellipsis\">...</span>")
            } else {
                sb.appendLine("    <a href=\"${baseUrl}&page=${page}&pageSize=${pagedResult.pageSize}\" class=\"pagination-link\">${page}</a>")
            }
        }

        // 添加下一页链接
        if (pagedResult.hasNext) {
            sb.appendLine("    <a href=\"${baseUrl}&page=${pagedResult.pageNumber + 1}&pageSize=${pagedResult.pageSize}\" class=\"pagination-link\">下一页</a>")
        } else {
            sb.appendLine("    <span class=\"pagination-link disabled\">下一页</span>")
        }

        sb.appendLine("  </div>")
        sb.appendLine("</div>")

        return sb.toString()
    }

    /**
     * 生成页码
     *
     * @param currentPage 当前页
     * @param totalPages 总页数
     * @param maxVisible 最大可见页码数
     * @return 页码列表
     */
    private fun generatePageNumbers(
        currentPage: Int,
        totalPages: Int,
        maxVisible: Int = 7
    ): List<Int> {
        if (totalPages <= maxVisible) {
            return (1..totalPages).toList()
        }

        val result = mutableListOf<Int>()
        val sidePages = (maxVisible - 3) / 2 // 当前页两侧的页码数

        // 添加第一页
        result.add(1)

        // 添加当前页左侧的页码
        val leftStart = maxOf(2, currentPage - sidePages)
        if (leftStart > 2) {
            result.add(-1) // 省略号
        }
        for (i in leftStart until currentPage) {
            result.add(i)
        }

        // 添加当前页
        result.add(currentPage)

        // 添加当前页右侧的页码
        val rightEnd = minOf(totalPages - 1, currentPage + sidePages)
        for (i in currentPage + 1..rightEnd) {
            result.add(i)
        }
        if (rightEnd < totalPages - 1) {
            result.add(-1) // 省略号
        }

        // 添加最后一页
        if (totalPages > 1) {
            result.add(totalPages)
        }

        return result
    }
}
