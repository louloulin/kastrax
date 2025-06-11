package ai.kastrax.edutech.demo

import ai.kastrax.edutech.assistant.*
import ai.kastrax.edutech.models.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 虚拟教学助手功能演示
 * 展示智能虚拟教学助手的核心功能
 */
fun main() = runBlocking {
    println("🤖 Kastrax智能虚拟教学助手演示")
    println("=" * 60)

    // 1. 创建虚拟教学助手
    val assistant = createSampleAssistant()
    println("👨‍🏫 创建虚拟教学助手: ${assistant.name}")
    println("   助手ID: ${assistant.assistantId}")
    println("   专业领域: ${assistant.specializations.joinToString(", ")}")
    println("   个性特征:")
    println("     友好度: ${(assistant.personality.friendliness * 100).toInt()}%")
    println("     耐心度: ${(assistant.personality.patience * 100).toInt()}%")
    println("     热情度: ${(assistant.personality.enthusiasm * 100).toInt()}%")
    println("     鼓励性: ${(assistant.personality.encouragement * 100).toInt()}%")
    println("   语言支持: ${assistant.languageSupport.joinToString(", ")}")
    println("   最大并发学生: ${assistant.capabilities.maxConcurrentStudents}")
    println()

    // 2. 创建学习对话
    val conversation = createSampleConversation(assistant)
    println("💬 创建学习对话: ${conversation.conversationId}")
    println("   学生ID: ${conversation.studentId}")
    println("   学科: ${conversation.subject}")
    println("   对话状态: ${conversation.status}")
    println("   学习目标: ${conversation.context.learningObjectives.joinToString(", ")}")
    println("   学生水平: ${conversation.context.studentLevel}")
    println("   学习风格: ${conversation.context.preferredLearningStyle}")
    println()

    // 3. 模拟学生消息和助手响应
    val studentMessages = listOf(
        "我不理解二次方程怎么解",
        "能给我举个具体的例子吗？",
        "这太难了，我觉得很困惑",
        "哦，我明白了！谢谢老师"
    )

    println("📝 模拟师生对话:")
    println("-" * 40)

    studentMessages.forEachIndexed { index, message ->
        println("学生: $message")
        
        // 检测情绪
        val emotion = detectStudentEmotion(message)
        println("   [情绪检测: ${emotion.primary}, 强度: ${(emotion.intensity * 100).toInt()}%]")
        
        // 生成助手响应
        val response = generateAssistantResponse(assistant, message, emotion)
        println("助手: ${response.content.text}")
        println("   [响应策略: ${response.reasoning.strategy}]")
        
        // 情绪建议
        if (emotion.intensity > 0.6f && emotion.primary in listOf(Emotion.FRUSTRATED, Emotion.CONFUSED)) {
            val suggestions = getEmotionSuggestions(emotion)
            println("   [建议: ${suggestions.firstOrNull()?.message ?: "继续鼓励"}]")
        }
        
        println()
    }

    // 4. 展示知识库内容
    val knowledgeBase = assistant.knowledgeBase
    println("📚 助手知识库:")
    println("   涵盖学科: ${knowledgeBase.subjects.keys.joinToString(", ")}")
    println("   教学方法: ${knowledgeBase.pedagogicalMethods.joinToString(", ")}")
    println("   评估策略: ${knowledgeBase.assessmentStrategies.joinToString(", ")}")
    println("   学习理论: ${knowledgeBase.learningTheories.joinToString(", ")}")
    
    // 展示数学知识主题
    val mathKnowledge = knowledgeBase.subjects[Subject.MATHEMATICS]
    if (mathKnowledge != null && mathKnowledge.topics.isNotEmpty()) {
        println("   数学主题示例:")
        mathKnowledge.topics.take(2).forEach { topic ->
            println("     • ${topic.title}: ${topic.description}")
            println("       难度: ${topic.difficulty}, 概念数: ${topic.concepts.size}")
        }
    }
    println()

    // 5. 展示对话摘要
    val summary = createSampleConversationSummary()
    println("📊 对话摘要:")
    println("   主要话题: ${summary.mainTopics.joinToString(", ")}")
    println("   提问数量: ${summary.questionsAsked}")
    println("   回答数量: ${summary.questionsAnswered}")
    println("   解释概念: ${summary.conceptsExplained.joinToString(", ")}")
    println("   遇到困难: ${summary.difficultiesEncountered.joinToString(", ")}")
    println("   学习进展: ${summary.progressMade.joinToString(", ")}")
    println("   推荐下一步: ${summary.recommendedNextSteps.joinToString(", ")}")
    println("   整体情绪: ${summary.overallSentiment}")
    println("   参与度: ${summary.engagementLevel}")
    println()

    // 6. 展示助手性能指标
    val performance = createSamplePerformanceMetrics(assistant)
    println("📈 助手性能指标:")
    println("   总对话数: ${performance.totalConversations}")
    println("   平均响应时间: ${performance.averageResponseTime}")
    println("   学生满意度: ${performance.studentSatisfactionScore}/5.0")
    println("   准确率: ${(performance.accuracyRate * 100).toInt()}%")
    println("   参与率: ${(performance.engagementRate * 100).toInt()}%")
    println("   问题解决率: ${(performance.problemResolutionRate * 100).toInt()}%")
    println("   知识缺口识别率: ${(performance.knowledgeGapIdentificationRate * 100).toInt()}%")
    println("   适应成功率: ${(performance.adaptationSuccessRate * 100).toInt()}%")
    println("   优势学科: ${performance.topPerformingSubjects.joinToString(", ")}")
    println("   改进领域: ${performance.improvementAreas.joinToString(", ")}")
    println()

    // 7. 展示情绪分析能力
    println("🎭 情绪分析能力演示:")
    val emotionExamples = mapOf(
        "我很兴奋学习新知识！" to Emotion.EXCITED,
        "这个问题让我很困惑..." to Emotion.CONFUSED,
        "太难了，我快要放弃了" to Emotion.FRUSTRATED,
        "我觉得有点无聊" to Emotion.BORED,
        "我担心考试会不及格" to Emotion.ANXIOUS,
        "我明白了，很有成就感！" to Emotion.CONFIDENT
    )

    emotionExamples.forEach { (text, expectedEmotion) ->
        val detected = detectStudentEmotion(text)
        println("   \"$text\"")
        println("     → 检测情绪: ${detected.primary} (强度: ${(detected.intensity * 100).toInt()}%)")
        val suggestions = getEmotionSuggestions(detected)
        if (suggestions.isNotEmpty()) {
            println("     → 应对建议: ${suggestions.first().message}")
        }
        println()
    }

    // 8. 展示适应性学习
    println("🔄 适应性学习演示:")
    println("   助手会根据学生的情绪和学习状态调整教学策略:")
    println("   • 学生困惑时 → 提供更详细的解释和例子")
    println("   • 学生沮丧时 → 增加鼓励和降低难度")
    println("   • 学生无聊时 → 增加互动性和趣味性")
    println("   • 学生兴奋时 → 提供更有挑战性的内容")
    println("   • 学生焦虑时 → 提供安慰和支持")
    println()

    // 9. 展示多模态支持
    println("🎨 多模态支持:")
    println("   助手支持多种教学辅助方式:")
    println("   • 文本解释: 清晰的文字说明")
    println("   • 视觉辅助: 图表、图像、动画")
    println("   • 互动练习: 实时问答和练习")
    println("   • 语音交互: 语音问答和朗读")
    println("   • 个性化内容: 根据学习风格调整")
    println()

    // 10. 总结
    println("✅ 虚拟教学助手功能演示完成！")
    println("🚀 Kastrax智能虚拟教学助手提供:")
    println("   • 🧠 智能情绪识别和适应性响应")
    println("   • 📖 丰富的学科知识库和教学资源")
    println("   • 🎯 个性化教学策略和学习路径")
    println("   • 💬 自然语言对话和多语言支持")
    println("   • 📊 详细的学习分析和进度跟踪")
    println("   • 🔄 实时学习状态调整和优化")
    println("   • 🎨 多模态教学内容和交互方式")
    println("   • 👥 支持大规模并发学习会话")
}

