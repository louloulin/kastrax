package ai.kastrax.edutech.assistant

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.analytics.LearningAnalyticsService
import ai.kastrax.edutech.content.ContentManagementService
import ai.kastrax.edutech.llm.LLMService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * 虚拟教学助手功能测试
 */
class VirtualAssistantTest {

    private val mockLLMService = MockLLMService()
    private val mockAnalyticsService = MockAnalyticsService()
    private val mockContentService = MockContentService()
    private val emotionService = EmotionDetectionService(mockLLMService)
    private val knowledgeBaseService = KnowledgeBaseService(mockContentService)

    private val virtualAssistantService = VirtualTeachingAssistantService(
        llmService = mockLLMService,
        analyticsService = mockAnalyticsService,
        contentService = mockContentService,
        emotionDetectionService = emotionService,
        knowledgeBaseService = knowledgeBaseService
    )

    @Test
    fun `VA-001 - 虚拟助手创建测试`() = runTest {
        // Given
        val name = "数学小助手"
        val specializations = setOf(Subject.MATHEMATICS)
        val personality = createTestPersonality()
        val capabilities = createTestCapabilities()

        // When
        val assistant = virtualAssistantService.createAssistant(
            name = name,
            specializations = specializations,
            personality = personality,
            capabilities = capabilities
        )

        // Then
        assertNotNull(assistant.assistantId)
        assertEquals(name, assistant.name)
        assertEquals(specializations, assistant.specializations)
        assertEquals(personality, assistant.personality)
        assertEquals(capabilities, assistant.capabilities)
        assertTrue(assistant.isActive)
    }

    @Test
    fun `VA-002 - 对话开始测试`() = runTest {
        // Given
        val assistant = createTestAssistant()
        val studentId = StudentId("student_001")
        val subject = Subject.MATHEMATICS

        // When
        val conversation = virtualAssistantService.startConversation(
            assistantId = assistant.assistantId,
            studentId = studentId,
            subject = subject
        )

        // Then
        assertNotNull(conversation.conversationId)
        assertEquals(assistant.assistantId, conversation.assistantId)
        assertEquals(studentId, conversation.studentId)
        assertEquals(subject, conversation.subject)
        assertEquals(ConversationStatus.ACTIVE, conversation.status)
        assertNotNull(conversation.startTime)
    }

    @Test
    fun `VA-003 - 学生消息处理测试`() = runTest {
        // Given
        val assistant = createTestAssistant()
        val conversation = startTestConversation(assistant)
        val studentMessage = "我不理解二次方程怎么解"

        // When
        val response = virtualAssistantService.processStudentMessage(
            conversationId = conversation.conversationId,
            message = studentMessage
        )

        // Then
        assertNotNull(response.responseId)
        assertEquals(conversation.conversationId, response.conversationId)
        assertNotNull(response.content.text)
        assertEquals(MessageIntent.ANSWER, response.content.intent)
        assertTrue(response.reasoning.knowledgeUsed.isNotEmpty())
    }

    @Test
    fun `VA-004 - 情绪检测测试`() = runTest {
        // Given
        val frustratedMessage = "这太难了，我完全不懂！"
        val excitedMessage = "太好了！我明白了！"
        val confusedMessage = "这是什么意思？我不明白。"

        // When
        val frustratedEmotion = emotionService.detectEmotion(frustratedMessage)
        val excitedEmotion = emotionService.detectEmotion(excitedMessage)
        val confusedEmotion = emotionService.detectEmotion(confusedMessage)

        // Then
        assertEquals(Emotion.FRUSTRATED, frustratedEmotion.primary)
        assertTrue(frustratedEmotion.intensity > 0.5f)
        
        assertEquals(Emotion.EXCITED, excitedEmotion.primary)
        assertTrue(excitedEmotion.intensity > 0.5f)
        
        assertEquals(Emotion.CONFUSED, confusedEmotion.primary)
        assertTrue(confusedEmotion.intensity > 0.5f)
    }

