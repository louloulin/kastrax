package ai.kastrax.edutech.assistant

import ai.kastrax.edutech.models.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * 简化的虚拟教学助手测试
 * 专注于数据模型和核心逻辑验证
 */
class SimpleVirtualAssistantTest {

    @Test
    fun `SVA-001 - 虚拟助手模型创建测试`() = runTest {
        // Given
        val assistant = VirtualTeachingAssistant(
            assistantId = "assistant_001",
            name = "数学小助手",
            personality = AssistantPersonality(
                friendliness = 0.8f,
                formality = 0.5f,
                patience = 0.9f,
                enthusiasm = 0.7f,
                empathy = 0.8f,
                humor = 0.6f,
                encouragement = 0.9f,
                adaptability = 0.8f
            ),
            specializations = setOf(Subject.MATHEMATICS),
            capabilities = AssistantCapabilities(
                canAnswerQuestions = true,
                canProvideExplanations = true,
                canCreateExercises = true,
                maxConcurrentStudents = 50
            ),
            knowledgeBase = AssistantKnowledgeBase(
                subjects = mapOf(Subject.MATHEMATICS to createTestSubjectKnowledge()),
                pedagogicalMethods = setOf(TeachingMethod.SOCRATIC_METHOD),
                assessmentStrategies = setOf(AssessmentStrategy.FORMATIVE_ASSESSMENT),
                learningTheories = setOf(LearningTheory.CONSTRUCTIVISM),
                lastKnowledgeUpdate = Clock.System.now()
            ),
            conversationStyle = ConversationStyle(
                tone = ConversationTone.FRIENDLY,
                complexity = ComplexityLevel.MODERATE,
                verbosity = VerbosityLevel.MODERATE,
                useOfExamples = ExampleUsage.MODERATE,
                questioningStyle = QuestioningStyle.GUIDED_DISCOVERY,
                feedbackStyle = FeedbackStyle.ENCOURAGING
            ),
            languageSupport = setOf(Language.ENGLISH, Language.CHINESE),
            createdAt = Clock.System.now(),
            lastUpdated = Clock.System.now()
        )

        // Then
        assertEquals("assistant_001", assistant.assistantId)
        assertEquals("数学小助手", assistant.name)
        assertTrue(assistant.personality.friendliness > 0.7f)
        assertTrue(assistant.specializations.contains(Subject.MATHEMATICS))
        assertTrue(assistant.capabilities.canAnswerQuestions)
        assertEquals(50, assistant.capabilities.maxConcurrentStudents)
        assertTrue(assistant.isActive)
    }

    @Test
    fun `SVA-002 - 助手个性特征测试`() = runTest {
        // Given
        val personality = AssistantPersonality(
            friendliness = 0.9f,
            formality = 0.2f,
            patience = 1.0f,
            enthusiasm = 0.8f,
            empathy = 0.9f,
            humor = 0.7f,
            encouragement = 1.0f,
            adaptability = 0.8f
        )

        // Then
        assertEquals(0.9f, personality.friendliness)
        assertEquals(0.2f, personality.formality)
        assertEquals(1.0f, personality.patience)
        assertEquals(0.8f, personality.enthusiasm)
        assertEquals(0.9f, personality.empathy)
        assertEquals(0.7f, personality.humor)
        assertEquals(1.0f, personality.encouragement)
        assertEquals(0.8f, personality.adaptability)
    }

    @Test
    fun `SVA-003 - 助手能力配置测试`() = runTest {
        // Given
        val capabilities = AssistantCapabilities(
            canAnswerQuestions = true,
            canProvideExplanations = true,
            canCreateExercises = true,
            canGradePapers = true,
            canProvideHints = true,
            canDetectEmotions = true,
            canAdaptDifficulty = true,
            canGenerateContent = true,
            canTranslateLanguages = false,
            canProvideVisualAids = true,
            canConductAssessments = true,
            canTrackProgress = true,
            maxConcurrentStudents = 100,
            responseTimeMs = 500L
        )

        // Then
        assertTrue(capabilities.canAnswerQuestions)
        assertTrue(capabilities.canProvideExplanations)
        assertTrue(capabilities.canCreateExercises)
        assertTrue(capabilities.canGradePapers)
        assertTrue(capabilities.canProvideHints)
        assertTrue(capabilities.canDetectEmotions)
        assertTrue(capabilities.canAdaptDifficulty)
        assertTrue(capabilities.canGenerateContent)
        assertFalse(capabilities.canTranslateLanguages)
        assertTrue(capabilities.canProvideVisualAids)
        assertTrue(capabilities.canConductAssessments)
        assertTrue(capabilities.canTrackProgress)
        assertEquals(100, capabilities.maxConcurrentStudents)
        assertEquals(500L, capabilities.responseTimeMs)
    }

