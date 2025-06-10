package ai.kastrax.edutech.learning

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.services.*
import ai.kastrax.memory.api.Memory
import ai.kastrax.rag.RAG
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * 学习服务测试
 * 
 * 验证ed2.md第一阶段Week 3-4基础学习服务
 */
class LearningServiceTest {
    
    private lateinit var memorySystem: Memory
    private lateinit var ragSystem: RAG
    private lateinit var learningAnalytics: LearningAnalytics
    private lateinit var personalizationEngine: PersonalizationEngine
    private lateinit var learningService: LearningService
    
    private val testStudentId = StudentId.generate()
    private val testCourseId = CourseId.generate()
    
    @BeforeTest
    fun setup() {
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
        coEvery { personalizationEngine.generateRecommendations(any(), any(), any(), any()) } returns emptyList()
        
        learningService = LearningService(
            memorySystem = memorySystem,
            ragSystem = ragSystem,
            learningAnalytics = learningAnalytics,
            personalizationEngine = personalizationEngine
        )
    }
    
    @Test
    fun `should create student actor successfully`() = runTest {
        // When
        val studentActor1 = learningService.getOrCreateStudentActor(testStudentId)
        val studentActor2 = learningService.getOrCreateStudentActor(testStudentId)
        
        // Then
        assertNotNull(studentActor1)
        assertSame(studentActor1, studentActor2) // 应该返回同一个实例
    }
    
    @Test
    fun `should start learning session successfully`() = runTest {
        // Given
        val objectives = listOf("学习基础数学", "提高解题能力")
        val context = mapOf("previousKnowledge" to "arithmetic")
        
        // When
        val result = learningService.startLearningSession(
            studentId = testStudentId,
            courseId = testCourseId,
            objectives = objectives,
            context = context
        )
        
        // Then
        assertTrue(result is LearningSessionResult.Success)
        val successResult = result as LearningSessionResult.Success
        assertNotNull(successResult.sessionId)
        assertEquals("学习会话启动成功", successResult.message)
    }
    
    @Test
    fun `should process learning activity successfully`() = runTest {
        // Given - 先启动学习会话
        val objectives = listOf("学习测试")
        val sessionResult = learningService.startLearningSession(
            studentId = testStudentId,
            courseId = testCourseId,
            objectives = objectives
        )
        
        assertTrue(sessionResult is LearningSessionResult.Success)
        val sessionId = (sessionResult as LearningSessionResult.Success).sessionId
        
        val activity = LearningActivity.create(
            type = ActivityType.READING,
            topic = Topic("测试主题"),
            difficulty = DifficultyLevel.BEGINNER,
            skillsInvolved = setOf(Skill.LOGICAL_REASONING)
        )
        
        // When
        val result = learningService.processLearningActivity(sessionId, activity)
        
        // Then
        assertTrue(result is ActivityProcessingResult.Success)
        val successResult = result as ActivityProcessingResult.Success
        assertEquals(activity.id, successResult.activityId)
        assertTrue(successResult.performance >= 0.0)
    }
    
    @Test
    fun `should get learning progress successfully`() = runTest {
        // When
        val result = learningService.getLearningProgress(testStudentId)
        
        // Then
        assertTrue(result is LearningProgressResult.Success)
        val successResult = result as LearningProgressResult.Success
        assertEquals(testStudentId, successResult.studentId)
        assertTrue(successResult.overallProgress >= 0.0)
    }
    
    @Test
    fun `should pause learning session successfully`() = runTest {
        // Given - 先启动学习会话
        val sessionResult = learningService.startLearningSession(
            studentId = testStudentId,
            courseId = testCourseId,
            objectives = listOf("测试")
        )
        
        assertTrue(sessionResult is LearningSessionResult.Success)
        val sessionId = (sessionResult as LearningSessionResult.Success).sessionId
        
        // When
        val result = learningService.pauseLearningSession(sessionId)
        
        // Then
        assertTrue(result is SessionOperationResult.Success)
        assertEquals("学习会话已暂停", (result as SessionOperationResult.Success).message)
    }
    
