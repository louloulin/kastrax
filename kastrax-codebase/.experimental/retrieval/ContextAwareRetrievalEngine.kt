package ai.kastrax.codebase.retrieval

import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.semantic.memory.SemanticMemory
import ai.kastrax.codebase.semantic.memory.SemanticMemoryManager
import ai.kastrax.codebase.semantic.memory.SemanticMemoryRetriever
import ai.kastrax.codebase.semantic.memory.SemanticMemorySearchResult
import ai.kastrax.rag.RAG
import ai.kastrax.rag.context.ContextBuilder
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.model.RetrieveContextResult
import ai.kastrax.rag.retriever.Retriever
import ai.kastrax.rag.retriever.RetrieverFactory
import ai.kastrax.rag.reranker.ContextAwareReranker
import ai.kastrax.rag.reranker.DiversityReranker
import ai.kastrax.rag.reranker.RelevanceReranker
import ai.kastrax.rag.reranker.Reranker
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * 检索结果
 *
 * @property memory 语义记忆
 * @property score 分数
 * @property explanation 解释
 */
data class RetrievalResult(
    val memory: SemanticMemory,
    val score: Double,
    val explanation: String? = null
)

/**
 * 检索引擎类型
 */
enum class RetrievalEngineType {
    CONTEXT_AWARE,
    MULTIFACTOR,
    CUSTOM
}

/**
 * 检索引擎事件类型
 */
enum class RetrievalEngineEventType {
    INITIALIZED,
    QUERY_EXECUTED,
    FEEDBACK_RECEIVED,
    MODEL_UPDATED,
    ERROR
}

/**
 * 检索引擎事件
 *
 * @property type 事件类型
 * @property message 事件消息
 * @property data 事件数据
 */
data class RetrievalEngineEvent(
    val type: RetrievalEngineEventType,
    val message: String,
    val data: Map<String, Any> = emptyMap()
)

/**
 * 上下文感知检索引擎配置
 *
 * @property engineType 引擎类型
 * @property maxContextSize 最大上下文大小
 * @property enableEventNotifications 是否启用事件通知
 * @property enableFeedbackLearning 是否启用反馈学习
 * @property enableExplanations 是否启用解释
 * @property contextBuilderConfig 上下文构建器配置
 * @property minScore 最小分数
 */
data class ContextAwareRetrievalEngineConfig(
    val engineType: RetrievalEngineType = RetrievalEngineType.CONTEXT_AWARE,
    val maxContextSize: Int = 10,
    val enableEventNotifications: Boolean = true,
    val enableFeedbackLearning: Boolean = true,
    val enableExplanations: Boolean = true,
    val contextBuilderConfig: ContextBuilderConfig = ContextBuilderConfig(),
    val minScore: Double = 0.7
)

/**
 * 上下文感知检索引擎
 *
 * 提供基于上下文的代码检索功能
 *
 * @property memoryManager 语义记忆管理器
 * @property embeddingService 嵌入服务
 * @property config 配置
 */