    @Test
    fun `SVA-004 - 对话会话模型测试`() = runTest {
        // Given
        val conversation = AssistantConversation(
            conversationId = "conv_001",
            assistantId = "assistant_001",
            studentId = StudentId("student_001"),
            subject = Subject.MATHEMATICS,
            startTime = Clock.System.now(),
            messages = listOf(
                ConversationMessage(
                    messageId = "msg_001",
                    sender = MessageSender.STUDENT,
                    content = MessageContent(
                        text = "我不理解二次方程",
                        intent = MessageIntent.QUESTION
                    ),
                    timestamp = Clock.System.now(),
                    metadata = MessageMetadata(
                        responseTime = kotlin.time.Duration.parse("0ms"),
                        processingSteps = emptyList(),
                        knowledgeUsed = emptyList(),
                        confidence = 1.0f
                    )
                )
            ),
            context = ConversationContext(
                currentTopic = "二次方程",
                learningObjectives = listOf("理解二次方程的概念"),
                studentLevel = DifficultyLevel.MEDIUM,
                previousTopics = emptyList(),
                strugglingAreas = listOf("代数"),
                strengths = emptyList(),
                preferredLearningStyle = LearningStyle.VISUAL,
                sessionGoals = listOf("掌握二次方程解法")
            ),
            status = ConversationStatus.ACTIVE
        )

        // Then
        assertEquals("conv_001", conversation.conversationId)
        assertEquals("assistant_001", conversation.assistantId)
        assertEquals(StudentId("student_001"), conversation.studentId)
        assertEquals(Subject.MATHEMATICS, conversation.subject)
        assertEquals(ConversationStatus.ACTIVE, conversation.status)
        assertEquals(1, conversation.messages.size)
        assertEquals("二次方程", conversation.context.currentTopic)
        assertEquals(DifficultyLevel.MEDIUM, conversation.context.studentLevel)
    }

    @Test
    fun `SVA-005 - 情绪检测模型测试`() = runTest {
        // Given
        val detectedEmotion = DetectedEmotion(
            primary = Emotion.FRUSTRATED,
            secondary = listOf(Emotion.CONFUSED),
            intensity = 0.8f,
            confidence = 0.9f
        )

        // Then
        assertEquals(Emotion.FRUSTRATED, detectedEmotion.primary)
        assertTrue(detectedEmotion.secondary.contains(Emotion.CONFUSED))
        assertEquals(0.8f, detectedEmotion.intensity)
        assertEquals(0.9f, detectedEmotion.confidence)
    }

    @Test
    fun `SVA-006 - 助手响应模型测试`() = runTest {
        // Given
        val response = AssistantResponse(
            responseId = "resp_001",
            conversationId = "conv_001",
            content = MessageContent(
                text = "让我来帮你理解二次方程。二次方程是形如ax²+bx+c=0的方程。",
                intent = MessageIntent.EXPLANATION
            ),
            reasoning = ResponseReasoning(
                strategy = ResponseStrategy.STEP_BY_STEP,
                knowledgeUsed = listOf("二次方程定义", "代数基础"),
                personalityFactors = listOf("耐心", "友好"),
                contextFactors = listOf("学生困惑", "数学主题"),
                adaptationReasons = listOf("简化解释")
            ),
            adaptations = listOf(
                ResponseAdaptation(
                    adaptationType = AdaptationType.DIFFICULTY_ADJUSTMENT,
                    reason = "学生表现困惑",
                    originalValue = "复杂解释",
                    adaptedValue = "简化解释"
                )
            ),
            followUpSuggestions = listOf(
                "需要我举个具体例子吗？",
                "想要练习一些简单的二次方程吗？"
            ),
            timestamp = Clock.System.now()
        )

        // Then
        assertEquals("resp_001", response.responseId)
        assertEquals("conv_001", response.conversationId)
        assertEquals(MessageIntent.EXPLANATION, response.content.intent)
        assertEquals(ResponseStrategy.STEP_BY_STEP, response.reasoning.strategy)
        assertTrue(response.reasoning.knowledgeUsed.contains("二次方程定义"))
        assertEquals(1, response.adaptations.size)
        assertEquals(2, response.followUpSuggestions.size)
    }