    @Test
    fun `should resume learning session successfully`() = runTest {
        // Given - 先启动并暂停学习会话
        val sessionResult = learningService.startLearningSession(
            studentId = testStudentId,
            courseId = testCourseId,
            objectives = listOf("测试")
        )
        
        assertTrue(sessionResult is LearningSessionResult.Success)
        val sessionId = (sessionResult as LearningSessionResult.Success).sessionId
        
        learningService.pauseLearningSession(sessionId)
        
        // When
        val result = learningService.resumeLearningSession(sessionId)
        
        // Then
        assertTrue(result is SessionOperationResult.Success)
        assertEquals("学习会话已恢复", (result as SessionOperationResult.Success).message)
    }
    
    @Test
    fun `should complete learning session successfully`() = runTest {
        // Given - 先启动学习会话
        val sessionResult = learningService.startLearningSession(
            studentId = testStudentId,
            courseId = testCourseId,
            objectives = listOf("测试")
        )
        
        assertTrue(sessionResult is LearningSessionResult.Success)
        val sessionId = (sessionResult as LearningSessionResult.Success).sessionId
        
        // When
        val result = learningService.completeLearningSession(sessionId)
        
        // Then
        assertTrue(result is SessionOperationResult.Success)
        assertEquals("学习会话已完成", (result as SessionOperationResult.Success).message)
    }
    
    @Test
    fun `should get active sessions for student`() = runTest {
        // Given - 启动多个学习会话
        val session1 = learningService.startLearningSession(
            studentId = testStudentId,
            courseId = testCourseId,
            objectives = listOf("测试1")
        )
        
        val anotherStudentId = StudentId.generate()
        val session2 = learningService.startLearningSession(
            studentId = anotherStudentId,
            courseId = testCourseId,
            objectives = listOf("测试2")
        )
        
        // When
        val studentSessions = learningService.getActiveSessions(testStudentId)
        val allSessions = learningService.getActiveSessions()
        
        // Then
        assertEquals(1, studentSessions.size)
        assertEquals(2, allSessions.size)
        assertEquals(testStudentId, studentSessions.first().studentId)
    }
    
    @Test
    fun `should get session statistics`() = runTest {
        // Given - 启动一些学习会话
        learningService.startLearningSession(
            studentId = testStudentId,
            courseId = testCourseId,
            objectives = listOf("测试1")
        )
        
        val anotherStudentId = StudentId.generate()
        learningService.startLearningSession(
            studentId = anotherStudentId,
            courseId = testCourseId,
            objectives = listOf("测试2")
        )
        
        // When
        val statistics = learningService.getSessionStatistics()
        
        // Then
        assertEquals(2, statistics.totalActiveSessions)
        assertEquals(2, statistics.uniqueActiveStudents)
        assertTrue(statistics.averageSessionDuration >= 0.0)
        assertNotNull(statistics.timestamp)
    }
    
    @Test
    fun `should handle non-existent session gracefully`() = runTest {
        // Given
        val nonExistentSessionId = SessionId.generate()
        
        // When
        val pauseResult = learningService.pauseLearningSession(nonExistentSessionId)
        val resumeResult = learningService.resumeLearningSession(nonExistentSessionId)
        val completeResult = learningService.completeLearningSession(nonExistentSessionId)
        
        // Then
        assertTrue(pauseResult is SessionOperationResult.Failure)
        assertTrue(resumeResult is SessionOperationResult.Failure)
        assertTrue(completeResult is SessionOperationResult.Failure)
    }
    
    @Test
    fun `should handle activity processing for non-existent session`() = runTest {
        // Given
        val nonExistentSessionId = SessionId.generate()
        val activity = LearningActivity.create(
            type = ActivityType.READING,
            topic = Topic("测试"),
            difficulty = DifficultyLevel.BEGINNER,
            skillsInvolved = setOf(Skill.LOGICAL_REASONING)
        )
        
        // When
        val result = learningService.processLearningActivity(nonExistentSessionId, activity)
        
        // Then
        assertTrue(result is ActivityProcessingResult.Failure)
        assertEquals("会话不存在或已结束", (result as ActivityProcessingResult.Failure).error)
    }
}
