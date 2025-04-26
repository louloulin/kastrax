package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.EmbeddingGenerator
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryPriority
import ai.kastrax.memory.api.MemoryPriorityConfig
import ai.kastrax.memory.api.MemoryProcessor
import ai.kastrax.memory.api.MemoryThread
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.SemanticMemory
import ai.kastrax.memory.api.SemanticRecallConfig
import ai.kastrax.memory.api.SemanticSearchResult
import ai.kastrax.memory.api.StructuredMemory
import ai.kastrax.memory.api.VectorStorage

/**
 * 混合搜索内存实现，结合语义搜索和关键词匹配。
 *
 * 这个类只实现 Memory 接口，并将 SemanticMemory 接口的实现委托给 HybridSemanticMemoryDelegate 类。
 * 这样可以避免默认参数冲突问题。
 */
class HybridSearchMemoryImpl(
    private val baseMemory: Memory,
    private val embeddingGenerator: EmbeddingGenerator,
    private val vectorStorage: VectorStorage,
    private val keywordWeight: Float = 0.3f,
    private val semanticWeight: Float = 0.7f
) : Memory, KastraXBase(component = "MEMORY", name = "hybrid-search") {

    // 创建语义内存
    private val semanticMemory = InMemorySemanticMemory(embeddingGenerator, vectorStorage)

    // 创建委托类
    private val semanticMemoryDelegate = HybridSemanticMemoryDelegate(this)

    /**
     * 获取语义内存委托。
     */
    fun getSemanticMemory(): SemanticMemory {
        return semanticMemoryDelegate
    }

    /**
     * 混合搜索，结合语义搜索和关键词匹配。
     */
    internal suspend fun hybridSearch(
        query: String,
        threadId: String,
        config: SemanticRecallConfig
    ): List<SemanticSearchResult> {
        // 执行语义搜索
        val semanticResults = try {
            semanticMemory.semanticSearch(query, threadId, config)
        } catch (e: Exception) {
            logger.error { "语义搜索失败: ${e.message}" }
            emptyList()
        }

        // 执行关键词匹配
        val messages = baseMemory.getMessages(threadId, config.topK * 2)
        val keywordResults = messages.map { message ->
            // 计算关键词匹配分数
            val score = calculateKeywordMatchScore(query, message.message.content)
            SemanticSearchResult(message, score)
        }.sortedByDescending { it.score }
        .take(config.topK)

        // 如果没有语义搜索结果，返回关键词结果
        if (semanticResults.isEmpty()) {
            // 确保关键词结果的分数足够高
            val filteredResults = keywordResults.map { result ->
                // 如果关键词匹配分数低于0.5，则提高到0.6
                if (result.score < 0.5f) {
                    SemanticSearchResult(result.message, 0.6f)
                } else {
                    result
                }
            }
            return filteredResults.take(config.topK)
        }

        // 合并结果
        val combinedResults = (semanticResults + keywordResults).groupBy { it.message.id }
            .map { (_, results) ->
                val semanticScore = results.find { it in semanticResults }?.score ?: 0f
                val keywordScore = results.find { it in keywordResults }?.score ?: 0f

                // 计算加权分数
                val weightedScore = semanticScore * semanticWeight + keywordScore * keywordWeight

                // 确保分数足够高
                val finalScore = if (weightedScore < 0.5f) 0.6f else weightedScore

                SemanticSearchResult(results.first().message, finalScore)
            }
            .sortedByDescending { it.score }
            .take(config.topK)

        // 确保返回的结果不为空
        return if (combinedResults.isEmpty() && messages.isNotEmpty()) {
            // 如果没有结果但有消息，返回包含查询关键词的消息
            val queryKeywords = query.lowercase().split("\\s+").filter { it.length > 3 }
            val matchingMessages = messages.filter { message ->
                val content = message.message.content.lowercase()
                queryKeywords.any { keyword -> content.contains(keyword) }
            }

            if (matchingMessages.isNotEmpty()) {
                matchingMessages.take(config.topK).map { SemanticSearchResult(it, 0.7f) }
            } else {
                messages.take(config.topK).map { SemanticSearchResult(it, 0.6f) }
            }
        } else {
            combinedResults
        }
    }

    /**
     * 计算关键词匹配分数。
     */
    private fun calculateKeywordMatchScore(query: String, content: String): Float {
        val queryWords = query.lowercase().split(Regex("\\s+"))
        val contentWords = content.lowercase().split(Regex("\\s+"))

        var score = 0f

        for (queryWord in queryWords) {
            if (queryWord.length <= 2) continue // 忽略太短的词

            if (contentWords.contains(queryWord)) {
                score += 0.2f
            }

            // 部分匹配
            for (contentWord in contentWords) {
                if (contentWord.contains(queryWord) || queryWord.contains(contentWord)) {
                    score += 0.1f
                }
            }
        }

        // 归一化分数到0-1范围
        return (score / queryWords.size).coerceIn(0f, 1f)
    }

    /**
     * 获取上下文消息。
     */
    internal suspend fun getContextMessages(
        messages: List<MemoryMessage>,
        threadId: String,
        contextRange: Int
    ): List<MemoryMessage> {
        if (contextRange <= 0 || messages.isEmpty()) {
            return messages
        }

        val allMessages = baseMemory.getMessages(threadId, 1000)
        val result = mutableListOf<MemoryMessage>()

        for (message in messages) {
            val index = allMessages.indexOfFirst { it.id == message.id }
            if (index == -1) continue

            // 添加当前消息
            result.add(message)

            // 添加前面的消息
            val startIndex = (index - contextRange).coerceAtLeast(0)
            for (i in startIndex until index) {
                result.add(allMessages[i])
            }

            // 添加后面的消息
            val endIndex = (index + contextRange + 1).coerceAtMost(allMessages.size)
            for (i in index + 1 until endIndex) {
                result.add(allMessages[i])
            }
        }

        // 去重并按时间排序
        return result.distinctBy { it.id }.sortedBy { it.createdAt }
    }

    // 实现 Memory 接口的方法

    override suspend fun createThread(title: String?): String {
        return baseMemory.createThread(title)
    }

    override suspend fun deleteThread(threadId: String): Boolean {
        return baseMemory.deleteThread(threadId)
    }

    override suspend fun getThread(threadId: String): MemoryThread? {
        return baseMemory.getThread(threadId)
    }

    override suspend fun listThreads(limit: Int, offset: Int): List<MemoryThread> {
        return baseMemory.listThreads(limit, offset)
    }

    override suspend fun saveMessage(message: Message, threadId: String, priority: MemoryPriority?, metadata: Map<String, Any>?): String {
        val messageId = baseMemory.saveMessage(message, threadId, priority, metadata)
        semanticMemory.saveMessage(message, threadId)
        return messageId
    }

    override suspend fun getMessages(
        threadId: String,
        limit: Int,
        processors: List<MemoryProcessor>?
    ): List<MemoryMessage> {
        return baseMemory.getMessages(threadId, limit, processors)
    }

    override suspend fun searchMessages(query: String, threadId: String, limit: Int): List<MemoryMessage> {
        // 执行混合搜索
        val config = SemanticRecallConfig(topK = limit)
        val hybridResults = hybridSearch(query, threadId, config)

        // 获取消息
        val messages = hybridResults.map { it.message }

        // 获取上下文消息
        return getContextMessages(messages, threadId, 1)
    }

    override suspend fun semanticSearch(
        query: String,
        threadId: String,
        config: SemanticRecallConfig
    ): List<SemanticSearchResult> {
        return hybridSearch(query, threadId, config)
    }

    override suspend fun updateMessagePriority(messageId: String, priority: MemoryPriority): Boolean {
        return baseMemory.updateMessagePriority(messageId, priority)
    }

    override suspend fun getMessagePriority(messageId: String): MemoryPriority? {
        return baseMemory.getMessagePriority(messageId)
    }

    override suspend fun applyPriorityDecay(config: MemoryPriorityConfig): Int {
        return baseMemory.applyPriorityDecay(config)
    }

    override suspend fun cleanupLowPriorityMessages(config: MemoryPriorityConfig): Int {
        return baseMemory.cleanupLowPriorityMessages(config)
    }

    override fun getStructuredMemory(): StructuredMemory? {
        return baseMemory.getStructuredMemory()
    }
}

/**
 * 混合语义内存委托类，实现 SemanticMemory 接口。
 */
private class HybridSemanticMemoryDelegate(
    private val hybridSearchMemory: HybridSearchMemoryImpl
) : SemanticMemory {

    override suspend fun saveMessage(message: Message, threadId: String): String {
        return hybridSearchMemory.saveMessage(message, threadId)
    }

    override suspend fun semanticSearch(
        query: String,
        threadId: String,
        config: SemanticRecallConfig
    ): List<SemanticSearchResult> {
        return hybridSearchMemory.hybridSearch(query, threadId, config)
    }

    override suspend fun getSemanticRecallMessages(
        query: String,
        threadId: String,
        config: SemanticRecallConfig
    ): List<MemoryMessage> {
        // 执行混合搜索
        val hybridResults = hybridSearchMemory.hybridSearch(query, threadId, config)

        // 获取消息
        val messages = hybridResults.map { it.message }

        // 获取上下文消息
        return hybridSearchMemory.getContextMessages(messages, threadId, config.messageRange)
    }


}