    @Test
    fun `SVA-007 - 知识主题模型测试`() = runTest {
        // Given
        val topic = KnowledgeTopic(
            topicId = "algebra_basics",
            title = "代数基础",
            description = "代数的基本概念和运算",
            prerequisites = emptyList(),
            difficulty = DifficultyLevel.MEDIUM,
            concepts = listOf(
                Concept(
                    conceptId = "variable",
                    name = "变量",
                    definition = "代表未知数的符号",
                    explanation = "变量通常用字母表示，如x、y、z",
                    visualAids = emptyList(),
                    relatedConcepts = listOf("方程", "表达式")
                )
            ),
            examples = listOf(
                Example(
                    exampleId = "linear_eq",
                    title = "一元一次方程",
                    description = "解方程 2x + 3 = 7",
                    solution = "x = 2",
                    stepByStep = listOf(
                        SolutionStep(1, "移项", "2x = 7 - 3"),
                        SolutionStep(2, "化简", "2x = 4"),
                        SolutionStep(3, "求解", "x = 2")
                    ),
                    difficulty = DifficultyLevel.EASY
                )
            ),
            commonMisconceptions = listOf(
                Misconception(
                    misconceptionId = "sign_error",
                    description = "移项时符号错误",
                    correctExplanation = "移项时要改变符号",
                    commonCauses = listOf("规则理解不清"),
                    correctionStrategies = listOf("强调规则", "多练习")
                )
            )
        )

        // Then
        assertEquals("algebra_basics", topic.topicId)
        assertEquals("代数基础", topic.title)
        assertEquals(DifficultyLevel.MEDIUM, topic.difficulty)
        assertEquals(1, topic.concepts.size)
        assertEquals(1, topic.examples.size)
        assertEquals(1, topic.commonMisconceptions.size)
        assertEquals("变量", topic.concepts.first().name)
        assertEquals("一元一次方程", topic.examples.first().title)
        assertEquals(3, topic.examples.first().stepByStep.size)
    }

    @Test
    fun `SVA-008 - 对话摘要模型测试`() = runTest {
        // Given
        val summary = ConversationSummary(
            mainTopics = listOf("二次方程", "代数基础"),
            questionsAsked = 3,
            questionsAnswered = 3,
            conceptsExplained = listOf("变量", "方程", "解法"),
            difficultiesEncountered = listOf("符号运算", "移项规则"),
            progressMade = listOf("理解变量概念", "掌握基本解法"),
            recommendedNextSteps = listOf("练习更多例题", "学习复杂方程"),
            overallSentiment = Sentiment.POSITIVE,
            engagementLevel = EngagementLevel.HIGH
        )

        // Then
        assertEquals(2, summary.mainTopics.size)
        assertEquals(3, summary.questionsAsked)
        assertEquals(3, summary.questionsAnswered)
        assertEquals(3, summary.conceptsExplained.size)
        assertEquals(2, summary.difficultiesEncountered.size)
        assertEquals(2, summary.progressMade.size)
        assertEquals(2, summary.recommendedNextSteps.size)
        assertEquals(Sentiment.POSITIVE, summary.overallSentiment)
        assertEquals(EngagementLevel.HIGH, summary.engagementLevel)
    }

