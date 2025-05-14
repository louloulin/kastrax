package ai.kastrax.codebase.retrieval

import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 代码相关性排序器配置
 *
 * @property typeWeights 类型权重
 * @property documentationBoost 文档注释提升
 * @property nameMatchBoost 名称匹配提升
 * @property qualifiedNameMatchBoost 限定名匹配提升
 * @property publicVisibilityBoost 公共可见性提升
 * @property recentlyModifiedBoost 最近修改提升
 */
data class CodeRelevanceRankerConfig(
    val typeWeights: Map<CodeElementType, Double> = mapOf(
        CodeElementType.CLASS to 1.2,
        CodeElementType.INTERFACE to 1.2,
        CodeElementType.METHOD to 1.1,
        CodeElementType.FUNCTION to 1.1,
        CodeElementType.FIELD to 0.9,
        CodeElementType.PROPERTY to 0.9,
        CodeElementType.FILE to 0.8,
        CodeElementType.PACKAGE to 0.7,
        CodeElementType.IMPORT to 0.5,
        CodeElementType.PARAMETER to 0.5,
        CodeElementType.LOCAL_VARIABLE to 0.5,
        CodeElementType.COMMENT to 0.3
    ),
    val documentationBoost: Double = 1.1,
    val nameMatchBoost: Double = 1.3,
    val qualifiedNameMatchBoost: Double = 1.2,
    val publicVisibilityBoost: Double = 1.1,
    val recentlyModifiedBoost: Double = 1.05
)

/**
 * 代码相关性排序器
 *
 * 对检索结果进行相关性排序
 *
 * @property config 配置
 */