class ContextAwareRetrievalEngine(
    private val memoryManager: SemanticMemoryManager,
    private val embeddingService: CodeEmbeddingService,
    private val config: ContextAwareRetrievalEngineConfig = ContextAwareRetrievalEngineConfig()
) {
    // RAG 实例
    private lateinit var rag: RAG

    // 检索器
    private lateinit var retriever: Retriever

    // 重排序器
    private lateinit var reranker: Reranker

    // 上下文构建器
    private lateinit var contextBuilder: ContextBuilder

    // 记忆检索器
    private val memoryRetriever: SemanticMemoryRetriever = memoryManager.getMemoryRetriever()

    // 查询历史
    private val queryHistory = ConcurrentHashMap<String, MutableList<String>>()

    // 结果历史
    private val resultHistory = ConcurrentHashMap<String, MutableList<SemanticMemorySearchResult>>()

    // 用户反馈
    private val userFeedback = ConcurrentHashMap<String, MutableMap<String, Double>>()

    // 事件流
    private val _events = MutableSharedFlow<RetrievalEngineEvent>(replay = 0)
    val events: SharedFlow<RetrievalEngineEvent> = _events.asSharedFlow()

    // 是否已初始化
    private val initialized = AtomicBoolean(false)

    /**
     * 初始化引擎
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (initialized.getAndSet(true)) {
            logger.info { "上下文感知检索引擎已经初始化" }
            return@withContext
        }

        logger.info { "初始化上下文感知检索引擎" }

        try {
            // 创建上下文构建器
            contextBuilder = ContextBuilder(config.contextBuilderConfig)

            // 创建检索器
            retriever = RetrieverFactory.createRetriever(
                embeddingService = embeddingService,
                vectorStore = memoryRetriever.getVectorStore()
            )

            // 创建重排序器
            reranker = when (config.engineType) {
                RetrievalEngineType.CONTEXT_AWARE -> ContextAwareReranker(embeddingService)
                RetrievalEngineType.MULTIFACTOR -> DiversityReranker(RelevanceReranker())
                else -> RelevanceReranker()
            }

            // 创建 RAG 实例
            rag = RAG.builder()
                .withEmbeddingService(embeddingService)
                .withRetriever(retriever)
                .withReranker(reranker)
                .withContextBuilder(contextBuilder)
                .build()

            // 发送初始化事件
            emitEvent(
                RetrievalEngineEventType.INITIALIZED,
                "上下文感知检索引擎初始化完成"
            )
        } catch (e: Exception) {
            logger.error(e) { "初始化上下文感知检索引擎失败: ${e.message}" }

            // 发送错误事件
            emitEvent(
                RetrievalEngineEventType.ERROR,
                "初始化上下文感知检索引擎失败: ${e.message}",
                mapOf("error" to e)
            )

            throw e
        }
    }

    /**
     * 创建上下文数据
     *
     * @param query 查询文本
     * @param sessionId 会话 ID
     * @param currentFile 当前文件
     * @param currentPosition 当前位置
     * @param selectedText 选中的文本
     * @param metadata 元数据
     * @return 上下文数据
     */
    private fun createContextData(
        query: String,
        sessionId: String,
        currentFile: String?,
        currentPosition: Int?,
        selectedText: String?,
        metadata: Map<String, Any>
    ): Map<String, Any> {
        // 获取查询历史
        val previousQueries = queryHistory.computeIfAbsent(sessionId) { mutableListOf() }
            .takeLast(config.maxContextSize)

        // 获取用户反馈
        val feedback = userFeedback.computeIfAbsent(sessionId) { mutableMapOf() }

        // 创建上下文数据
        val contextData = mutableMapOf<String, Any>(
            "query" to query,
            "previousQueries" to previousQueries,
            "userFeedback" to feedback
        )

        // 添加当前文件信息
        if (currentFile != null) {
            contextData["currentFile"] = currentFile
        }

        // 添加当前位置信息
        if (currentPosition != null) {
            contextData["currentPosition"] = currentPosition
        }

        // 添加选中的文本信息
        if (selectedText != null) {
            contextData["selectedText"] = selectedText
        }

        // 添加其他元数据
        contextData.putAll(metadata)

        return contextData
    }

    /**
     * 检索
     *
     * @param query 查询文本
     * @param sessionId 会话 ID
     * @param limit 返回结果的最大数量
     * @param minScore 最小分数
     * @param currentFile 当前文件
     * @param currentPosition 当前位置
     * @param selectedText 选中的文本
     * @param metadata 元数据
     * @return 检索结果列表
     */
    suspend fun retrieve(
        query: String,
        sessionId: String = "default",
        limit: Int = 10,
        minScore: Double = config.minScore,
        currentFile: String? = null,
        currentPosition: Int? = null,
        selectedText: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): List<RetrievalResult> = withContext(Dispatchers.IO) {
        try {
            // 确保引擎已初始化
            if (!initialized.get()) {
                initialize()
            }

            // 创建上下文数据
            val contextData = createContextData(
                query = query,
                sessionId = sessionId,
                currentFile = currentFile,
                currentPosition = currentPosition,
                selectedText = selectedText,
                metadata = metadata
            )

            // 执行 RAG 检索
            val retrieveResult = rag.retrieveContext(
                query = query,
                contextData = contextData,
                limit = limit,
                minScore = minScore
            )

            // 更新查询历史
            queryHistory.computeIfAbsent(sessionId) { mutableListOf() }.add(query)

            // 将 RAG 结果转换为语义记忆结果
            val results = convertToRetrievalResults(retrieveResult, sessionId)

            // 发送查询执行事件
            emitEvent(
                RetrievalEngineEventType.QUERY_EXECUTED,
                "执行查询: $query, 找到 ${results.size} 个结果",
                mapOf(
                    "query" to query,
                    "sessionId" to sessionId,
                    "resultCount" to results.size
                )
            )

            return@withContext results
        } catch (e: Exception) {
            logger.error(e) { "检索失败: $query, ${e.message}" }

            // 发送错误事件
            emitEvent(
                RetrievalEngineEventType.ERROR,
                "检索失败: $query, ${e.message}",
                mapOf("error" to e, "query" to query)
            )

            return@withContext emptyList()
        }
    }

    /**
     * 将 RAG 检索结果转换为语义记忆检索结果
     *
     * @param retrieveResult RAG 检索结果
     * @param sessionId 会话 ID
     * @return 语义记忆检索结果列表
     */
    private fun convertToRetrievalResults(retrieveResult: RetrieveContextResult, sessionId: String): List<RetrievalResult> {
        val results = mutableListOf<RetrievalResult>()

        // 获取结果历史
        val previousResults = resultHistory.computeIfAbsent(sessionId) { mutableListOf() }

        // 处理每个检索结果
        retrieveResult.documents.forEach { document ->
            // 尝试从元数据中获取记忆 ID
            val memoryId = document.metadata["memoryId"] as? String

            if (memoryId != null) {
                // 从记忆管理器中获取记忆
                val memory = memoryManager.getMemory(memoryId)

                if (memory != null) {
                    // 创建检索结果
                    val result = RetrievalResult(
                        memory = memory,
                        score = document.score,
                        explanation = if (config.enableExplanations) document.explanation else null
                    )

                    results.add(result)

                    // 更新结果历史
                    previousResults.add(SemanticMemorySearchResult(memory, document.score))
                }
            }
        }

        // 限制结果历史大小
        while (previousResults.size > config.maxContextSize * 3) {
            previousResults.removeAt(0)
        }

        return results
    }

    /**
     * 提供反馈
     *
     * @param memoryId 记忆 ID
     * @param score 分数
     * @param sessionId 会话 ID
     * @param comment 评论
     * @return 是否成功提供反馈
     */
    suspend fun provideFeedback(
        memoryId: String,
        score: Double,
        sessionId: String = "default",
        comment: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 获取用户反馈
            val feedback = userFeedback.computeIfAbsent(sessionId) { mutableMapOf() }

            // 添加反馈
            feedback[memoryId] = score

            // 如果启用了反馈学习，将反馈信息传递给 RAG
            if (config.enableFeedbackLearning && initialized.get()) {
                rag.provideFeedback(memoryId, score, comment)
            }

            // 发送反馈接收事件
            emitEvent(
                RetrievalEngineEventType.FEEDBACK_RECEIVED,
                "接收反馈: 记忆 $memoryId, 分数 $score",
                mapOf(
                    "memoryId" to memoryId,
                    "score" to score,
                    "sessionId" to sessionId,
                    "comment" to (comment ?: "")
                )
            )

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "提供反馈失败: $memoryId, ${e.message}" }

            // 发送错误事件
            emitEvent(
                RetrievalEngineEventType.ERROR,
                "提供反馈失败: $memoryId, ${e.message}",
                mapOf("error" to e, "memoryId" to memoryId)
            )

            return@withContext false
        }
    }

    /**
     * 清除会话历史
     *
     * @param sessionId 会话 ID
     * @return 是否成功清除
     */
    fun clearSessionHistory(sessionId: String): Boolean {
        try {
            queryHistory.remove(sessionId)
            resultHistory.remove(sessionId)
            userFeedback.remove(sessionId)

            return true
        } catch (e: Exception) {
            logger.error(e) { "清除会话历史失败: $sessionId, ${e.message}" }
            return false
        }
    }

    /**
     * 清除所有历史
     */
    fun clearAllHistory() {
        queryHistory.clear()
        resultHistory.clear()
        userFeedback.clear()
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        if (initialized.get()) {
            rag.clearCache()
        }
    }

    /**
     * 获取记忆检索器
     *
     * @return 记忆检索器
     */
    fun getMemoryRetriever(): SemanticMemoryRetriever {
        return memoryRetriever
    }

    /**
     * 获取 RAG 实例
     *
     * @return RAG 实例
     */
    fun getRag(): RAG {
        if (!initialized.get()) {
            throw IllegalStateException("检索引擎尚未初始化")
        }
        return rag
    }

    /**
     * 发送事件
     *
     * @param type 事件类型
     * @param message 事件消息
     * @param data 事件数据
     */
    private suspend fun emitEvent(
        type: RetrievalEngineEventType,
        message: String,
        data: Map<String, Any> = emptyMap()
    ) {
        if (!config.enableEventNotifications) {
            return
        }

        try {
            val event = RetrievalEngineEvent(type, message, data)
            _events.emit(event)
        } catch (e: Exception) {
            logger.error(e) { "发送事件失败: $type, $message, ${e.message}" }
        }
    }
}
