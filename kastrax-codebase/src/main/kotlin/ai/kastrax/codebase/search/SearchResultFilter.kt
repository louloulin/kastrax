package ai.kastrax.codebase.search

import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Visibility
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * 搜索结果过滤器配置
 *
 * @property enableTypeFiltering 是否启用类型过滤
 * @property enableVisibilityFiltering 是否启用可见性过滤
 * @property enablePathFiltering 是否启用路径过滤
 * @property enableScoreFiltering 是否启用分数过滤
 * @property defaultMinScore 默认最小分数
 */
data class SearchResultFilterConfig(
    val enableTypeFiltering: Boolean = true,
    val enableVisibilityFiltering: Boolean = true,
    val enablePathFiltering: Boolean = true,
    val enableScoreFiltering: Boolean = true,
    val defaultMinScore: Double = 0.5
)

/**
 * 过滤条件
 *
 * @property types 类型列表
 * @property visibilities 可见性列表
 * @property includePaths 包含路径列表
 * @property excludePaths 排除路径列表
 * @property minScore 最小分数
 * @property maxScore 最大分数
 * @property includePatterns 包含模式列表
 * @property excludePatterns 排除模式列表
 */
data class FilterCriteria(
    val types: Set<CodeElementType>? = null,
    val visibilities: Set<Visibility>? = null,
    val includePaths: List<String>? = null,
    val excludePaths: List<String>? = null,
    val minScore: Double? = null,
    val maxScore: Double? = null,
    val includePatterns: List<Regex>? = null,
    val excludePatterns: List<Regex>? = null
)

/**
 * 搜索结果过滤器
 *
 * 对搜索结果进行过滤
 *
 * @property config 配置
 */