class CodeRelevanceRanker(
    private val config: CodeRelevanceRankerConfig = CodeRelevanceRankerConfig()
) {
    /**
     * 对检索结果进行排序
     *
     * @param results 检索结果列表
     * @param query 查询字符串
     * @return 排序后的检索结果列表
     */
    fun rankResults(results: List<RetrievalResult>, query: String): List<RetrievalResult> {
        if (results.isEmpty()) {
            return emptyList()
        }
        
        val queryTerms = query.lowercase().split(Regex("\\s+"))
        
        // 计算每个结果的相关性分数
        val scoredResults = results.map { result ->
            val element = result.element
            var score = result.score
            
            // 应用类型权重
            val typeWeight = config.typeWeights[element.type] ?: 1.0
            score *= typeWeight
            
            // 应用文档注释提升
            if (element.documentation.isNotEmpty()) {
                score *= config.documentationBoost
            }
            
            // 应用名称匹配提升
            if (queryTerms.any { element.name.lowercase().contains(it) }) {
                score *= config.nameMatchBoost
            }
            
            // 应用限定名匹配提升
            if (queryTerms.any { element.qualifiedName.lowercase().contains(it) }) {
                score *= config.qualifiedNameMatchBoost
            }
            
            // 应用公共可见性提升
            if (element.visibility.name.lowercase() == "public") {
                score *= config.publicVisibilityBoost
            }
            
            // 应用最近修改提升
            val lastModified = element.metadata["lastModified"] as? Long
            if (lastModified != null) {
                val currentTime = System.currentTimeMillis()
                val daysSinceModified = (currentTime - lastModified) / (1000 * 60 * 60 * 24)
                if (daysSinceModified < 7) {
                    score *= config.recentlyModifiedBoost
                }
            }
            
            result.copy(score = score)
        }
        
        // 按分数降序排序
        return scoredResults.sortedByDescending { it.score }
    }
    
    /**
     * 对检索结果进行分组排序
     *
     * @param results 检索结果列表
     * @param query 查询字符串
     * @param groupByType 是否按类型分组
     * @return 排序后的检索结果列表
     */
    fun rankAndGroupResults(
        results: List<RetrievalResult>,
        query: String,
        groupByType: Boolean = true
    ): List<RetrievalResult> {
        if (results.isEmpty()) {
            return emptyList()
        }
        
        val rankedResults = rankResults(results, query)
        
        if (!groupByType) {
            return rankedResults
        }
        
        // 按类型分组并保持每组内的排序
        val groupedResults = rankedResults.groupBy { it.element.type }
        
        // 按类型权重排序组，然后合并结果
        val typeWeights = config.typeWeights
        val sortedGroups = groupedResults.entries.sortedByDescending { 
            typeWeights[it.key] ?: 1.0
        }
        
        return sortedGroups.flatMap { it.value }
    }
    
    /**
     * 对检索结果进行去重
     *
     * @param results 检索结果列表
     * @return 去重后的检索结果列表
     */
    fun deduplicateResults(results: List<RetrievalResult>): List<RetrievalResult> {
        if (results.isEmpty()) {
            return emptyList()
        }
        
        val seen = mutableSetOf<String>()
        val deduplicated = mutableListOf<RetrievalResult>()
        
        for (result in results) {
            val element = result.element
            val key = element.id
            
            if (key !in seen) {
                seen.add(key)
                deduplicated.add(result)
            }
        }
        
        return deduplicated
    }
    
    /**
     * 对检索结果进行多样性排序
     *
     * @param results 检索结果列表
     * @param diversityFactor 多样性因子
     * @return 多样性排序后的检索结果列表
     */
    fun diversifyResults(
        results: List<RetrievalResult>,
        diversityFactor: Double = 0.3
    ): List<RetrievalResult> {
        if (results.size <= 1) {
            return results
        }
        
        val diversified = mutableListOf<RetrievalResult>()
        val remaining = results.toMutableList()
        
        // 添加第一个结果
        diversified.add(remaining.removeAt(0))
        
        while (remaining.isNotEmpty()) {
            // 计算每个剩余结果与已选结果的多样性分数
            val diversityScores = remaining.map { candidate ->
                val element = candidate.element
                
                // 计算与已选结果的相似度
                val similarities = diversified.map { selected ->
                    calculateElementSimilarity(element, selected.element)
                }
                
                // 多样性分数 = 原始分数 * (1 - 最大相似度 * 多样性因子)
                val maxSimilarity = similarities.maxOrNull() ?: 0.0
                val diversityScore = candidate.score * (1.0 - maxSimilarity * diversityFactor)
                
                candidate to diversityScore
            }
            
            // 选择多样性分数最高的结果
            val (nextResult, _) = diversityScores.maxByOrNull { it.second }!!
            diversified.add(nextResult)
            remaining.remove(nextResult)
        }
        
        return diversified
    }
    
    /**
     * 计算两个代码元素之间的相似度
     *
     * @param element1 代码元素1
     * @param element2 代码元素2
     * @return 相似度
     */
    private fun calculateElementSimilarity(element1: CodeElement, element2: CodeElement): Double {
        var similarity = 0.0
        
        // 如果类型相同，增加相似度
        if (element1.type == element2.type) {
            similarity += 0.3
        }
        
        // 如果在同一个文件中，增加相似度
        if (element1.location.filePath == element2.location.filePath) {
            similarity += 0.4
        }
        
        // 如果有父子关系，增加相似度
        if (element1.parent?.id == element2.id || element2.parent?.id == element1.id) {
            similarity += 0.5
        }
        
        // 如果名称相似，增加相似度
        val nameJaccardSimilarity = calculateJaccardSimilarity(
            element1.name.lowercase(),
            element2.name.lowercase()
        )
        similarity += nameJaccardSimilarity * 0.2
        
        return minOf(similarity, 1.0)
    }
    
    /**
     * 计算Jaccard相似度
     *
     * @param s1 字符串1
     * @param s2 字符串2
     * @return Jaccard相似度
     */
    private fun calculateJaccardSimilarity(s1: String, s2: String): Double {
        val set1 = s1.toCharArray().toSet()
        val set2 = s2.toCharArray().toSet()
        
        val intersection = set1.intersect(set2).size
        val union = set1.union(set2).size
        
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }
}