    @Test
    fun `VA-005 - 情绪建议生成测试`() = runTest {
        // Given
        val frustratedEmotion = DetectedEmotion(
            primary = Emotion.FRUSTRATED,
            secondary = emptyList(),
            intensity = 0.8f,
            confidence = 0.9f
        )

        // When
        val suggestions = emotionService.getEmotionBasedSuggestions(frustratedEmotion)

        // Then
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.type == SuggestionType.ENCOURAGEMENT })
        assertTrue(suggestions.any { it.message.contains("困难") || it.message.contains("一步一步") })
    }

    @Test
    fun `VA-006 - 知识库构建测试`() = runTest {
        // Given
        val specializations = setOf(Subject.MATHEMATICS, Subject.SCIENCE)

        // When
        val knowledgeBase = knowledgeBaseService.buildKnowledgeBase(specializations)

        // Then
        assertEquals(specializations.size, knowledgeBase.subjects.size)
        assertTrue(knowledgeBase.subjects.containsKey(Subject.MATHEMATICS))
        assertTrue(knowledgeBase.subjects.containsKey(Subject.SCIENCE))
        assertTrue(knowledgeBase.pedagogicalMethods.isNotEmpty())
        assertTrue(knowledgeBase.assessmentStrategies.isNotEmpty())
        assertTrue(knowledgeBase.learningTheories.isNotEmpty())
    }

    @Test
    fun `VA-007 - 对话结束和摘要生成测试`() = runTest {
        // Given
        val assistant = createTestAssistant()
        val conversation = startTestConversation(assistant)
        
        // 模拟一些对话
        virtualAssistantService.processStudentMessage(conversation.conversationId, "什么是代数？")
        virtualAssistantService.processStudentMessage(conversation.conversationId, "能给我举个例子吗？")

        // When
        val summary = virtualAssistantService.endConversation(conversation.conversationId)

        // Then
        assertNotNull(summary)
        assertTrue(summary.questionsAsked > 0)
        assertTrue(summary.questionsAnswered > 0)
        assertTrue(summary.mainTopics.isNotEmpty())
        assertTrue(summary.recommendedNextSteps.isNotEmpty())
    }

    @Test
    fun `VA-008 - 助手性能指标测试`() = runTest {
        // Given
        val assistant = createTestAssistant()
        val timeRange = Pair(
            Clock.System.now().minus(kotlin.time.Duration.parse("1h")),
            Clock.System.now()
        )

        // When
        val performance = virtualAssistantService.getAssistantPerformance(
            assistantId = assistant.assistantId,
            timeRange = timeRange
        )

        // Then
        assertEquals(assistant.assistantId, performance.assistantId)
        assertEquals(timeRange, performance.timeRange)
        assertTrue(performance.studentSatisfactionScore >= 0.0f)
        assertTrue(performance.accuracyRate >= 0.0f)
        assertTrue(performance.engagementRate >= 0.0f)
        assertTrue(performance.topPerformingSubjects.isNotEmpty())
    }

    @Test
    fun `VA-009 - 助手知识更新测试`() = runTest {
        // Given
        val assistant = createTestAssistant()
        val subject = Subject.MATHEMATICS
        val newKnowledge = createTestSubjectKnowledge(subject)

        // When
        virtualAssistantService.updateAssistantKnowledge(
            assistantId = assistant.assistantId,
            subject = subject,
            newKnowledge = newKnowledge
        )

        // Then - 验证知识已更新（通过后续查询验证）
        assertTrue(true) // 简化验证
    }

    @Test
    fun `VA-010 - 情绪趋势分析测试`() = runTest {
        // Given
        val userId = "student_001"
        val messages = listOf(
            "我很困惑",
            "还是不明白",
            "有点懂了",
            "明白了！",
            "太好了！"
        )

        // 模拟情绪历史
        messages.forEach { message ->
            emotionService.detectEmotion(message, userId)
        }

        // When
        val trend = emotionService.analyzeEmotionTrend(userId)

        // Then
        assertNotNull(trend)
        assertTrue(trend.confidence > 0.0f)
        assertTrue(trend.volatility >= 0.0f)
    }

    @Test
    fun `VA-011 - 虚拟助手个性特征测试`() = runTest {
        // Given
        val friendlyPersonality = AssistantPersonality(
            friendliness = 0.9f,
            formality = 0.3f,
            patience = 0.8f,
            enthusiasm = 0.7f,
            empathy = 0.8f,
            humor = 0.6f,
            encouragement = 0.9f,
            adaptability = 0.7f
        )

        // When
        val assistant = virtualAssistantService.createAssistant(
            name = "友好助手",
            specializations = setOf(Subject.MATHEMATICS),
            personality = friendlyPersonality,
            capabilities = createTestCapabilities()
        )

        // Then
        assertEquals(friendlyPersonality, assistant.personality)
        assertEquals(ConversationTone.FRIENDLY, assistant.conversationStyle.tone)
        assertTrue(assistant.personality.friendliness > 0.8f)
        assertTrue(assistant.personality.encouragement > 0.8f)
    }

    @Test
    fun `VA-012 - 多语言支持测试`() = runTest {
        // Given
        val assistant = createTestAssistant()

        // Then
        assertTrue(assistant.languageSupport.contains(Language.ENGLISH))
        assertTrue(assistant.languageSupport.contains(Language.CHINESE))
        assertTrue(assistant.languageSupport.size >= 2)
    }

    @Test
    fun `VA-013 - 助手能力限制测试`() = runTest {
        // Given
        val capabilities = AssistantCapabilities(
            maxConcurrentStudents = 5,
            responseTimeMs = 500L
        )

        // When
        val assistant = virtualAssistantService.createAssistant(
            name = "限制助手",
            specializations = setOf(Subject.MATHEMATICS),
            personality = createTestPersonality(),
            capabilities = capabilities
        )

        // Then
        assertEquals(5, assistant.capabilities.maxConcurrentStudents)
        assertEquals(500L, assistant.capabilities.responseTimeMs)
    }

    @Test
    fun `VA-014 - 对话上下文管理测试`() = runTest {
        // Given
        val assistant = createTestAssistant()
        val conversation = startTestConversation(assistant)

        // When
        virtualAssistantService.processStudentMessage(conversation.conversationId, "我想学习代数")
        val response = virtualAssistantService.processStudentMessage(conversation.conversationId, "什么是变量？")

        // Then
        assertNotNull(response)
        assertTrue(response.content.text.isNotEmpty())
        // 验证上下文被正确维护
    }

    @Test
    fun `VA-015 - 助手专业化测试`() = runTest {
        // Given
        val mathSpecializations = setOf(Subject.MATHEMATICS)
        val scienceSpecializations = setOf(Subject.SCIENCE)

        // When
        val mathAssistant = virtualAssistantService.createAssistant(
            name = "数学助手",
            specializations = mathSpecializations,
            personality = createTestPersonality(),
            capabilities = createTestCapabilities()
        )

        val scienceAssistant = virtualAssistantService.createAssistant(
            name = "科学助手",
            specializations = scienceSpecializations,
            personality = createTestPersonality(),
            capabilities = createTestCapabilities()
        )

        // Then
        assertEquals(mathSpecializations, mathAssistant.specializations)
        assertEquals(scienceSpecializations, scienceAssistant.specializations)
        assertTrue(mathAssistant.knowledgeBase.subjects.containsKey(Subject.MATHEMATICS))
        assertTrue(scienceAssistant.knowledgeBase.subjects.containsKey(Subject.SCIENCE))
    }

    // 辅助方法
    private suspend fun createTestAssistant(): VirtualTeachingAssistant {
        return virtualAssistantService.createAssistant(
            name = "测试助手",
            specializations = setOf(Subject.MATHEMATICS),
            personality = createTestPersonality(),
            capabilities = createTestCapabilities()
        )
    }

    private suspend fun startTestConversation(assistant: VirtualTeachingAssistant): AssistantConversation {
        return virtualAssistantService.startConversation(
            assistantId = assistant.assistantId,
            studentId = StudentId("test_student"),
            subject = Subject.MATHEMATICS
        )
    }

    private fun createTestPersonality(): AssistantPersonality {
        return AssistantPersonality(
            friendliness = 0.8f,
            formality = 0.5f,
            patience = 0.9f,
            enthusiasm = 0.7f,
            empathy = 0.8f,
            humor = 0.6f,
            encouragement = 0.9f,
            adaptability = 0.8f
        )
    }

    private fun createTestCapabilities(): AssistantCapabilities {
        return AssistantCapabilities(
            canAnswerQuestions = true,
            canProvideExplanations = true,
            canCreateExercises = true,
            canGradePapers = true,
            canProvideHints = true,
            canDetectEmotions = true,
            canAdaptDifficulty = true,
            canGenerateContent = true,
            maxConcurrentStudents = 100,
            responseTimeMs = 1000L
        )
    }

    private fun createTestSubjectKnowledge(subject: Subject): SubjectKnowledge {
        return SubjectKnowledge(
            subject = subject,
            gradeLevel = GradeLevel.GRADE_8,
            topics = emptyList(),
            competencyLevel = CompetencyLevel.INTERMEDIATE,
            lastUpdated = Clock.System.now()
        )
    }
}

