package ai.kastrax.edutech.assistant

import ai.kastrax.core.actor.Actor
import ai.kastrax.core.actor.ActorContext
import ai.kastrax.core.actor.Message
import ai.kastrax.edutech.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * 虚拟教学助手Actor
 * 处理单个虚拟助手的所有交互和状态管理
 */
class VirtualAssistantActor(
    private val assistantId: String,
    private val assistantService: VirtualTeachingAssistantService,
    private val emotionService: EmotionDetectionService
) : Actor {

    private var assistant: VirtualTeachingAssistant? = null
    private val activeConversations = mutableMapOf<String, AssistantConversation>()
    private val conversationMetrics = mutableMapOf<String, ConversationMetrics>()
    private var lastActivityTime: Instant = Clock.System.now()

    override suspend fun receive(message: Message, context: ActorContext) {
        lastActivityTime = Clock.System.now()
        
        when (message) {
            is InitializeAssistant -> handleInitializeAssistant(message, context)
            is StartConversation -> handleStartConversation(message, context)
            is ProcessStudentMessage -> handleProcessStudentMessage(message, context)
            is EndConversation -> handleEndConversation(message, context)
            is UpdateAssistantKnowledge -> handleUpdateKnowledge(message, context)
            is GetAssistantStatus -> handleGetStatus(message, context)
            is GetConversationHistory -> handleGetHistory(message, context)
            is AdaptAssistantBehavior -> handleAdaptBehavior(message, context)
            is AnalyzePerformance -> handleAnalyzePerformance(message, context)
        }
    }

    private suspend fun handleInitializeAssistant(message: InitializeAssistant, context: ActorContext) {
        try {
            assistant = assistantService.createAssistant(
                name = message.name,
                specializations = message.specializations,
                personality = message.personality,
                capabilities = message.capabilities
            )
            
            context.reply(AssistantInitialized(
                assistantId = assistantId,
                success = true,
                assistant = assistant!!,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(AssistantInitialized(
                assistantId = assistantId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleStartConversation(message: StartConversation, context: ActorContext) {
        try {
            val conversation = assistantService.startConversation(
                assistantId = assistantId,
                studentId = message.studentId,
                subject = message.subject,
                initialMessage = message.initialMessage
            )
            
            activeConversations[conversation.conversationId] = conversation
            conversationMetrics[conversation.conversationId] = ConversationMetrics(
                startTime = Clock.System.now(),
                messageCount = 0,
                averageResponseTime = 0.seconds,
                emotionChanges = 0,
                topicsDiscussed = mutableSetOf()
            )
            
            context.reply(ConversationStarted(
                conversationId = conversation.conversationId,
                success = true,
                conversation = conversation,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(ConversationStarted(
                conversationId = "",
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleProcessStudentMessage(message: ProcessStudentMessage, context: ActorContext) {
        try {
            val startTime = Clock.System.now()
            
            // 处理学生消息
            val response = assistantService.processStudentMessage(
                conversationId = message.conversationId,
                message = message.message
            )
            
            // 更新对话指标
            updateConversationMetrics(message.conversationId, startTime)
            
            // 分析情绪变化
            val emotion = emotionService.detectEmotion(message.message, message.studentId.value)
            val emotionTrend = emotionService.analyzeEmotionTrend(message.studentId.value)
            
            // 获取情绪建议
            val emotionSuggestions = emotionService.getEmotionBasedSuggestions(emotion)
            
            // 检查是否需要适应行为
            if (shouldAdaptBehavior(emotion, emotionTrend)) {
                adaptAssistantBehavior(emotion, emotionTrend)
            }
            
            context.reply(MessageProcessed(
                conversationId = message.conversationId,
                success = true,
                response = response,
                detectedEmotion = emotion,
                emotionSuggestions = emotionSuggestions,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(MessageProcessed(
                conversationId = message.conversationId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleEndConversation(message: EndConversation, context: ActorContext) {
        try {
            val summary = assistantService.endConversation(message.conversationId)
            
            // 移除活跃对话
            activeConversations.remove(message.conversationId)
            val metrics = conversationMetrics.remove(message.conversationId)
            
            context.reply(ConversationEnded(
                conversationId = message.conversationId,
                success = true,
                summary = summary,
                metrics = metrics,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(ConversationEnded(
                conversationId = message.conversationId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleUpdateKnowledge(message: UpdateAssistantKnowledge, context: ActorContext) {
        try {
            assistantService.updateAssistantKnowledge(
                assistantId = assistantId,
                subject = message.subject,
                newKnowledge = message.knowledge
            )
            
            context.reply(KnowledgeUpdated(
                assistantId = assistantId,
                success = true,
                subject = message.subject,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(KnowledgeUpdated(
                assistantId = assistantId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleGetStatus(message: GetAssistantStatus, context: ActorContext) {
        val status = AssistantStatus(
            assistantId = assistantId,
            isActive = assistant != null,
            activeConversations = activeConversations.size,
            lastActivityTime = lastActivityTime,
            totalConversationsToday = conversationMetrics.size,
            averageResponseTime = calculateAverageResponseTime(),
            currentLoad = calculateCurrentLoad()
        )
        
        context.reply(AssistantStatusResponse(
            status = status,
            timestamp = Clock.System.now()
        ))
    }

    private suspend fun handleGetHistory(message: GetConversationHistory, context: ActorContext) {
        val conversations = activeConversations.values.filter { 
            it.studentId == message.studentId 
        }.toList()
        
        context.reply(ConversationHistoryResponse(
            studentId = message.studentId,
            conversations = conversations,
            timestamp = Clock.System.now()
        ))
    }

    private suspend fun handleAdaptBehavior(message: AdaptAssistantBehavior, context: ActorContext) {
        try {
            // 根据适应请求调整助手行为
            val currentAssistant = assistant ?: throw IllegalStateException("Assistant not initialized")
            
            val adaptedPersonality = adaptPersonality(
                currentAssistant.personality,
                message.adaptationType,
                message.targetValue
            )
            
            assistant = currentAssistant.copy(
                personality = adaptedPersonality,
                lastUpdated = Clock.System.now()
            )
            
            context.reply(BehaviorAdapted(
                assistantId = assistantId,
                success = true,
                adaptationType = message.adaptationType,
                newValue = message.targetValue,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(BehaviorAdapted(
                assistantId = assistantId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    private suspend fun handleAnalyzePerformance(message: AnalyzePerformance, context: ActorContext) {
        try {
            val performance = assistantService.getAssistantPerformance(
                assistantId = assistantId,
                timeRange = message.timeRange
            )
            
            context.reply(PerformanceAnalysisResponse(
                assistantId = assistantId,
                success = true,
                performance = performance,
                timestamp = Clock.System.now()
            ))
        } catch (e: Exception) {
            context.reply(PerformanceAnalysisResponse(
                assistantId = assistantId,
                success = false,
                error = e.message,
                timestamp = Clock.System.now()
            ))
        }
    }

    // 私有辅助方法
    private fun updateConversationMetrics(conversationId: String, startTime: Instant) {
        val metrics = conversationMetrics[conversationId] ?: return
        val responseTime = Clock.System.now() - startTime
        
        conversationMetrics[conversationId] = metrics.copy(
            messageCount = metrics.messageCount + 1,
            averageResponseTime = (metrics.averageResponseTime + responseTime) / 2
        )
    }

    private fun shouldAdaptBehavior(emotion: DetectedEmotion, trend: EmotionTrend): Boolean {
        return when {
            emotion.primary == Emotion.FRUSTRATED && emotion.intensity > 0.7f -> true
            emotion.primary == Emotion.BORED && emotion.intensity > 0.6f -> true
            trend.trend == TrendDirection.DECLINING && trend.confidence > 0.7f -> true
            else -> false
        }
    }

    private fun adaptAssistantBehavior(emotion: DetectedEmotion, trend: EmotionTrend) {
        val currentAssistant = assistant ?: return
        
        val adaptedPersonality = when (emotion.primary) {
            Emotion.FRUSTRATED -> currentAssistant.personality.copy(
                patience = minOf(currentAssistant.personality.patience + 0.1f, 1.0f),
                encouragement = minOf(currentAssistant.personality.encouragement + 0.1f, 1.0f)
            )
            Emotion.BORED -> currentAssistant.personality.copy(
                enthusiasm = minOf(currentAssistant.personality.enthusiasm + 0.1f, 1.0f),
                humor = minOf(currentAssistant.personality.humor + 0.1f, 1.0f)
            )
            else -> currentAssistant.personality
        }
        
        assistant = currentAssistant.copy(
            personality = adaptedPersonality,
            lastUpdated = Clock.System.now()
        )
    }

    private fun adaptPersonality(
        personality: AssistantPersonality,
        adaptationType: PersonalityAdaptationType,
        targetValue: Float
    ): AssistantPersonality {
        return when (adaptationType) {
            PersonalityAdaptationType.FRIENDLINESS -> personality.copy(friendliness = targetValue)
            PersonalityAdaptationType.FORMALITY -> personality.copy(formality = targetValue)
            PersonalityAdaptationType.PATIENCE -> personality.copy(patience = targetValue)
            PersonalityAdaptationType.ENTHUSIASM -> personality.copy(enthusiasm = targetValue)
            PersonalityAdaptationType.EMPATHY -> personality.copy(empathy = targetValue)
            PersonalityAdaptationType.HUMOR -> personality.copy(humor = targetValue)
            PersonalityAdaptationType.ENCOURAGEMENT -> personality.copy(encouragement = targetValue)
            PersonalityAdaptationType.ADAPTABILITY -> personality.copy(adaptability = targetValue)
        }
    }

    private fun calculateAverageResponseTime(): kotlin.time.Duration {
        val allResponseTimes = conversationMetrics.values.map { it.averageResponseTime }
        return if (allResponseTimes.isNotEmpty()) {
            allResponseTimes.reduce { acc, duration -> acc + duration } / allResponseTimes.size
        } else {
            0.seconds
        }
    }

    private fun calculateCurrentLoad(): Float {
        val maxConcurrentConversations = assistant?.capabilities?.maxConcurrentStudents ?: 100
        return activeConversations.size.toFloat() / maxConcurrentConversations
    }
}

// 辅助数据类
data class ConversationMetrics(
    val startTime: Instant,
    val messageCount: Int,
    val averageResponseTime: kotlin.time.Duration,
    val emotionChanges: Int,
    val topicsDiscussed: MutableSet<String>
)

data class AssistantStatus(
    val assistantId: String,
    val isActive: Boolean,
    val activeConversations: Int,
    val lastActivityTime: Instant,
    val totalConversationsToday: Int,
    val averageResponseTime: kotlin.time.Duration,
    val currentLoad: Float
)

enum class PersonalityAdaptationType {
    FRIENDLINESS, FORMALITY, PATIENCE, ENTHUSIASM, EMPATHY, HUMOR, ENCOURAGEMENT, ADAPTABILITY
}
