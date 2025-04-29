package ai.kastrax.a2x.semantic

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 语义服务，整合语义层的各个组件
 */
class SemanticService {
    /**
     * 本体管理器
     */
    val ontologyManager = OntologyManager()

    /**
     * 语义映射器
     */
    val semanticMapper = SemanticMapper()

    /**
     * 上下文管理器
     */
    val contextManager = ContextManager()

    /**
     * 意图识别器
     */
    val intentRecognizer = IntentRecognizer()

    /**
     * 实体解析器
     */
    val entityResolver = EntityResolver()

    /**
     * 处理消息
     */
    fun processMessage(text: String, sessionId: String? = null): MessageProcessingResult {
        // 识别意图
        val intentResult = intentRecognizer.recognizeIntent(text)

        // 解析实体
        val entities = entityResolver.resolveEntities(text)

        // 更新会话
        val session = if (sessionId != null) {
            contextManager.getSession(sessionId)?.let { session ->
                // 添加历史记录
                contextManager.addSessionHistory(
                    sessionId = sessionId,
                    entry = HistoryEntry(
                        id = "entry-${java.util.UUID.randomUUID()}",
                        type = "user",
                        content = buildJsonObject {
                            put("text", text)
                        }
                    )
                )

                // 更新会话状态
                if (intentResult.topIntent != null) {
                    contextManager.updateSessionState(
                        sessionId = sessionId,
                        key = "intent",
                        value = kotlinx.serialization.json.Json.encodeToJsonElement(Intent.serializer(), intentResult.topIntent)
                    )
                }

                // 更新实体状态
                entities.forEach { (type, typeEntities) ->
                    contextManager.updateSessionState(
                        sessionId = sessionId,
                        key = "entity_$type",
                        value = kotlinx.serialization.json.Json.encodeToJsonElement(
                            kotlinx.serialization.builtins.ListSerializer(ResolvedEntity.serializer()),
                            typeEntities
                        )
                    )
                }

                // 获取更新后的会话
                contextManager.getSession(sessionId)
            }
        } else {
            null
        }

        return MessageProcessingResult(
            text = text,
            intentResult = intentResult,
            entities = entities,
            session = session
        )
    }

    /**
     * 生成响应
     */
    fun generateResponse(result: MessageProcessingResult, sessionId: String? = null): ResponseGenerationResult {
        // 这里应该实现响应生成逻辑
        // 可以基于意图、实体和会话状态生成响应
        // 这里只是一个简单的实现

        val responseText = if (result.intentResult.topIntent != null) {
            "我理解你的意图是：${result.intentResult.topIntent.name}"
        } else {
            "我不太理解你的意思，能请你说得更清楚一些吗？"
        }

        // 更新会话
        val session = if (sessionId != null) {
            contextManager.getSession(sessionId)?.let { session ->
                // 添加历史记录
                contextManager.addSessionHistory(
                    sessionId = sessionId,
                    entry = HistoryEntry(
                        id = "entry-${java.util.UUID.randomUUID()}",
                        type = "system",
                        content = buildJsonObject {
                            put("text", responseText)
                        }
                    )
                )

                // 获取更新后的会话
                contextManager.getSession(sessionId)
            }
        } else {
            null
        }

        return ResponseGenerationResult(
            text = responseText,
            session = session
        )
    }

    /**
     * 创建会话
     */
    fun createSession(contextId: String, entityId: String): Session? {
        return contextManager.createSession(contextId, entityId)
    }

    /**
     * 获取会话
     */
    fun getSession(sessionId: String): Session? {
        return contextManager.getSession(sessionId)
    }

    /**
     * 创建上下文
     */
    fun createContext(name: String, description: String, type: String): Context {
        return contextManager.createContext(name, description, type)
    }

    /**
     * 获取上下文
     */
    fun getContext(contextId: String): Context? {
        return contextManager.getContext(contextId)
    }

    /**
     * 添加消息到会话
     */
    fun addMessageToSession(sessionId: String, type: String, content: JsonObject): Session? {
        return contextManager.addSessionHistory(
            sessionId = sessionId,
            entry = HistoryEntry(
                id = "entry-${java.util.UUID.randomUUID()}",
                type = type,
                content = content
            )
        )
    }
}

/**
 * 消息处理结果
 */
data class MessageProcessingResult(
    /**
     * 输入文本
     */
    val text: String,

    /**
     * 意图识别结果
     */
    val intentResult: IntentRecognitionResult,

    /**
     * 实体解析结果
     */
    val entities: Map<String, List<ResolvedEntity>>,

    /**
     * 会话
     */
    val session: Session?
)

/**
 * 响应生成结果
 */
data class ResponseGenerationResult(
    /**
     * 响应文本
     */
    val text: String,

    /**
     * 会话
     */
    val session: Session?
)
