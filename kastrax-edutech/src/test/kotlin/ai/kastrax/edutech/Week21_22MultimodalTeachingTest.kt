package ai.kastrax.edutech

import ai.kastrax.edutech.multimodal.*
import ai.kastrax.edutech.models.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Week 21-22 多模态智能教学助手测试
 * 
 * 测试范围：
 * - 语音交互教学助手
 * - 视觉内容理解和生成
 * - 多模态学习内容创建
 * - 智能问答和解释系统
 * - 个性化教学策略推荐
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Week21_22MultimodalTeachingTest {
    
    private lateinit var teachingAssistant: MultimodalTeachingAssistant
    private lateinit var teachingService: MultimodalTeachingService
    
    @BeforeAll
    fun setup() {
        println("🚀 初始化Week 21-22多模态智能教学助手测试环境...")
        
        teachingAssistant = MultimodalTeachingAssistant()
        teachingService = MultimodalTeachingService()
        
        println("✅ Week 21-22多模态智能教学助手测试环境初始化完成")
    }
    
    @Test
    @DisplayName("MT-001: 语音交互教学助手测试")
    fun testVoiceInteractionTeachingAssistant() = runBlocking {
        println("\n🎤 测试语音交互教学助手...")
        
        // 1. 准备测试数据
        val studentId = StudentId.generate()
        val audioInput = AudioInput(
            audioData = "模拟音频数据".toByteArray(),
            format = "wav",
            sampleRate = 44100,
            duration = 3.seconds
        )
        
        val teachingContext = TeachingContext(
            subject = Subject.MATHEMATICS,
            currentTopic = "二次方程",
            difficultyLevel = DifficultyLevel.INTERMEDIATE,
            sessionType = SessionType.Q_AND_A,
            studentLevel = "高中"
        )
        
        // 2. 执行语音交互
        val voiceResult = teachingAssistant.processVoiceInteraction(
            studentId = studentId,
            audioInput = audioInput,
            context = teachingContext
        )
        
        // 3. 验证结果
        assertTrue(voiceResult is VoiceInteractionResult.Success, "语音交互应该成功")
        val interaction = (voiceResult as VoiceInteractionResult.Success).interaction
        
        assertNotNull(interaction, "语音交互结果不应为空")
        assertEquals(studentId, interaction.studentId, "交互应该属于正确的学生")
        assertEquals(teachingContext, interaction.context, "教学上下文应该匹配")
        assertNotNull(interaction.input, "语音识别结果不应为空")
        assertNotNull(interaction.response, "教学响应不应为空")
        assertNotNull(interaction.audioOutput, "音频输出不应为空")
        assertTrue(interaction.input.confidence > 0.0, "识别置信度应该大于0")
        
        println("✅ 语音交互教学助手测试通过")
        println("   识别文本: ${interaction.input.text}")
        println("   识别置信度: ${String.format("%.2f", interaction.input.confidence)}")
        println("   教学意图: ${interaction.intent.intent}")
        println("   响应文本: ${interaction.response.text}")
    }
    
    @Test
    @DisplayName("MT-002: 视觉内容理解和生成测试")
    fun testVisualContentProcessing() = runBlocking {
        println("\n👁️ 测试视觉内容理解和生成...")
        
        val studentId = StudentId.generate()
        val visualInput = VisualInput(
            imageData = "模拟图像数据".toByteArray(),
            format = "png",
            width = 800,
            height = 600,
            metadata = mapOf("source" to "student_upload", "type" to "math_problem")
        )
        
        val processingRequest = VisualProcessingRequest(
            enableImageUnderstanding = true,
            enableTextExtraction = true,
            enableContentGeneration = true,
            enableQuestionGeneration = true,
            subject = Subject.MATHEMATICS,
            difficultyLevel = DifficultyLevel.INTERMEDIATE
        )
        
        // 执行视觉内容处理
        val visualResult = teachingAssistant.processVisualContent(
            studentId = studentId,
            visualInput = visualInput,
            processingRequest = processingRequest
        )
        
        assertTrue(visualResult is VisualProcessingResult.Success, "视觉内容处理应该成功")
        val processing = (visualResult as VisualProcessingResult.Success).processing
        
        assertNotNull(processing, "视觉处理结果不应为空")
        assertEquals(studentId, processing.studentId, "处理应该属于正确的学生")
        assertEquals(visualInput, processing.input, "输入应该匹配")
        assertEquals(processingRequest, processing.request, "请求应该匹配")
        assertTrue(processing.outputs.isNotEmpty(), "应该有处理输出")
        assertTrue(processing.processingTime.inWholeSeconds > 0, "处理时间应该大于0")
        
        // 验证不同类型的输出
        val outputTypes = processing.outputs.map { it.type }.toSet()
        assertTrue(outputTypes.contains(VisualOutputType.IMAGE_ANALYSIS), "应该包含图像分析")
        assertTrue(outputTypes.contains(VisualOutputType.TEXT_EXTRACTION), "应该包含文本提取")
        assertTrue(outputTypes.contains(VisualOutputType.TEACHING_CONTENT), "应该包含教学内容")
        assertTrue(outputTypes.contains(VisualOutputType.QUESTIONS), "应该包含问题生成")
        
        println("✅ 视觉内容理解和生成测试通过")
        println("   处理输出数量: ${processing.outputs.size}")
        println("   输出类型: ${outputTypes.joinToString(", ")}")
        println("   处理时间: ${processing.processingTime}")
    }
    
    @Test
    @DisplayName("MT-003: 多模态学习内容创建测试")
    fun testMultimodalContentCreation() = runBlocking {
        println("\n🎨 测试多模态学习内容创建...")
        
        val contentRequest = MultimodalContentRequest(
            topic = "光合作用",
            difficultyLevel = DifficultyLevel.INTERMEDIATE,
            targetAudience = "初中生",
            estimatedDuration = 30.minutes,
            includeText = true,
            includeVisuals = true,
            includeAudio = true,
            includeInteractive = true,
            visualStyle = VisualStyle.EDUCATIONAL,
            voiceSettings = VoiceSettings(voice = "teacher_female", speed = 1.0)
        )
        
        val teachingObjectives = listOf(
            "理解光合作用的基本概念",
            "掌握光合作用的化学方程式",
            "了解光合作用的生物学意义"
        )
        
        // 执行多模态内容创建
        val contentResult = teachingAssistant.createMultimodalContent(
            contentRequest = contentRequest,
            teachingObjectives = teachingObjectives
        )
        
        assertTrue(contentResult is MultimodalContentResult.Success, "多模态内容创建应该成功")
        val content = (contentResult as MultimodalContentResult.Success).content
        
        assertNotNull(content, "多模态内容不应为空")
        assertEquals(contentRequest.topic, content.topic, "主题应该匹配")
        assertEquals(teachingObjectives, content.objectives, "教学目标应该匹配")
        assertTrue(content.components.isNotEmpty(), "应该有内容组件")
        assertNotNull(content.integratedContent, "应该有整合内容")
        assertTrue(content.metadata.isNotEmpty(), "应该有元数据")
        
        // 验证不同类型的内容组件
        val componentTypes = content.components.map { it.type }.toSet()
        assertTrue(componentTypes.contains(ContentType.TEXT), "应该包含文本内容")
        assertTrue(componentTypes.contains(ContentType.IMAGE), "应该包含图像内容")
        assertTrue(componentTypes.contains(ContentType.AUDIO), "应该包含音频内容")
        assertTrue(componentTypes.contains(ContentType.INTERACTIVE), "应该包含交互内容")
        
        println("✅ 多模态学习内容创建测试通过")
        println("   内容主题: ${content.topic}")
        println("   组件数量: ${content.components.size}")
        println("   组件类型: ${componentTypes.joinToString(", ")}")
        println("   教学目标数量: ${content.objectives.size}")
    }
    
    @Test
    @DisplayName("MT-004: 智能问答和解释系统测试")
    fun testIntelligentQASystem() = runBlocking {
        println("\n🤖 测试智能问答和解释系统...")
        
        val studentId = StudentId.generate()
        val studentQuestion = StudentQuestion(
            text = "什么是相对论？它有什么实际应用？",
            timestamp = Clock.System.now(),
            context = LearningContext(
                currentCourse = "物理学",
                currentTopic = "现代物理",
                difficultyLevel = DifficultyLevel.ADVANCED,
                previousTopics = listOf("经典力学", "电磁学")
            ),
            questionType = QuestionType.CONCEPTUAL
        )
        
        val learningContext = LearningContext(
            currentCourse = "物理学",
            currentTopic = "相对论",
            difficultyLevel = DifficultyLevel.ADVANCED
        )
        
        // 执行智能问答
        val qaResult = teachingAssistant.processIntelligentQA(
            studentId = studentId,
            question = studentQuestion,
            context = learningContext
        )
        
        assertTrue(qaResult is IntelligentQAResult.Success, "智能问答应该成功")
        val response = (qaResult as IntelligentQAResult.Success).response
        
        assertNotNull(response, "问答响应不应为空")
        assertEquals(studentId, response.studentId, "响应应该属于正确的学生")
        assertEquals(studentQuestion, response.originalQuestion, "原始问题应该匹配")
        assertNotNull(response.questionAnalysis, "问题分析不应为空")
        assertNotNull(response.answer, "答案不应为空")
        assertNotNull(response.explanation, "解释不应为空")
        assertTrue(response.supplementaryResources.isNotEmpty(), "应该有补充资源")
        assertTrue(response.followUpQuestions.isNotEmpty(), "应该有后续问题")
        assertTrue(response.confidence >= 0.0 && response.confidence <= 1.0, "置信度应该在0-1之间")
        
        // 验证解释的完整性
        assertTrue(response.explanation.stepByStep.isNotEmpty(), "应该有分步解释")
        assertTrue(response.explanation.examples.isNotEmpty(), "应该有例子")
        assertTrue(response.explanation.analogies.isNotEmpty(), "应该有类比")
        
        println("✅ 智能问答和解释系统测试通过")
        println("   问题类型: ${response.questionAnalysis.questionType}")
        println("   答案置信度: ${String.format("%.2f", response.confidence)}")
        println("   补充资源数量: ${response.supplementaryResources.size}")
        println("   后续问题数量: ${response.followUpQuestions.size}")
        println("   解释步骤数量: ${response.explanation.stepByStep.size}")
    }
    
    @Test
    @DisplayName("MT-005: 个性化教学策略推荐测试")
    fun testPersonalizedTeachingStrategies() = runBlocking {
        println("\n📚 测试个性化教学策略推荐...")
        
        val studentId = StudentId.generate()
        val learningProfile = LearningProfile.createDefault(studentId)
        val currentTopic = "微积分基础"
        
        val performanceData = StudentPerformanceData(
            studentId = studentId,
            recentScores = listOf(0.7, 0.8, 0.6, 0.9, 0.75),
            timeSpentOnTopics = mapOf(
                "函数" to 45.minutes,
                "极限" to 60.minutes,
                "导数" to 30.minutes
            ),
            difficultyProgression = listOf(
                DifficultyLevel.BEGINNER,
                DifficultyLevel.INTERMEDIATE,
                DifficultyLevel.INTERMEDIATE,
                DifficultyLevel.ADVANCED
            ),
            engagementMetrics = mapOf(
                "attention_span" to 0.8,
                "participation" to 0.7,
                "completion_rate" to 0.85
            )
        )
        
        // 执行教学策略推荐
        val strategyResult = teachingAssistant.recommendTeachingStrategies(
            studentId = studentId,
            learningProfile = learningProfile,
            currentTopic = currentTopic,
            performanceData = performanceData
        )
        
        assertTrue(strategyResult is TeachingStrategyResult.Success, "教学策略推荐应该成功")
        val recommendation = (strategyResult as TeachingStrategyResult.Success).recommendation
        
        assertNotNull(recommendation, "策略推荐不应为空")
        assertEquals(studentId, recommendation.studentId, "推荐应该属于正确的学生")
        assertEquals(currentTopic, recommendation.topic, "主题应该匹配")
        assertNotNull(recommendation.learningStyleAnalysis, "学习风格分析不应为空")
        assertNotNull(recommendation.knowledgeMastery, "知识掌握评估不应为空")
        assertTrue(recommendation.recommendedStrategies.isNotEmpty(), "应该有推荐策略")
        assertNotNull(recommendation.implementationGuidance, "应该有实施指导")
        assertNotNull(recommendation.expectedOutcomes, "应该有预期结果")
        
        // 验证策略的优先级排序
        val priorities = recommendation.recommendedStrategies.map { it.priority }
        assertEquals(priorities.sorted(), priorities, "策略应该按优先级排序")
        
        // 验证预期结果的合理性
        assertTrue(recommendation.expectedOutcomes.learningImprovement >= 0.0, "学习改进应该为非负值")
        assertTrue(recommendation.expectedOutcomes.engagementIncrease >= 0.0, "参与度提升应该为非负值")
        assertTrue(recommendation.expectedOutcomes.confidenceLevel >= 0.0, "置信度应该为非负值")
        
        println("✅ 个性化教学策略推荐测试通过")
        println("   主要学习风格: ${recommendation.learningStyleAnalysis.primaryStyle}")
        println("   整体掌握度: ${String.format("%.2f", recommendation.knowledgeMastery.overallMastery)}")
        println("   推荐策略数量: ${recommendation.recommendedStrategies.size}")
        println("   识别困难数量: ${recommendation.identifiedDifficulties.size}")
        println("   预期学习改进: ${String.format("%.2f", recommendation.expectedOutcomes.learningImprovement * 100)}%")
    }
    
    @Test
    @DisplayName("MT-006: 多模态教学服务集成测试")
    fun testMultimodalTeachingServiceIntegration() = runBlocking {
        println("\n🔗 测试多模态教学服务集成...")
        
        val teacherId = "teacher_001"
        val studentIds = listOf(StudentId.generate(), StudentId.generate(), StudentId.generate())
        
        val sessionConfig = MultimodalSessionConfig(
            subject = Subject.COMPUTER_SCIENCE,
            topic = "人工智能基础",
            duration = 90.minutes,
            enableVoiceInteraction = true,
            enableVisualProcessing = true,
            enableTextAnalysis = true,
            maxParticipants = 30
        )
        
        // 1. 启动多模态教学会话
        val sessionResult = teachingService.startMultimodalSession(
            teacherId = teacherId,
            studentIds = studentIds,
            sessionConfig = sessionConfig
        )
        
        assertTrue(sessionResult is MultimodalSessionResult.Success, "多模态会话启动应该成功")
        val session = (sessionResult as MultimodalSessionResult.Success).session
        
        assertNotNull(session, "会话不应为空")
        assertEquals(teacherId, session.teacherId, "教师ID应该匹配")
        assertEquals(studentIds, session.studentIds, "学生ID列表应该匹配")
        assertEquals(sessionConfig, session.config, "会话配置应该匹配")
        assertEquals(MultimodalSessionStatus.ACTIVE, session.status, "会话状态应该为活跃")
        
        // 2. 生成个性化多模态内容
        val contentRequest = PersonalizedContentRequest(
            topic = "机器学习算法",
            difficultyLevel = DifficultyLevel.INTERMEDIATE,
            duration = 45.minutes,
            learningObjectives = listOf("理解监督学习", "掌握分类算法", "应用实际案例")
        )
        
        val contentResult = teachingService.generatePersonalizedContent(
            studentId = studentIds.first(),
            contentRequest = contentRequest
        )
        
        assertTrue(contentResult is PersonalizedContentResult.Success, "个性化内容生成应该成功")
        val personalizedContent = (contentResult as PersonalizedContentResult.Success).content
        
        assertNotNull(personalizedContent, "个性化内容不应为空")
        assertEquals(studentIds.first(), personalizedContent.studentId, "学生ID应该匹配")
        assertEquals(contentRequest, personalizedContent.originalRequest, "原始请求应该匹配")
        assertTrue(personalizedContent.estimatedEffectiveness > 0.0, "预估效果应该大于0")
        
        println("✅ 多模态教学服务集成测试通过")
        println("   会话ID: ${session.id}")
        println("   参与学生数量: ${session.studentIds.size}")
        println("   会话主题: ${session.config.topic}")
        println("   个性化内容效果: ${String.format("%.2f", personalizedContent.estimatedEffectiveness)}")
        println("   自适应元素数量: ${personalizedContent.adaptiveElements.size}")
    }
    
    @AfterAll
    fun cleanup() {
        println("\n🧹 清理测试环境...")
        println("✅ Week 21-22多模态智能教学助手测试完成")
    }
}
