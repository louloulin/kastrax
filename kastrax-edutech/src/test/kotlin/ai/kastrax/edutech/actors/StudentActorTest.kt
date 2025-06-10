package ai.kastrax.edutech.actors

import ai.kastrax.memory.api.Memory
import ai.kastrax.rag.RAG
import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.services.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * StudentActor单元测试
 *
 * 验证ed2.md第2.1节Actor模型的教育场景实现
 * 测试简化后的Actor实现
 */
class StudentActorTest {

    private lateinit var memorySystem: Memory
    private lateinit var ragSystem: RAG
    private lateinit var learningAnalytics: LearningAnalytics
    private lateinit var personalizationEngine: PersonalizationEngine
    private lateinit var studentActor: StudentActor

    private val testStudentId = StudentId.generate()
    private val testCourseId = CourseId.generate()

    @BeforeTest
    fun setup() {
        // 创建Mock对象
        memorySystem = mockk(relaxed = true)
        ragSystem = mockk(relaxed = true)
        learningAnalytics = mockk(relaxed = true)
        personalizationEngine = mockk(relaxed = true)

        // 配置Mock行为
        coEvery { memorySystem.saveMessage(any(), any()) } returns ""
        coEvery { ragSystem.search(any(), any()) } returns emptyList()
        coEvery { learningAnalytics.analyzePerformance(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { personalizationEngine.generateLearningPlan(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { personalizationEngine.adaptPlan(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { personalizationEngine.regeneratePlan(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { personalizationEngine.generateRecommendations(any(), any(), any(), any()) } returns emptyList()

        // 创建StudentActor实例
        studentActor = StudentActor(
            studentId = testStudentId,
            memorySystem = memorySystem,
            ragSystem = ragSystem,
            learningAnalytics = learningAnalytics,
            personalizationEngine = personalizationEngine
        )
    }
    
    @Test
    fun `should start learning session successfully`() = runTest {
        // Given
        val objectives = listOf("学习基础数学概念", "提高问题解决能力")
        val startMessage = StartLearningSession(
            courseId = testCourseId,
            objectives = objectives
        )

        // When
        val result = studentActor.receive(startMessage)

        // Then
        assertNotNull(result)
        assertTrue(result is SessionStarted)

        // Verify interactions
        coVerify { memorySystem.saveMessage(any(), eq(testStudentId.toString())) }
        coVerify { personalizationEngine.generateLearningPlan(eq(testStudentId), eq(objectives), any()) }
    }
    
    @Test
    fun `should process learning activity correctly`() = runTest {
        // Given - 首先启动学习会话
        val objectives = listOf("学习基础代数")
        val startMessage = StartLearningSession(testCourseId, objectives)
        val mockPlan = PersonalizedLearningPlan.empty(testStudentId)
        coEvery { personalizationEngine.generateLearningPlan(any(), any(), any()) } returns mockPlan

        studentActor.receive(startMessage) // 启动会话

        // Given - 处理学习活动
        val activity = LearningActivity.create(
            type = ActivityType.READING,
            topic = Topic("基础代数"),
            difficulty = DifficultyLevel.BEGINNER,
            skillsInvolved = setOf(Skill.LOGICAL_REASONING)
        )

        val processMessage = ProcessLearningActivity(
            sessionId = SessionId.generate(),
            activity = activity
        )

        val mockAnalysis = PerformanceAnalysis(
            overallScore = 85.0,
            identifiedStrengths = listOf("理解能力强"),
            identifiedWeaknesses = listOf("需要更多练习"),
            improvementSuggestions = listOf("多做练习题"),
            recommendedNextSteps = listOf("进入下一章节"),
            learningVelocity = 1.2,
            difficultyAdjustment = 0.1
        )

        coEvery { learningAnalytics.analyzePerformance(any(), any(), any()) } returns mockAnalysis
        coEvery { personalizationEngine.adaptPlan(any(), any(), any()) } returns mockPlan

        // When
        val result = studentActor.receive(processMessage)

        // Then
        assertNotNull(result)
        assertTrue(result is ActivityProcessed)

        val activityProcessed = result as ActivityProcessed
        assertEquals(activity.id, activityProcessed.activityId)
        assertTrue(activityProcessed.performance > 0.0)
        assertNotNull(activityProcessed.feedback)

        // Verify interactions
        coVerify { learningAnalytics.analyzePerformance(eq(testStudentId), any(), any()) }
        coVerify { personalizationEngine.adaptPlan(any(), eq(mockAnalysis), any()) }
    }
    
    @Test
    fun `should handle personalization update`() = runTest {
        // Given
        val profileUpdates = mapOf(
            "learningStyle" to "VISUAL",
            "preferredDifficulty" to "INTERMEDIATE"
        )
        val preferences = LearningPreferences(
            preferredDifficulty = DifficultyLevel.INTERMEDIATE,
            preferredContentTypes = setOf(ContentType.VIDEO, ContentType.INTERACTIVE),
            preferredSessionDuration = 45,
            preferredTimeOfDay = TimeOfDay.MORNING,
            feedbackFrequency = FeedbackFrequency.IMMEDIATE,
            challengeLevel = ChallengeLevel.STRETCH
        )

        val updateMessage = UpdatePersonalization(
            profileUpdates = profileUpdates,
            preferences = preferences
        )

        // When
        val result = studentActor.receive(updateMessage)

        // Then
        assertNotNull(result)
        assertTrue(result is PersonalizationUpdated)
    }
    
    @Test
    fun `should generate progress report request`() = runTest {
        // Given
        val timeRange = ProgressTimeRange(
            startTime = Clock.System.now().minus(7.days),
            endTime = Clock.System.now()
        )
        val subjects = setOf(Subject.MATHEMATICS, Subject.PHYSICS)
        
        val progressQuery = GetLearningProgress(
            timeRange = timeRange,
            subjects = subjects,
            includeDetails = true
        )
        
        // When & Then
        assertEquals(timeRange, progressQuery.timeRange)
        assertEquals(subjects, progressQuery.subjects)
        assertTrue(progressQuery.includeDetails)
    }
    
    @Test
    fun `should create recommendation request with context`() = runTest {
        // Given
        val context = RecommendationContext(
            subject = Subject.MATHEMATICS,
            difficulty = DifficultyLevel.INTERMEDIATE,
            contentTypes = setOf(ContentType.VIDEO, ContentType.INTERACTIVE),
            timeAvailable = 30,
            specificTopics = listOf("代数", "几何"),
            learningGoals = listOf("提高数学推理能力", "掌握基础概念")
        )

        val recommendationRequest = GetRecommendations(context = context)

        // When
        val result = studentActor.receive(recommendationRequest)

        // Then
        assertNotNull(result)
        assertTrue(result is RecommendationsGenerated)
    }
    
    @Test
    fun `should handle learning goal updates`() = runTest {
        // Given
        val goals = listOf(
            LearningGoal(
                id = "goal1",
                title = "掌握基础代数",
                description = "学习并掌握基础代数概念和运算",
                subject = Subject.MATHEMATICS,
                targetDate = Clock.System.now().plus(30.days),
                priority = GoalPriority.HIGH,
                measurableOutcomes = listOf("能够解决一元一次方程", "理解代数表达式"),
                currentProgress = 25.0
            )
        )

        val goalUpdate = UpdateLearningGoals(newGoals = goals)

        // When
        val result = studentActor.receive(goalUpdate)

        // Then
        assertNotNull(result)
        assertTrue(result is LearningGoalsUpdated)
    }
    
    @Test
    fun `should record metacognitive reflection`() = runTest {
        // Given
        val reflection = MetacognitiveReflection(
            id = "reflection1",
            timestamp = Clock.System.now(),
            activityId = ActivityId.generate(),
            reflectionType = ReflectionType.AFTER_LEARNING,
            content = "我发现通过画图能够更好地理解几何问题",
            selfAssessment = SelfAssessment(
                understanding = 4,
                confidence = 3,
                effort = 5,
                satisfaction = 4,
                difficulty = 3
            ),
            strategiesUsed = listOf("画图法", "分步骤解决"),
            strategiesEffectiveness = mapOf("画图法" to 5, "分步骤解决" to 4),
            futureStrategies = listOf("多练习类似题目", "寻求老师帮助")
        )

        val metacognitionMessage = RecordMetacognition(reflection = reflection)

        // When
        val result = studentActor.receive(metacognitionMessage)

        // Then
        assertNotNull(result)
        assertTrue(result is MetacognitionRecorded)
    }
    
    @Test
    fun `should validate basic model creation`() = runTest {
        // Given & When & Then - 验证基本模型创建
        val studentId = StudentId.generate()
        assertTrue(studentId.value.startsWith("student_"))

        val courseId = CourseId.generate()
        assertTrue(courseId.value.startsWith("course_"))

        val activityId = ActivityId.generate()
        assertTrue(activityId.value.startsWith("activity_"))
    }
    
    @AfterTest
    fun cleanup() {
        clearAllMocks()
    }
}