// 辅助函数
private fun createSampleAssistant(): VirtualTeachingAssistant {
    return VirtualTeachingAssistant(
        assistantId = "assistant_demo_001",
        name = "智能数学助手小明",
        personality = AssistantPersonality(
            friendliness = 0.9f,
            formality = 0.4f,
            patience = 0.95f,
            enthusiasm = 0.8f,
            empathy = 0.9f,
            humor = 0.7f,
            encouragement = 0.95f,
            adaptability = 0.85f
        ),
        specializations = setOf(Subject.MATHEMATICS, Subject.SCIENCE),
        capabilities = AssistantCapabilities(
            canAnswerQuestions = true,
            canProvideExplanations = true,
            canCreateExercises = true,
            canGradePapers = true,
            canProvideHints = true,
            canDetectEmotions = true,
            canAdaptDifficulty = true,
            canGenerateContent = true,
            canTranslateLanguages = true,
            canProvideVisualAids = true,
            canConductAssessments = true,
            canTrackProgress = true,
            maxConcurrentStudents = 200,
            responseTimeMs = 500L
        ),
        knowledgeBase = createSampleKnowledgeBase(),
        conversationStyle = ConversationStyle(
            tone = ConversationTone.FRIENDLY,
            complexity = ComplexityLevel.MODERATE,
            verbosity = VerbosityLevel.DETAILED,
            useOfExamples = ExampleUsage.FREQUENT,
            questioningStyle = QuestioningStyle.GUIDED_DISCOVERY,
            feedbackStyle = FeedbackStyle.ENCOURAGING
        ),
        languageSupport = setOf(Language.CHINESE, Language.ENGLISH),
        createdAt = Clock.System.now(),
        lastUpdated = Clock.System.now()
    )
}