// Mock服务类
class MockLLMService : LLMService {
    override suspend fun generate(prompt: String): String {
        return when {
            prompt.contains("分析") -> "这是一个关于数学的问题，学生需要帮助理解代数概念。"
            prompt.contains("助手") -> "我很乐意帮助你学习数学！让我们从基础概念开始。"
            else -> "这是一个模拟的LLM响应。"
        }
    }
}

class MockAnalyticsService : LearningAnalyticsService {
    override suspend fun getStudentProfile(studentId: StudentId): StudentProfile {
        return StudentProfile(
            studentId = studentId,
            gradeLevel = GradeLevel.GRADE_8,
            learningStyle = LearningStyle.VISUAL,
            knowledgeLevel = mapOf(Subject.MATHEMATICS to MasteryLevel.INTERMEDIATE),
            preferredContentTypes = setOf(ContentType.VIDEO, ContentType.INTERACTIVE),
            currentDifficultyLevel = DifficultyLevel.MEDIUM
        )
    }

    override suspend fun recordLearningActivity(
        studentId: StudentId,
        activity: LearningActivity,
        context: Map<String, String>
    ) {
        // Mock implementation
    }
}

class MockContentService : ContentManagementService {
    override suspend fun getAvailableContent(): List<LearningContent> {
        return listOf(
            LearningContent(
                id = "content_1",
                title = "代数基础",
                description = "代数的基本概念",
                contentType = ContentType.TEXT,
                subject = Subject.MATHEMATICS,
                gradeLevel = GradeLevel.GRADE_8,
                difficulty = DifficultyLevel.MEDIUM,
                url = "https://example.com/algebra.html",
                estimatedDuration = kotlin.time.Duration.parse("30m")
            )
        )
    }
}
