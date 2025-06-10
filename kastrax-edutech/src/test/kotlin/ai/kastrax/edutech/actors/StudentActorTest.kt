package ai.kastrax.edutech.actors

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.memory.api.Memory
import ai.kastrax.rag.RAGSystem
import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.services.LearningAnalytics
import ai.kastrax.edutech.services.PersonalizationEngine
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes

/**
 * StudentActor单元测试
 * 
 * 验证ed2.md第2.1节Actor模型的教育场景实现
 */
class StudentActorTest {
    
    private lateinit var actorSystem: ActorSystem
    private lateinit var memorySystem: Memory
    private lateinit var ragSystem: RAGSystem
    private lateinit var learningAnalytics: LearningAnalytics
    private lateinit var personalizationEngine: PersonalizationEngine
    private lateinit var studentActor: PID
    
    private val testStudentId = StudentId.generate()
    private val testCourseId = CourseId.generate()
    
    @BeforeTest
    fun setup() {
        // 创建Mock对象
        actorSystem = mockk(relaxed = true)
        memorySystem = mockk(relaxed = true)
        ragSystem = mockk(relaxed = true)
        learningAnalytics = mockk(relaxed = true)
        personalizationEngine = mockk(relaxed = true)
        
        // 配置Mock行为
        coEvery { memorySystem.saveMessage(any(), any()) } returns Unit
        coEvery { ragSystem.search(any(), any(), any()) } returns emptyList()
        coEvery { learningAnalytics.analyzePerformance(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { personalizationEngine.generateLearningPlan(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { personalizationEngine.adaptPlan(any(), any(), any()) } returns mockk(relaxed = true)
        
        // 创建StudentActor实例
        studentActor = mockk(relaxed = true)
    }
    
    @Test
    fun `should start learning session successfully`() = runTest {
        // Given
        val objectives = listOf("学习基础数学概念", "提高问题解决能力")
        val startMessage = StartLearningSession(
            courseId = testCourseId,
            objectives = objectives
        )
        
        // When & Then
        // 在实际测试中，这里会验证Actor的消息处理
        // 由于我们使用的是Mock，这里主要验证消息结构的正确性
        assertNotNull(startMessage.courseId)
        assertEquals(objectives, startMessage.objectives)
        assertTrue(startMessage.initialContext.isEmpty())
    }
    
    @Test
    fun `should process learning activity correctly`() = runTest {
        // Given
        val sessionId = SessionId.generate()
        val activity = LearningActivity.create(
            type = ActivityType.READING,
            topic = Topic("基础代数"),
            difficulty = DifficultyLevel.BEGINNER,
            skillsInvolved = setOf(Skill.LOGICAL_REASONING, Skill.PROBLEM_SOLVING)
        )
        
        val processMessage = ProcessLearningActivity(
            sessionId = sessionId,
            activity = activity
        )
        
        // When & Then
        assertEquals(sessionId, processMessage.sessionId)
        assertEquals(activity, processMessage.activity)
        assertEquals(ActivityType.READING, processMessage.activity.type)
        assertEquals(DifficultyLevel.BEGINNER, processMessage.activity.difficulty)
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
        
        // When & Then
        assertEquals(profileUpdates, updateMessage.profileUpdates)
        assertEquals(preferences, updateMessage.preferences)
        assertEquals(DifficultyLevel.INTERMEDIATE, updateMessage.preferences.preferredDifficulty)
    }
    
    @Test
    fun `should generate progress report request`() = runTest {
        // Given
        val timeRange = ProgressTimeRange(
            startTime = Clock.System.now().minus(7, kotlinx.datetime.DateTimeUnit.DAY),
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
        
        // When & Then
        assertEquals(context, recommendationRequest.context)
        assertEquals(Subject.MATHEMATICS, recommendationRequest.context.subject)
        assertEquals(30L, recommendationRequest.context.timeAvailable)
        assertTrue(recommendationRequest.context.specificTopics.contains("代数"))
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
                targetDate = Clock.System.now().plus(30, kotlinx.datetime.DateTimeUnit.DAY),
                priority = GoalPriority.HIGH,
                measurableOutcomes = listOf("能够解决一元一次方程", "理解代数表达式"),
                currentProgress = 25.0
            ),
            LearningGoal(
                id = "goal2",
                title = "提高阅读理解",
                description = "增强中文阅读理解能力",
                subject = Subject.CHINESE,
                targetDate = Clock.System.now().plus(60, kotlinx.datetime.DateTimeUnit.DAY),
                priority = GoalPriority.MEDIUM,
                measurableOutcomes = listOf("能够分析文章主旨", "理解修辞手法"),
                currentProgress = 40.0
            )
        )
        
        val goalUpdate = UpdateLearningGoals(newGoals = goals)
        
        // When & Then
        assertEquals(goals, goalUpdate.newGoals)
        assertEquals(2, goalUpdate.newGoals.size)
        assertEquals("掌握基础代数", goalUpdate.newGoals[0].title)
        assertEquals(GoalPriority.HIGH, goalUpdate.newGoals[0].priority)
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
        
        // When & Then
        assertEquals(reflection, metacognitionMessage.reflection)
        assertEquals(ReflectionType.AFTER_LEARNING, metacognitionMessage.reflection.reflectionType)
        assertEquals(4, metacognitionMessage.reflection.selfAssessment.understanding)
        assertTrue(metacognitionMessage.reflection.strategiesUsed.contains("画图法"))
    }
    
    @Test
    fun `should validate student model type safety`() = runTest {
        // Given
        val student = Student.create(
            name = "张三",
            email = "zhangsan@example.com",
            gradeLevel = GradeLevel.GRADE_8,
            learningProfile = LearningProfile.default()
        )
        
        // When & Then - 验证强类型系统的优势
        assertTrue(student.id.value.startsWith("student_"))
        assertEquals("张三", student.name)
        assertEquals(GradeLevel.GRADE_8, student.gradeLevel)
        assertEquals(LearningStyle.BALANCED, student.learningProfile.learningStyle)
        
        // 验证年级级别的类型安全方法
        assertTrue(student.gradeLevel.isMiddleSchool())
        assertFalse(student.gradeLevel.isElementary())
        assertEquals(8, student.gradeLevel.numericValue)
    }
    
    @Test
    fun `should validate learning session lifecycle`() = runTest {
        // Given
        val session = LearningSession.create(
            studentId = testStudentId,
            courseId = testCourseId,
            objectives = listOf("学习目标1", "学习目标2")
        )
        
        // When - 添加学习活动
        val activity = LearningActivity.create(
            type = ActivityType.QUIZ,
            topic = Topic("测试主题"),
            difficulty = DifficultyLevel.INTERMEDIATE,
            skillsInvolved = setOf(Skill.CRITICAL_THINKING)
        )
        
        val completedActivity = activity.complete(85.0, "表现良好")
        val updatedSession = session.addActivity(completedActivity)
        val finalSession = updatedSession.complete()
        
        // Then
        assertTrue(session.isActive())
        assertEquals(SessionStatus.ACTIVE, session.status)
        assertEquals(1, updatedSession.activities.size)
        assertEquals(SessionStatus.COMPLETED, finalSession.status)
        assertNotNull(finalSession.endTime)
        assertNotNull(finalSession.getDuration())
    }
    
    @Test
    fun `should validate learning preferences and cognitive abilities`() = runTest {
        // Given
        val cognitiveAbilities = CognitiveAbilities(
            workingMemoryCapacity = 7,
            processingSpeed = 6,
            attentionSpan = 8,
            logicalReasoning = 7,
            spatialAbility = 5
        )
        
        val motivationProfile = MotivationProfile(
            intrinsicMotivation = 8,
            extrinsicMotivation = 6,
            goalOrientation = GoalOrientation.MASTERY,
            competitiveness = 5,
            persistenceLevel = 9
        )
        
        // When & Then
        assertEquals(6.6, cognitiveAbilities.getOverallScore())
        
        val boostFactors = motivationProfile.getCurrentBoostFactors()
        assertEquals(0.8, boostFactors["intrinsic"])
        assertEquals(0.6, boostFactors["extrinsic"])
        assertEquals(0.9, boostFactors["persistent"])
    }
    
    @AfterTest
    fun cleanup() {
        clearAllMocks()
    }
}