private fun createSampleConversation(assistant: VirtualTeachingAssistant): AssistantConversation {
    return AssistantConversation(
        conversationId = "conv_demo_001",
        assistantId = assistant.assistantId,
        studentId = StudentId("student_demo_001"),
        subject = Subject.MATHEMATICS,
        startTime = Clock.System.now(),
        messages = emptyList(),
        context = ConversationContext(
            currentTopic = "二次方程",
            learningObjectives = listOf("理解二次方程概念", "掌握求解方法"),
            studentLevel = DifficultyLevel.MEDIUM,
            previousTopics = listOf("一次方程", "代数基础"),
            strugglingAreas = listOf("符号运算"),
            strengths = listOf("基础计算"),
            preferredLearningStyle = LearningStyle.VISUAL,
            sessionGoals = listOf("完成二次方程练习", "理解判别式")
        ),
        status = ConversationStatus.ACTIVE
    )
}

private fun createSampleKnowledgeBase(): AssistantKnowledgeBase {
    val mathTopics = listOf(
        KnowledgeTopic(
            topicId = "quadratic_equations",
            title = "二次方程",
            description = "形如ax²+bx+c=0的方程及其解法",
            prerequisites = listOf("一次方程", "代数基础"),
            difficulty = DifficultyLevel.MEDIUM,
            concepts = listOf(
                Concept(
                    conceptId = "quadratic_formula",
                    name = "求根公式",
                    definition = "二次方程的通用求解公式",
                    explanation = "x = (-b ± √(b²-4ac)) / 2a",
                    visualAids = emptyList(),
                    relatedConcepts = listOf("判别式", "根的性质")
                )
            ),
            examples = listOf(
                Example(
                    exampleId = "simple_quadratic",
                    title = "简单二次方程",
                    description = "解方程 x² - 5x + 6 = 0",
                    solution = "x = 2 或 x = 3",
                    stepByStep = listOf(
                        SolutionStep(1, "识别系数", "a=1, b=-5, c=6"),
                        SolutionStep(2, "应用求根公式", "x = (5 ± √(25-24)) / 2"),
                        SolutionStep(3, "计算结果", "x = (5 ± 1) / 2 = 2 或 3")
                    ),
                    difficulty = DifficultyLevel.EASY
                )
            ),
            commonMisconceptions = listOf(
                Misconception(
                    misconceptionId = "discriminant_error",
                    description = "判别式计算错误",
                    correctExplanation = "判别式 = b² - 4ac，注意符号",
                    commonCauses = listOf("符号错误", "计算失误"),
                    correctionStrategies = listOf("强调符号规则", "多做练习")
                )
            )
        )
    )

    return AssistantKnowledgeBase(
        subjects = mapOf(
            Subject.MATHEMATICS to SubjectKnowledge(
                subject = Subject.MATHEMATICS,
                gradeLevel = GradeLevel.GRADE_9,
                topics = mathTopics,
                competencyLevel = CompetencyLevel.ADVANCED,
                lastUpdated = Clock.System.now()
            )
        ),
        pedagogicalMethods = setOf(
            TeachingMethod.SOCRATIC_METHOD,
            TeachingMethod.GUIDED_DISCOVERY,
            TeachingMethod.PROBLEM_BASED
        ),
        assessmentStrategies = setOf(
            AssessmentStrategy.FORMATIVE_ASSESSMENT,
            AssessmentStrategy.ADAPTIVE_ASSESSMENT
        ),
        learningTheories = setOf(
            LearningTheory.CONSTRUCTIVISM,
            LearningTheory.SOCIAL_LEARNING_THEORY
        ),
        lastKnowledgeUpdate = Clock.System.now()
    )
}

