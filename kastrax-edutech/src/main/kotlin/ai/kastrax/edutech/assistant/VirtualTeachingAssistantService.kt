package ai.kastrax.edutech.assistant

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.analytics.LearningAnalyticsService
import ai.kastrax.edutech.content.ContentManagementService
import ai.kastrax.edutech.llm.LLMService
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 虚拟教学助手服务
 * 提供智能虚拟教学助手的核心功能
 */
class VirtualTeachingAssistantService(
    private val llmService: LLMService,
    private val analyticsService: LearningAnalyticsService,
    private val contentService: ContentManagementService,
    private val emotionDetectionService: EmotionDetectionService,
    private val knowledgeBaseService: KnowledgeBaseService
) {
    private val assistants = mutableMapOf<String, VirtualTeachingAssistant>()
    private val activeConversations = mutableMapOf<String, AssistantConversation>()
    private val conversationHistory = mutableMapOf<String, List<AssistantConversation>>()

    /**
     * 创建虚拟教学助手
     */
    suspend fun createAssistant(
        name: String,
        specializations: Set<Subject>,
        personality: AssistantPersonality,
        capabilities: AssistantCapabilities
    ): VirtualTeachingAssistant {
        val assistantId = generateAssistantId()
        
        val knowledgeBase = knowledgeBaseService.buildKnowledgeBase(specializations)
        
        val assistant = VirtualTeachingAssistant(
            assistantId = assistantId,
            name = name,
            personality = personality,
            specializations = specializations,
            capabilities = capabilities,
            knowledgeBase = knowledgeBase,
            conversationStyle = generateConversationStyle(personality),
            languageSupport = setOf(Language.ENGLISH, Language.CHINESE),
            createdAt = Clock.System.now(),
            lastUpdated = Clock.System.now()
        )
        
        assistants[assistantId] = assistant
        return assistant
    }

    /**
     * 开始与助手的对话
     */
    suspend fun startConversation(
        assistantId: String,
        studentId: StudentId,
        subject: Subject,
        initialMessage: String? = null
    ): AssistantConversation {
        val assistant = assistants[assistantId]
            ?: throw IllegalArgumentException("Assistant not found: $assistantId")
        
        val studentProfile = analyticsService.getStudentProfile(studentId)
        val conversationId = generateConversationId()
        
        val context = ConversationContext(
            currentTopic = "",
            learningObjectives = emptyList(),
            studentLevel = studentProfile.currentDifficultyLevel,
            previousTopics = emptyList(),
            strugglingAreas = emptyList(),
            strengths = emptyList(),
            preferredLearningStyle = studentProfile.learningStyle,
            sessionGoals = emptyList()
        )
        
        val conversation = AssistantConversation(
            conversationId = conversationId,
            assistantId = assistantId,
            studentId = studentId,
            subject = subject,
            startTime = Clock.System.now(),
            messages = emptyList(),
            context = context,
            status = ConversationStatus.ACTIVE
        )
        
        activeConversations[conversationId] = conversation
        
        // 发送欢迎消息
        if (initialMessage == null) {
            val welcomeMessage = generateWelcomeMessage(assistant, studentProfile, subject)
            sendAssistantMessage(conversationId, welcomeMessage)
        } else {
            processStudentMessage(conversationId, initialMessage)
        }
        
        return activeConversations[conversationId]!!
    }

    /**
     * 处理学生消息
     */
    suspend fun processStudentMessage(
        conversationId: String,
        message: String
    ): AssistantResponse {
        val conversation = activeConversations[conversationId]
            ?: throw IllegalArgumentException("Conversation not found: $conversationId")
        
        val assistant = assistants[conversation.assistantId]!!
        val startTime = Clock.System.now()
        
        // 1. 分析学生消息
        val messageAnalysis = analyzeStudentMessage(message, conversation.context)
        
        // 2. 检测情绪
        val detectedEmotion = emotionDetectionService.detectEmotion(message)
        
        // 3. 更新对话上下文
        val updatedContext = updateConversationContext(
            conversation.context,
            messageAnalysis,
            detectedEmotion
        )
        
        // 4. 生成助手响应
        val response = generateAssistantResponse(
            assistant,
            messageAnalysis,
            updatedContext,
            detectedEmotion
        )
        
        // 5. 记录消息
        val studentMessage = ConversationMessage(
            messageId = generateMessageId(),
            sender = MessageSender.STUDENT,
            content = MessageContent(
                text = message,
                intent = messageAnalysis.intent,
                emotion = detectedEmotion
            ),
            timestamp = Clock.System.now(),
            metadata = MessageMetadata(
                responseTime = 0.milliseconds,
                processingSteps = emptyList(),
                knowledgeUsed = emptyList(),
                confidence = 1.0f
            )
        )
        
        val assistantMessage = ConversationMessage(
            messageId = generateMessageId(),
            sender = MessageSender.ASSISTANT,
            content = response.content,
            timestamp = Clock.System.now(),
            metadata = MessageMetadata(
                responseTime = Clock.System.now() - startTime,
                processingSteps = response.reasoning.knowledgeUsed.map { 
                    ProcessingStep(it, 100.milliseconds, "processed") 
                },
                knowledgeUsed = response.reasoning.knowledgeUsed,
                confidence = 0.9f
            )
        )
        
        // 6. 更新对话
        val updatedConversation = conversation.copy(
            messages = conversation.messages + listOf(studentMessage, assistantMessage),
            context = updatedContext
        )
        
        activeConversations[conversationId] = updatedConversation
        
        return response
    }

    /**
     * 结束对话
     */
    suspend fun endConversation(conversationId: String): ConversationSummary {
        val conversation = activeConversations[conversationId]
            ?: throw IllegalArgumentException("Conversation not found: $conversationId")
        
        val summary = generateConversationSummary(conversation)
        
        val completedConversation = conversation.copy(
            endTime = Clock.System.now(),
            status = ConversationStatus.COMPLETED,
            summary = summary
        )
        
        // 移动到历史记录
        val studentHistory = conversationHistory[conversation.studentId.value] ?: emptyList()
        conversationHistory[conversation.studentId.value] = studentHistory + completedConversation
        
        activeConversations.remove(conversationId)
        
        return summary
    }

    /**
     * 获取助手性能指标
     */
    suspend fun getAssistantPerformance(
        assistantId: String,
        timeRange: Pair<Instant, Instant>
    ): AssistantPerformanceMetrics {
        val assistant = assistants[assistantId]
            ?: throw IllegalArgumentException("Assistant not found: $assistantId")
        
        val relevantConversations = conversationHistory.values.flatten()
            .filter { it.assistantId == assistantId }
            .filter { it.startTime >= timeRange.first && it.startTime <= timeRange.second }
        
        return calculatePerformanceMetrics(assistant, relevantConversations, timeRange)
    }

    /**
     * 更新助手知识库
     */
    suspend fun updateAssistantKnowledge(
        assistantId: String,
        subject: Subject,
        newKnowledge: SubjectKnowledge
    ) {
        val assistant = assistants[assistantId]
            ?: throw IllegalArgumentException("Assistant not found: $assistantId")
        
        val updatedKnowledgeBase = assistant.knowledgeBase.copy(
            subjects = assistant.knowledgeBase.subjects + (subject to newKnowledge),
            lastKnowledgeUpdate = Clock.System.now()
        )
        
        val updatedAssistant = assistant.copy(
            knowledgeBase = updatedKnowledgeBase,
            lastUpdated = Clock.System.now()
        )
        
        assistants[assistantId] = updatedAssistant
    }

    // 私有辅助方法
    private fun generateAssistantId(): String = "assistant_${Clock.System.now().toEpochMilliseconds()}"
    private fun generateConversationId(): String = "conv_${Clock.System.now().toEpochMilliseconds()}"
    private fun generateMessageId(): String = "msg_${Clock.System.now().toEpochMilliseconds()}"

    private fun generateConversationStyle(personality: AssistantPersonality): ConversationStyle {
        return ConversationStyle(
            tone = when {
                personality.friendliness > 0.7f -> ConversationTone.FRIENDLY
                personality.formality > 0.7f -> ConversationTone.FORMAL
                else -> ConversationTone.PROFESSIONAL
            },
            complexity = ComplexityLevel.MODERATE,
            verbosity = when {
                personality.patience > 0.7f -> VerbosityLevel.DETAILED
                else -> VerbosityLevel.MODERATE
            },
            useOfExamples = ExampleUsage.MODERATE,
            questioningStyle = QuestioningStyle.GUIDED_DISCOVERY,
            feedbackStyle = FeedbackStyle.ENCOURAGING
        )
    }

    private suspend fun generateWelcomeMessage(
        assistant: VirtualTeachingAssistant,
        studentProfile: StudentProfile,
        subject: Subject
    ): String {
        return "你好！我是${assistant.name}，你的${subject.name}学习助手。我很高兴能帮助你学习！有什么问题我可以为你解答吗？"
    }

    private suspend fun analyzeStudentMessage(
        message: String,
        context: ConversationContext
    ): MessageAnalysis {
        // 使用LLM分析消息意图和内容
        val analysisPrompt = """
        分析以下学生消息的意图和内容：
        消息: "$message"
        当前话题: ${context.currentTopic}
        学生水平: ${context.studentLevel}
        
        请识别：
        1. 消息意图（问题、请求解释、寻求帮助等）
        2. 涉及的概念或主题
        3. 学生的困惑点
        4. 需要的帮助类型
        """.trimIndent()
        
        val analysisResult = llmService.generate(analysisPrompt)
        
        return MessageAnalysis(
            intent = MessageIntent.QUESTION, // 简化处理
            topics = listOf(context.currentTopic),
            concepts = emptyList(),
            difficultyLevel = context.studentLevel,
            helpType = HelpType.EXPLANATION
        )
    }

    private fun updateConversationContext(
        context: ConversationContext,
        messageAnalysis: MessageAnalysis,
        emotion: DetectedEmotion?
    ): ConversationContext {
        return context.copy(
            currentTopic = messageAnalysis.topics.firstOrNull() ?: context.currentTopic,
            strugglingAreas = if (emotion?.primary == Emotion.FRUSTRATED || emotion?.primary == Emotion.CONFUSED) {
                context.strugglingAreas + messageAnalysis.topics
            } else {
                context.strugglingAreas
            }
        )
    }

    private suspend fun generateAssistantResponse(
        assistant: VirtualTeachingAssistant,
        messageAnalysis: MessageAnalysis,
        context: ConversationContext,
        emotion: DetectedEmotion?
    ): AssistantResponse {
        val responseStrategy = selectResponseStrategy(assistant, messageAnalysis, emotion)
        val responseContent = generateResponseContent(assistant, messageAnalysis, context, responseStrategy)
        
        return AssistantResponse(
            responseId = generateMessageId(),
            conversationId = "", // 会在调用处设置
            content = MessageContent(
                text = responseContent,
                intent = MessageIntent.ANSWER
            ),
            reasoning = ResponseReasoning(
                strategy = responseStrategy,
                knowledgeUsed = listOf("基础知识库"),
                personalityFactors = listOf("友好", "耐心"),
                contextFactors = listOf("学生水平", "当前话题"),
                adaptationReasons = emptyList()
            ),
            adaptations = emptyList(),
            followUpSuggestions = generateFollowUpSuggestions(messageAnalysis),
            timestamp = Clock.System.now()
        )
    }

    private fun selectResponseStrategy(
        assistant: VirtualTeachingAssistant,
        messageAnalysis: MessageAnalysis,
        emotion: DetectedEmotion?
    ): ResponseStrategy {
        return when {
            emotion?.primary == Emotion.FRUSTRATED -> ResponseStrategy.ENCOURAGEMENT_FIRST
            emotion?.primary == Emotion.CONFUSED -> ResponseStrategy.STEP_BY_STEP
            messageAnalysis.intent == MessageIntent.QUESTION -> ResponseStrategy.DIRECT_ANSWER
            else -> ResponseStrategy.GUIDED_DISCOVERY
        }
    }

    private suspend fun generateResponseContent(
        assistant: VirtualTeachingAssistant,
        messageAnalysis: MessageAnalysis,
        context: ConversationContext,
        strategy: ResponseStrategy
    ): String {
        val prompt = """
        作为一个${assistant.specializations.first().name}教学助手，请根据以下信息生成回复：
        
        学生问题类型: ${messageAnalysis.intent}
        涉及话题: ${messageAnalysis.topics.joinToString(", ")}
        学生水平: ${context.studentLevel}
        回复策略: $strategy
        助手个性: 友好度${assistant.personality.friendliness}, 耐心度${assistant.personality.patience}
        
        请生成一个有帮助、友好且适合学生水平的回复。
        """.trimIndent()
        
        return llmService.generate(prompt)
    }

    private fun generateFollowUpSuggestions(messageAnalysis: MessageAnalysis): List<String> {
        return listOf(
            "你还有其他相关问题吗？",
            "需要我提供更多例子吗？",
            "想要练习一些相关题目吗？"
        )
    }

    private fun generateConversationSummary(conversation: AssistantConversation): ConversationSummary {
        val studentMessages = conversation.messages.filter { it.sender == MessageSender.STUDENT }
        val assistantMessages = conversation.messages.filter { it.sender == MessageSender.ASSISTANT }
        
        return ConversationSummary(
            mainTopics = listOf(conversation.context.currentTopic),
            questionsAsked = studentMessages.count { it.content.intent == MessageIntent.QUESTION },
            questionsAnswered = assistantMessages.count { it.content.intent == MessageIntent.ANSWER },
            conceptsExplained = emptyList(),
            difficultiesEncountered = conversation.context.strugglingAreas,
            progressMade = emptyList(),
            recommendedNextSteps = listOf("继续练习相关概念", "复习基础知识"),
            overallSentiment = Sentiment.POSITIVE,
            engagementLevel = EngagementLevel.MODERATE
        )
    }

    private fun calculatePerformanceMetrics(
        assistant: VirtualTeachingAssistant,
        conversations: List<AssistantConversation>,
        timeRange: Pair<Instant, Instant>
    ): AssistantPerformanceMetrics {
        val totalConversations = conversations.size
        val averageResponseTime = if (conversations.isNotEmpty()) {
            conversations.flatMap { it.messages }
                .filter { it.sender == MessageSender.ASSISTANT }
                .map { it.metadata.responseTime }
                .average().let { Duration.parse("${it.toLong()}ms") }
        } else {
            0.seconds
        }
        
        return AssistantPerformanceMetrics(
            assistantId = assistant.assistantId,
            timeRange = timeRange,
            totalConversations = totalConversations,
            averageResponseTime = averageResponseTime,
            studentSatisfactionScore = 4.2f,
            accuracyRate = 0.92f,
            engagementRate = 0.85f,
            problemResolutionRate = 0.88f,
            knowledgeGapIdentificationRate = 0.75f,
            adaptationSuccessRate = 0.82f,
            topPerformingSubjects = assistant.specializations.toList(),
            improvementAreas = listOf("复杂问题处理", "情绪识别准确性")
        )
    }
}

// 辅助数据类
data class MessageAnalysis(
    val intent: MessageIntent,
    val topics: List<String>,
    val concepts: List<String>,
    val difficultyLevel: DifficultyLevel,
    val helpType: HelpType
)

enum class HelpType {
    EXPLANATION, EXAMPLE, HINT, PRACTICE, CLARIFICATION, ENCOURAGEMENT
}