class SearchResultFilter(
    private val config: SearchResultFilterConfig = SearchResultFilterConfig()
) {
    /**
     * 过滤检索结果
     *
     * @param results 检索结果列表
     * @param criteria 过滤条件
     * @return 过滤后的检索结果列表
     */
    fun filterResults(
        results: List<RetrievalResult>,
        criteria: FilterCriteria
    ): List<RetrievalResult> {
        logger.info { "过滤 ${results.size} 个搜索结果" }

        return results.filter { result ->
            val element = result.element
            val score = result.score

            // 类型过滤
            if (config.enableTypeFiltering && criteria.types != null && criteria.types.isNotEmpty()) {
                if (element.type !in criteria.types) {
                    return@filter false
                }
            }

            // 可见性过滤
            if (config.enableVisibilityFiltering && criteria.visibilities != null && criteria.visibilities.isNotEmpty()) {
                if (element.visibility !in criteria.visibilities) {
                    return@filter false
                }
            }

            // 路径过滤
            if (config.enablePathFiltering) {
                // 包含路径
                if (criteria.includePaths != null && criteria.includePaths.isNotEmpty()) {
                    val filePath = element.location.filePath
                    if (criteria.includePaths.none { filePath.contains(it) }) {
                        return@filter false
                    }
                }

                // 排除路径
                if (criteria.excludePaths != null && criteria.excludePaths.isNotEmpty()) {
                    val filePath = element.location.filePath
                    if (criteria.excludePaths.any { filePath.contains(it) }) {
                        return@filter false
                    }
                }
            }

            // 分数过滤
            if (config.enableScoreFiltering) {
                // 最小分数
                if (criteria.minScore != null && score < criteria.minScore) {
                    return@filter false
                }

                // 最大分数
                if (criteria.maxScore != null && score > criteria.maxScore) {
                    return@filter false
                }
            }

            // 模式过滤
            // 包含模式
            if (criteria.includePatterns != null && criteria.includePatterns.isNotEmpty()) {
                val text = "${element.name} ${element.qualifiedName} ${element.documentation}"
                if (criteria.includePatterns.none { it.containsMatchIn(text) }) {
                    return@filter false
                }
            }

            // 排除模式
            if (criteria.excludePatterns != null && criteria.excludePatterns.isNotEmpty()) {
                val text = "${element.name} ${element.qualifiedName} ${element.documentation}"
                if (criteria.excludePatterns.any { it.containsMatchIn(text) }) {
                    return@filter false
                }
            }

            true
        }
    }

    /**
     * 过滤高亮结果
     *
     * @param results 高亮结果列表
     * @param criteria 过滤条件
     * @return 过滤后的高亮结果列表
     */
    fun filterHighlightResults(
        results: List<HighlightResult>,
        criteria: FilterCriteria
    ): List<HighlightResult> {
        logger.info { "过滤 ${results.size} 个高亮结果" }

        return results.filter { result ->
            val element = result.element
            val score = result.score

            // 类型过滤
            if (config.enableTypeFiltering && criteria.types != null && criteria.types.isNotEmpty()) {
                if (element.type !in criteria.types) {
                    return@filter false
                }
            }

            // 可见性过滤
            if (config.enableVisibilityFiltering && criteria.visibilities != null && criteria.visibilities.isNotEmpty()) {
                if (element.visibility !in criteria.visibilities) {
                    return@filter false
                }
            }

            // 路径过滤
            if (config.enablePathFiltering) {
                // 包含路径
                if (criteria.includePaths != null && criteria.includePaths.isNotEmpty()) {
                    val filePath = element.location.filePath
                    if (criteria.includePaths.none { filePath.contains(it) }) {
                        return@filter false
                    }
                }

                // 排除路径
                if (criteria.excludePaths != null && criteria.excludePaths.isNotEmpty()) {
                    val filePath = element.location.filePath
                    if (criteria.excludePaths.any { filePath.contains(it) }) {
                        return@filter false
                    }
                }
            }

            // 分数过滤
            if (config.enableScoreFiltering) {
                // 最小分数
                if (criteria.minScore != null && score < criteria.minScore) {
                    return@filter false
                }

                // 最大分数
                if (criteria.maxScore != null && score > criteria.maxScore) {
                    return@filter false
                }
            }

            // 模式过滤
            // 包含模式
            if (criteria.includePatterns != null && criteria.includePatterns.isNotEmpty()) {
                val text = "${element.name} ${element.qualifiedName} ${element.documentation}"
                if (criteria.includePatterns.none { it.containsMatchIn(text) }) {
                    return@filter false
                }
            }

            // 排除模式
            if (criteria.excludePatterns != null && criteria.excludePatterns.isNotEmpty()) {
                val text = "${element.name} ${element.qualifiedName} ${element.documentation}"
                if (criteria.excludePatterns.any { it.containsMatchIn(text) }) {
                    return@filter false
                }
            }

            true
        }
    }

    /**
     * 过滤代码元素
     *
     * @param elements 代码元素列表
     * @param criteria 过滤条件
     * @return 过滤后的代码元素列表
     */
    fun filterCodeElements(
        elements: List<CodeElement>,
        criteria: FilterCriteria
    ): List<CodeElement> {
        logger.info { "过滤 ${elements.size} 个代码元素" }

        return elements.filter { element ->
            // 类型过滤
            if (config.enableTypeFiltering && criteria.types != null && criteria.types.isNotEmpty()) {
                if (element.type !in criteria.types) {
                    return@filter false
                }
            }

            // 可见性过滤
            if (config.enableVisibilityFiltering && criteria.visibilities != null && criteria.visibilities.isNotEmpty()) {
                if (element.visibility !in criteria.visibilities) {
                    return@filter false
                }
            }

            // 路径过滤
            if (config.enablePathFiltering) {
                // 包含路径
                if (criteria.includePaths != null && criteria.includePaths.isNotEmpty()) {
                    val filePath = element.location.filePath
                    if (criteria.includePaths.none { filePath.contains(it) }) {
                        return@filter false
                    }
                }

                // 排除路径
                if (criteria.excludePaths != null && criteria.excludePaths.isNotEmpty()) {
                    val filePath = element.location.filePath
                    if (criteria.excludePaths.any { filePath.contains(it) }) {
                        return@filter false
                    }
                }
            }

            // 模式过滤
            // 包含模式
            if (criteria.includePatterns != null && criteria.includePatterns.isNotEmpty()) {
                val text = "${element.name} ${element.qualifiedName} ${element.documentation}"
                if (criteria.includePatterns.none { it.containsMatchIn(text) }) {
                    return@filter false
                }
            }

            // 排除模式
            if (criteria.excludePatterns != null && criteria.excludePatterns.isNotEmpty()) {
                val text = "${element.name} ${element.qualifiedName} ${element.documentation}"
                if (criteria.excludePatterns.any { it.containsMatchIn(text) }) {
                    return@filter false
                }
            }

            true
        }
    }

    /**
     * 创建过滤条件
     *
     * @param types 类型列表
     * @param visibilities 可见性列表
     * @param includePaths 包含路径列表
     * @param excludePaths 排除路径列表
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @param includePatterns 包含模式列表
     * @param excludePatterns 排除模式列表
     * @return 过滤条件
     */
    fun createFilterCriteria(
        types: Set<CodeElementType>? = null,
        visibilities: Set<Visibility>? = null,
        includePaths: List<String>? = null,
        excludePaths: List<String>? = null,
        minScore: Double? = null,
        maxScore: Double? = null,
        includePatterns: List<String>? = null,
        excludePatterns: List<String>? = null
    ): FilterCriteria {
        // 编译正则表达式
        val includeRegexes = includePatterns?.map { Regex(it, RegexOption.IGNORE_CASE) }
        val excludeRegexes = excludePatterns?.map { Regex(it, RegexOption.IGNORE_CASE) }

        return FilterCriteria(
            types = types,
            visibilities = visibilities,
            includePaths = includePaths,
            excludePaths = excludePaths,
            minScore = minScore ?: config.defaultMinScore,
            maxScore = maxScore,
            includePatterns = includeRegexes,
            excludePatterns = excludeRegexes
        )
    }
}