private fun detectStudentEmotion(message: String): DetectedEmotion {
    return when {
        message.contains("困惑") || message.contains("不明白") -> DetectedEmotion(
            primary = Emotion.CONFUSED,
            secondary = emptyList(),
            intensity = 0.7f,
            confidence = 0.8f
        )
        message.contains("难") || message.contains("困难") -> DetectedEmotion(
            primary = Emotion.FRUSTRATED,
            secondary = emptyList(),
            intensity = 0.8f,
            confidence = 0.9f
        )
        message.contains("明白") || message.contains("谢谢") -> DetectedEmotion(
            primary = Emotion.HAPPY,
            secondary = emptyList(),
            intensity = 0.8f,
            confidence = 0.9f
        )
        message.contains("兴奋") || message.contains("太好了") -> DetectedEmotion(
            primary = Emotion.EXCITED,
            secondary = emptyList(),
            intensity = 0.9f,
            confidence = 0.9f
        )
        message.contains("无聊") -> DetectedEmotion(
            primary = Emotion.BORED,
            secondary = emptyList(),
            intensity = 0.7f,
            confidence = 0.8f
        )
        message.contains("担心") || message.contains("焦虑") -> DetectedEmotion(
            primary = Emotion.ANXIOUS,
            secondary = emptyList(),
            intensity = 0.7f,
            confidence = 0.8f
        )
        else -> DetectedEmotion(
            primary = Emotion.SATISFIED,
            secondary = emptyList(),
            intensity = 0.5f,
            confidence = 0.6f
        )
    }
}