    @Test
    fun `SVA-009 - 助手性能指标测试`() = runTest {
        // Given
        val metrics = AssistantPerformanceMetrics(
            assistantId = "assistant_001",
            timeRange = Pair(
                Clock.System.now().minus(kotlin.time.Duration.parse("24h")),
                Clock.System.now()
            ),
            totalConversations = 25,
            averageResponseTime = kotlin.time.Duration.parse("800ms"),
            studentSatisfactionScore = 4.3f,
            accuracyRate = 0.92f,
            engagementRate = 0.87f,
            problemResolutionRate = 0.89f,
            knowledgeGapIdentificationRate = 0.78f,
            adaptationSuccessRate = 0.85f,
            topPerformingSubjects = listOf(Subject.MATHEMATICS, Subject.SCIENCE),
            improvementAreas = listOf("复杂问题处理", "情绪识别准确性")
        )

        // Then
        assertEquals("assistant_001", metrics.assistantId)
        assertEquals(25, metrics.totalConversations)
        assertEquals(kotlin.time.Duration.parse("800ms"), metrics.averageResponseTime)
        assertEquals(4.3f, metrics.studentSatisfactionScore)
        assertEquals(0.92f, metrics.accuracyRate)
        assertEquals(0.87f, metrics.engagementRate)
        assertEquals(0.89f, metrics.problemResolutionRate)
        assertEquals(0.78f, metrics.knowledgeGapIdentificationRate)
        assertEquals(0.85f, metrics.adaptationSuccessRate)
        assertEquals(2, metrics.topPerformingSubjects.size)
        assertEquals(2, metrics.improvementAreas.size)
    }

    @Test
    fun `SVA-010 - 视觉辅助模型测试`() = runTest {
        // Given
        val visualAid = VisualAid(
            aidId = "diagram_001",
            type = VisualAidType.DIAGRAM,
            url = "/images/quadratic_formula.png",
            description = "二次方程求根公式图解",
            altText = "显示二次方程ax²+bx+c=0的求根公式"
        )

        // Then
        assertEquals("diagram_001", visualAid.aidId)
        assertEquals(VisualAidType.DIAGRAM, visualAid.type)
        assertEquals("/images/quadratic_formula.png", visualAid.url)
        assertEquals("二次方程求根公式图解", visualAid.description)
        assertEquals("显示二次方程ax²+bx+c=0的求根公式", visualAid.altText)
    }

    @Test
    fun `SVA-011 - 对话风格配置测试`() = runTest {
        // Given
        val conversationStyle = ConversationStyle(
            tone = ConversationTone.ENCOURAGING,
            complexity = ComplexityLevel.SIMPLE,
            verbosity = VerbosityLevel.DETAILED,
            useOfExamples = ExampleUsage.FREQUENT,
            questioningStyle = QuestioningStyle.SOCRATIC,
            feedbackStyle = FeedbackStyle.IMMEDIATE
        )

        // Then
        assertEquals(ConversationTone.ENCOURAGING, conversationStyle.tone)
        assertEquals(ComplexityLevel.SIMPLE, conversationStyle.complexity)
        assertEquals(VerbosityLevel.DETAILED, conversationStyle.verbosity)
        assertEquals(ExampleUsage.FREQUENT, conversationStyle.useOfExamples)
        assertEquals(QuestioningStyle.SOCRATIC, conversationStyle.questioningStyle)
        assertEquals(FeedbackStyle.IMMEDIATE, conversationStyle.feedbackStyle)
    }

    @Test
    fun `SVA-012 - 消息内容模型测试`() = runTest {
        // Given
        val messageContent = MessageContent(
            text = "我理解了！谢谢老师的解释。",
            attachments = listOf(
                MessageAttachment(
                    attachmentId = "att_001",
                    type = AttachmentType.IMAGE,
                    url = "/uploads/student_work.jpg",
                    description = "学生作业图片",
                    size = 1024000L
                )
            ),
            intent = MessageIntent.FEEDBACK,
            emotion = DetectedEmotion(
                primary = Emotion.HAPPY,
                secondary = emptyList(),
                intensity = 0.8f,
                confidence = 0.9f
            ),
            confidence = 0.95f
        )

        // Then
        assertEquals("我理解了！谢谢老师的解释。", messageContent.text)
        assertEquals(1, messageContent.attachments.size)
        assertEquals(MessageIntent.FEEDBACK, messageContent.intent)
        assertEquals(Emotion.HAPPY, messageContent.emotion?.primary)
        assertEquals(0.95f, messageContent.confidence)
        assertEquals(AttachmentType.IMAGE, messageContent.attachments.first().type)
        assertEquals(1024000L, messageContent.attachments.first().size)
    }

