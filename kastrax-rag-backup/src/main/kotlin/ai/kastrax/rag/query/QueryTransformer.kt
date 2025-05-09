package ai.kastrax.rag.query

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 查询转换器接口，用于转换和增强用户查询。
 */
interface QueryTransformer {
    /**
     * 转换查询文本。
     *
     * @param query 原始查询文本
     * @return 转换后的查询文本
     */
    suspend fun transform(query: String): String

    /**
     * 转换查询文本并返回多个变体。
     *
     * @param query 原始查询文本
     * @return 转换后的查询文本列表
     */
    suspend fun transformToMultiple(query: String): List<String> = listOf(transform(query))
}

/**
 * 查询转换器配置。
 *
 * @property enabled 是否启用查询转换
 */
data class QueryTransformerConfig(
    val enabled: Boolean = true
)

/**
 * 组合查询转换器，按顺序应用多个转换器。
 *
 * @property transformers 查询转换器列表
 */
class CompositeQueryTransformer(
    private val transformers: List<QueryTransformer>
) : QueryTransformer {

    override suspend fun transform(query: String): String {
        var currentQuery = query
        for (transformer in transformers) {
            currentQuery = transformer.transform(currentQuery)
        }
        return currentQuery
    }

    override suspend fun transformToMultiple(query: String): List<String> {
        // 从第一个转换器开始，依次应用每个转换器，并收集所有变体
        var queries = listOf(query)

        for (transformer in transformers) {
            queries = queries.flatMap { transformer.transformToMultiple(it) }
        }

        return queries.distinct()
    }
}

/**
 * 空查询转换器，不进行任何转换。
 */
class NoOpQueryTransformer : QueryTransformer {
    override suspend fun transform(query: String): String = query
}

/**
 * 查询规范化转换器，对查询进行基本的清理和规范化。
 */
class NormalizationQueryTransformer : QueryTransformer {
    override suspend fun transform(query: String): String {
        // 移除多余的空格
        var normalizedQuery = query.trim().replace(Regex("\\s+"), " ")

        // 移除常见的标点符号
        normalizedQuery = normalizedQuery.replace(Regex("[,.;:!?()\\[\\]{}'\"]"), " ")

        // 再次移除多余的空格
        normalizedQuery = normalizedQuery.replace(Regex("\\s+"), " ").trim()

        // 转换为小写
        normalizedQuery = normalizedQuery.lowercase()

        return normalizedQuery
    }
}

/**
 * 查询扩展转换器，通过添加同义词或相关术语来扩展查询。
 *
 * @property synonymMap 同义词映射
 */
class SynonymQueryTransformer(
    private val synonymMap: Map<String, List<String>> = emptyMap()
) : QueryTransformer {

    override suspend fun transform(query: String): String {
        val words = query.split(Regex("\\s+"))
        val expandedWords = words.map { word ->
            val synonyms = synonymMap[word.lowercase()]
            if (synonyms != null && synonyms.isNotEmpty()) {
                "$word (${synonyms.joinToString(" OR ")})"
            } else {
                word
            }
        }

        return expandedWords.joinToString(" ")
    }

    override suspend fun transformToMultiple(query: String): List<String> {
        val words = query.split(Regex("\\s+"))
        val wordVariants = words.map { word ->
            val synonyms = synonymMap[word.lowercase()] ?: emptyList()
            listOf(word) + synonyms
        }

        // 生成所有可能的组合
        return generateCombinations(wordVariants).map { it.joinToString(" ") }
    }

    private fun generateCombinations(wordVariants: List<List<String>>): List<List<String>> {
        if (wordVariants.isEmpty()) {
            return listOf(emptyList())
        }

        val result = mutableListOf<List<String>>()
        val restCombinations = generateCombinations(wordVariants.drop(1))

        for (variant in wordVariants.first()) {
            for (combination in restCombinations) {
                result.add(listOf(variant) + combination)
            }
        }

        return result
    }
}

/**
 * 查询分解转换器，将复杂查询分解为多个简单查询。
 */
class DecompositionQueryTransformer : QueryTransformer {

    override suspend fun transform(query: String): String {
        // 默认实现返回原始查询
        return query
    }

    override suspend fun transformToMultiple(query: String): List<String> {
        // 简单的分解策略：按句子分割
        val sentences = query.split(Regex("[.!?]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // 如果只有一个句子，尝试按逗号或分号分割
        if (sentences.size <= 1) {
            val phrases = query.split(Regex("[,;]"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            return if (phrases.size > 1) phrases else listOf(query)
        }

        return sentences
    }
}

/**
 * 查询重写转换器，使用 LLM 重写查询以提高检索效果。
 *
 * @property llmClient LLM 客户端
 * @property systemPrompt 系统提示
 */
class LLMQueryTransformer(
    private val llmClient: (String) -> String,
    private val systemPrompt: String = "你是一个查询重写专家。你的任务是重写用户的查询，使其更加清晰、具体和有效，以便从文档检索系统中获取最相关的信息。"
) : QueryTransformer {

    override suspend fun transform(query: String): String {
        try {
            val prompt = """
                $systemPrompt

                原始查询: $query

                请重写这个查询，使其更加有效。只返回重写后的查询，不要包含任何解释或其他文本。
            """.trimIndent()

            val response = llmClient(prompt).trim()

            logger.debug { "原始查询: $query" }
            logger.debug { "重写查询: $response" }

            return response
        } catch (e: Exception) {
            logger.error(e) { "LLM 查询重写失败" }
            return query
        }
    }

    override suspend fun transformToMultiple(query: String): List<String> {
        try {
            val prompt = """
                $systemPrompt

                原始查询: $query

                请生成 3 个不同的查询变体，每个变体都应该能够帮助检索与原始查询相关的信息。
                每个变体应该从不同的角度或使用不同的术语来表达相同的信息需求。
                只返回查询变体，每行一个，不要包含任何解释或其他文本。
            """.trimIndent()

            val response = llmClient(prompt).trim()

            // 解析响应，提取查询变体
            val variants = response.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(3)

            logger.debug { "原始查询: $query" }
            logger.debug { "查询变体: $variants" }

            return listOf(query) + variants
        } catch (e: Exception) {
            logger.error(e) { "LLM 查询变体生成失败" }
            return listOf(query)
        }
    }
}