private fun generateAssistantResponse(
    assistant: VirtualTeachingAssistant,
    message: String,
    emotion: DetectedEmotion
): AssistantResponse {
    val responseText = when (emotion.primary) {
        Emotion.CONFUSED -> "我理解你的困惑。让我用更简单的方式来解释这个概念。"
        Emotion.FRUSTRATED -> "我知道这可能有些困难，但不要担心，我们一步一步来解决。"
        Emotion.HAPPY -> "太好了！看到你理解了我很高兴。我们继续学习下一个概念吧！"
        Emotion.EXCITED -> "你的热情很棒！让我们尝试一些更有挑战性的问题。"
        Emotion.BORED -> "让我们换个更有趣的方式来学习这个内容！"
        Emotion.ANXIOUS -> "不用担心，学习是一个过程。我会一直在这里帮助你。"
        else -> "我很乐意帮助你学习。有什么具体问题吗？"
    }

    return AssistantResponse(
        responseId = "resp_demo_${Clock.System.now().toEpochMilliseconds()}",
        conversationId = "conv_demo_001",
        content = MessageContent(
            text = responseText,
            intent = MessageIntent.ANSWER
        ),
        reasoning = ResponseReasoning(
            strategy = when (emotion.primary) {
                Emotion.CONFUSED -> ResponseStrategy.STEP_BY_STEP
                Emotion.FRUSTRATED -> ResponseStrategy.ENCOURAGEMENT_FIRST
                Emotion.EXCITED -> ResponseStrategy.CHALLENGE_INCREASE
                else -> ResponseStrategy.DIRECT_ANSWER
            },
            knowledgeUsed = listOf("情绪识别", "教学策略"),
            personalityFactors = listOf("耐心", "鼓励"),
            contextFactors = listOf("学生情绪", "学习进度"),
            adaptationReasons = listOf("根据情绪调整")
        ),
        adaptations = emptyList(),
        followUpSuggestions = listOf("需要更多解释吗？", "想要练习题吗？"),
        timestamp = Clock.System.now()
    )
}

private fun getEmotionSuggestions(emotion: DetectedEmotion): List<EmotionSuggestion> {
    return when (emotion.primary) {
        Emotion.FRUSTRATED -> listOf(
            EmotionSuggestion(
                type = SuggestionType.ENCOURAGEMENT,
                message = "提供鼓励和支持，降低学习难度",
                action = "调整教学策略"
            )
        )
        Emotion.CONFUSED -> listOf(
            EmotionSuggestion(
                type = SuggestionType.CLARIFICATION,
                message = "提供更清晰的解释和具体例子",
                action = "增加解释详细度"
            )
        )
        else -> emptyList()
    }
}

private fun createSampleConversationSummary(): ConversationSummary {
    return ConversationSummary(
        mainTopics = listOf("二次方程", "求根公式", "判别式"),
        questionsAsked = 4,
        questionsAnswered = 4,
        conceptsExplained = listOf("二次方程定义", "求根公式", "解题步骤"),
        difficultiesEncountered = listOf("符号运算", "公式记忆"),
        progressMade = listOf("理解基本概念", "掌握解题方法"),
        recommendedNextSteps = listOf("练习更多例题", "学习根的性质", "应用题练习"),
        overallSentiment = Sentiment.POSITIVE,
        engagementLevel = EngagementLevel.HIGH
    )
}

private fun createSamplePerformanceMetrics(assistant: VirtualTeachingAssistant): AssistantPerformanceMetrics {
    return AssistantPerformanceMetrics(
        assistantId = assistant.assistantId,
        timeRange = Pair(
            Clock.System.now().minus(kotlin.time.Duration.parse("24h")),
            Clock.System.now()
        ),
        totalConversations = 45,
        averageResponseTime = 650.milliseconds,
        studentSatisfactionScore = 4.6f,
        accuracyRate = 0.94f,
        engagementRate = 0.89f,
        problemResolutionRate = 0.91f,
        knowledgeGapIdentificationRate = 0.82f,
        adaptationSuccessRate = 0.87f,
        topPerformingSubjects = listOf(Subject.MATHEMATICS, Subject.SCIENCE),
        improvementAreas = listOf("复杂问题分解", "创意教学方法")
    )
}

private operator fun String.times(n: Int): String = this.repeat(n)