    @Test
    fun `SVA-013 - 语言支持测试`() = runTest {
        // Given
        val multilingualAssistant = VirtualTeachingAssistant(
            assistantId = "multilingual_001",
            name = "多语言助手",
            personality = createTestPersonality(),
            specializations = setOf(Subject.FOREIGN_LANGUAGE),
            capabilities = createTestCapabilities(),
            knowledgeBase = createTestKnowledgeBase(),
            conversationStyle = createTestConversationStyle(),
            languageSupport = setOf(
                Language.ENGLISH,
                Language.CHINESE,
                Language.SPANISH,
                Language.FRENCH,
                Language.JAPANESE
            ),
            createdAt = Clock.System.now(),
            lastUpdated = Clock.System.now()
        )

        // Then
        assertEquals(5, multilingualAssistant.languageSupport.size)
        assertTrue(multilingualAssistant.languageSupport.contains(Language.ENGLISH))
        assertTrue(multilingualAssistant.languageSupport.contains(Language.CHINESE))
        assertTrue(multilingualAssistant.languageSupport.contains(Language.SPANISH))
        assertTrue(multilingualAssistant.languageSupport.contains(Language.FRENCH))
        assertTrue(multilingualAssistant.languageSupport.contains(Language.JAPANESE))
    }

    @Test
    fun `SVA-014 - 教学方法枚举测试`() = runTest {
        // Given & Then
        val methods = TeachingMethod.values()
        
        assertTrue(methods.contains(TeachingMethod.SOCRATIC_METHOD))
        assertTrue(methods.contains(TeachingMethod.DIRECT_INSTRUCTION))
        assertTrue(methods.contains(TeachingMethod.INQUIRY_BASED))
        assertTrue(methods.contains(TeachingMethod.COLLABORATIVE_LEARNING))
        assertTrue(methods.contains(TeachingMethod.PROBLEM_BASED))
        assertTrue(methods.contains(TeachingMethod.EXPERIENTIAL_LEARNING))
        assertTrue(methods.contains(TeachingMethod.FLIPPED_CLASSROOM))
        assertTrue(methods.contains(TeachingMethod.GAMIFICATION))
    }

    @Test
    fun `SVA-015 - 评估策略枚举测试`() = runTest {
        // Given & Then
        val strategies = AssessmentStrategy.values()
        
        assertTrue(strategies.contains(AssessmentStrategy.FORMATIVE_ASSESSMENT))
        assertTrue(strategies.contains(AssessmentStrategy.SUMMATIVE_ASSESSMENT))
        assertTrue(strategies.contains(AssessmentStrategy.PEER_ASSESSMENT))
        assertTrue(strategies.contains(AssessmentStrategy.SELF_ASSESSMENT))
        assertTrue(strategies.contains(AssessmentStrategy.AUTHENTIC_ASSESSMENT))
        assertTrue(strategies.contains(AssessmentStrategy.DIAGNOSTIC_ASSESSMENT))
        assertTrue(strategies.contains(AssessmentStrategy.ADAPTIVE_ASSESSMENT))
    }

    // 辅助方法
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
            maxConcurrentStudents = 50
        )
    }

    private fun createTestKnowledgeBase(): AssistantKnowledgeBase {
        return AssistantKnowledgeBase(
            subjects = mapOf(Subject.MATHEMATICS to createTestSubjectKnowledge()),
            pedagogicalMethods = setOf(TeachingMethod.SOCRATIC_METHOD),
            assessmentStrategies = setOf(AssessmentStrategy.FORMATIVE_ASSESSMENT),
            learningTheories = setOf(LearningTheory.CONSTRUCTIVISM),
            lastKnowledgeUpdate = Clock.System.now()
        )
    }

    private fun createTestConversationStyle(): ConversationStyle {
        return ConversationStyle(
            tone = ConversationTone.FRIENDLY,
            complexity = ComplexityLevel.MODERATE,
            verbosity = VerbosityLevel.MODERATE,
            useOfExamples = ExampleUsage.MODERATE,
            questioningStyle = QuestioningStyle.GUIDED_DISCOVERY,
            feedbackStyle = FeedbackStyle.ENCOURAGING
        )
    }

    private fun createTestSubjectKnowledge(): SubjectKnowledge {
        return SubjectKnowledge(
            subject = Subject.MATHEMATICS,
            gradeLevel = GradeLevel.GRADE_8,
            topics = emptyList(),
            competencyLevel = CompetencyLevel.INTERMEDIATE,
            lastUpdated = Clock.System.now()
        )
    }
}
