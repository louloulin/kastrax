package ai.kastrax.codebase.retrieval

import ai.kastrax.codebase.context.Context
import ai.kastrax.codebase.context.ContextBuilder
import ai.kastrax.codebase.context.ContextLevel
import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.vector.CodeVectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * 检索结果
 *
 * @property element 代码元素
 * @property score 分数
 * @property explanation 解释
 */
data class RetrievalResult(
    val element: CodeElement,
    val score: Double,
    val explanation: String? = null
)

/**
 * 检索引擎类型
 */
enum class RetrievalEngineType {
    /**
     * 上下文感知
     */
    CONTEXT_AWARE,
    
    /**
     * 多因素
     */
    MULTIFACTOR,
    
    /**
     * 自定义
     */
    CUSTOM
}

/**
 * 检索引擎事件类型
 */
enum class RetrievalEngineEventType {
    /**
     * 初始化
     */
    INITIALIZED,
    
    /**
     * 查询执行
     */
    QUERY_EXECUTED,
    
    /**
     * 反馈接收
     */
    FEEDBACK_RECEIVED,
    
    /**
     * 模型更新
     */
    MODEL_UPDATED,
    
    /**
     * 错误
     */
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
 * @property minScore 最小分数
 */
data class ContextAwareRetrievalEngineConfig(
    val engineType: RetrievalEngineType = RetrievalEngineType.CONTEXT_AWARE,
    val maxContextSize: Int = 10,
    val enableEventNotifications: Boolean = true,
    val enableFeedbackLearning: Boolean = true,
    val enableExplanations: Boolean = true,
    val minScore: Double = 0.7
)

/**
 * 上下文感知检索引擎
 *
 * 提供基于上下文的代码检索功能
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property contextBuilder 上下文构建器
 * @property config 配置
 */
class ContextAwareRetrievalEngine(
    private val vectorStore: CodeVectorStore,
    private val embeddingService: CodeEmbeddingService,
    private val contextBuilder: ContextBuilder,
    private val config: ContextAwareRetrievalEngineConfig = ContextAwareRetrievalEngineConfig()
) {
    // 查询历史
    private val queryHistory = ConcurrentHashMap<String, MutableList<String>>()

    // 结果历史
    private val resultHistory = ConcurrentHashMap<String, MutableList<RetrievalResult>>()

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
        currentFile: Path?,
        currentPosition: Location?,
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
            contextData["currentFile"] = currentFile.toString()
        }

        // 添加当前位置信息
        if (currentPosition != null) {
            contextData["currentPosition"] = currentPosition.toString()
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
        currentFile: Path? = null,
        currentPosition: Location? = null,
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

            // 构建查询向量
            val queryVector = embeddingService.embed(query).toList()

            // 执行相似度搜索
            val searchResults = vectorStore.similaritySearch(
                vector = queryVector,
                limit = limit * 2, // 获取更多结果，以便后续过滤
                minScore = minScore.toFloat()
            )

            // 构建上下文
            val context = if (currentPosition != null) {
                // 如果指定了位置，构建位置相关的上下文
                contextBuilder.buildContext(
                    query = query,
                    position = currentPosition,
                    maxElements = limit * 2,
                    minScore = minScore.toFloat()
                )
            } else if (currentFile != null) {
                // 如果指定了文件，构建文件相关的上下文
                contextBuilder.buildFileContext(
                    filePath = currentFile,
                    maxElements = limit * 2
                )
            } else {
                // 否则，构建普通上下文
                contextBuilder.buildContext(
                    query = query,
                    maxElements = limit * 2,
                    minScore = minScore.toFloat()
                )
            }

            // 合并搜索结果和上下文元素
            val results = mergeResults(searchResults, context, limit, minScore)

            // 更新查询历史
            queryHistory.computeIfAbsent(sessionId) { mutableListOf() }.add(query)

            // 更新结果历史
            resultHistory.computeIfAbsent(sessionId) { mutableListOf() }.addAll(results)

            // 限制历史大小
            while (queryHistory[sessionId]?.size ?: 0 > config.maxContextSize) {
                queryHistory[sessionId]?.removeAt(0)
            }
            while (resultHistory[sessionId]?.size ?: 0 > config.maxContextSize * 3) {
                resultHistory[sessionId]?.removeAt(0)
            }

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
     * 合并结果
     *
     * @param searchResults 搜索结果
     * @param context 上下文
     * @param limit 返回结果的最大数量
     * @param minScore 最小分数
     * @return 检索结果列表
     */
    private fun mergeResults(
        searchResults: List<ai.kastrax.codebase.vector.CodeSearchResult>,
        context: Context,
        limit: Int,
        minScore: Double
    ): List<RetrievalResult> {
        // 创建结果映射表
        val resultMap = mutableMapOf<String, RetrievalResult>()

        // 添加搜索结果
        searchResults.forEach { result ->
            if (result.score >= minScore) {
                resultMap[result.element.id] = RetrievalResult(
                    element = result.element,
                    score = result.score,
                    explanation = if (config.enableExplanations) {
                        "相似度搜索结果，分数: ${result.score}"
                    } else {
                        null
                    }
                )
            }
        }

        // 添加上下文元素
        context.elements.forEach { contextElement ->
            val element = contextElement.element
            val existingResult = resultMap[element.id]

            if (existingResult == null || contextElement.relevance > existingResult.score) {
                resultMap[element.id] = RetrievalResult(
                    element = element,
                    score = contextElement.relevance.toDouble(),
                    explanation = if (config.enableExplanations) {
                        "上下文元素，级别: ${contextElement.level}，相关性: ${contextElement.relevance}"
                    } else {
                        null
                    }
                )
            }
        }

        // 排序并限制结果数量
        return resultMap.values.sortedByDescending { it.score }.take(limit)
    }

    /**
     * 提供反馈
     *
     * @param elementId 元素 ID
     * @param score 分数
     * @param sessionId 会话 ID
     * @param comment 评论
     * @return 是否成功提供反馈
     */
    suspend fun provideFeedback(
        elementId: String,
        score: Double,
        sessionId: String = "default",
        comment: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 获取用户反馈
            val feedback = userFeedback.computeIfAbsent(sessionId) { mutableMapOf() }

            // 添加反馈
            feedback[elementId] = score

            // 发送反馈接收事件
            emitEvent(
                RetrievalEngineEventType.FEEDBACK_RECEIVED,
                "接收反馈: 元素 $elementId, 分数 $score",
                mapOf(
                    "elementId" to elementId,
                    "score" to score,
                    "sessionId" to sessionId,
                    "comment" to (comment ?: "")
                )
            )

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "提供反馈失败: $elementId, ${e.message}" }

            // 发送错误事件
            emitEvent(
                RetrievalEngineEventType.ERROR,
                "提供反馈失败: $elementId, ${e.message}",
                mapOf("error" to e, "elementId" to elementId)
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
        contextBuilder.clearContextCache()
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
