package ai.kastrax.edutech.actors

import ai.kastrax.memory.api.Memory
import ai.kastrax.rag.RAG
import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.services.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.test.*

/**
 * TeacherActor单元测试
 * 
 * 验证ed2.md第2.1节教师Actor的功能实现
 * 测试班级管理、内容生成和进度分析功能
 */
class TeacherActorTest {
    
    private lateinit var memorySystem: Memory
    private lateinit var ragSystem: RAG
    private lateinit var contentGenerationService: ContentGenerationService
    private lateinit var learningAnalytics: LearningAnalytics
    private lateinit var classManagementService: ClassManagementService
    private lateinit var teacherActor: TeacherActor
    
    private val testTeacherId = TeacherId.generate()
    private val testClassroomId = ClassroomId.generate()
    
    @BeforeTest
    fun setup() {
        // 创建Mock对象
        memorySystem = mockk(relaxed = true)
        ragSystem = mockk(relaxed = true)
        contentGenerationService = mockk(relaxed = true)
        learningAnalytics = mockk(relaxed = true)
        classManagementService = mockk(relaxed = true)
        
        // 配置Mock行为
        coEvery { memorySystem.saveMessage(any(), any()) } returns Unit
        coEvery { ragSystem.search(any(), any()) } returns emptyList()
        
        // 创建TeacherActor实例
        teacherActor = TeacherActor(
            teacherId = testTeacherId,
            classroomId = testClassroomId,
            memorySystem = memorySystem,
            ragSystem = ragSystem,
            contentGenerationService = contentGenerationService,
            learningAnalytics = learningAnalytics,
            classManagementService = classManagementService
        )
    }
    
    @Test
    fun `should handle class management actions correctly`() = runTest {
        // Given
        val studentId = StudentId.generate()
        val message = ManageClass(
            classId = testClassroomId,
            action = ClassAction.ADD_STUDENT,
            parameters = mapOf("studentId" to studentId.value)
        )

        // When
        val result = teacherActor.receive(message)

        // Then
        assertNotNull(result)
        assertTrue(result is ClassActionCompleted)

        val actionCompleted = result as ClassActionCompleted
        assertEquals(ClassAction.ADD_STUDENT, actionCompleted.action)
        assertEquals("Success", actionCompleted.result)
    }
    
    @Test
    fun `should generate content successfully`() = runTest {
        // Given
        val message = GenerateContent(
            contentType = ContentType.TEXT,
            subject = Subject.MATHEMATICS,
            difficulty = DifficultyLevel.INTERMEDIATE,
            learningObjectives = listOf("理解二次方程", "掌握求解方法"),
            targetAudience = "初中二年级学生",
            constraints = ContentConstraints(
                maxDuration = 45
            )
        )
        
        val mockSearchResults = listOf(
            createMockSearchResult("二次方程教学资源"),
            createMockSearchResult("数学教学方法")
        )
        coEvery { ragSystem.search(any(), any()) } returns mockSearchResults
        
        // When
        val result = teacherActor.receive(message)
        
        // Then
        assertNotNull(result)
        assertTrue(result is ContentGenerated)

        val contentGenerated = result as ContentGenerated
        assertNotNull(contentGenerated.content)
        assertEquals(ContentType.TEXT, contentGenerated.content.type)
        assertEquals(Subject.MATHEMATICS, contentGenerated.content.subject)
        assertEquals(DifficultyLevel.INTERMEDIATE, contentGenerated.content.difficulty)
        
        // Verify RAG system interaction
        coVerify { ragSystem.search(any(), eq(10)) }
    }
    
    @Test
    fun `should analyze class progress correctly`() = runTest {
        // Given
        val timeRange = ProgressTimeRange(
            startTime = Clock.System.now().minus(7.days),
            endTime = Clock.System.now()
        )
        val message = AnalyzeClassProgress(
            classId = testClassroomId.value,
            analysisType = AnalysisType.OVERALL_PERFORMANCE,
            timeRange = timeRange
        )
        
        // When
        val result = teacherActor.receive(message)
        
        // Then
        assertNotNull(result)
        assertTrue(result is ProgressAnalysisCompleted)
        
        val analysisCompleted = result as ProgressAnalysisCompleted
        assertEquals(AnalysisType.OVERALL_PERFORMANCE, analysisCompleted.analysisType)
        assertNotNull(analysisCompleted.results)
        assertNotNull(analysisCompleted.improvements)
        assertNotNull(analysisCompleted.timestamp)
        
        // Verify the analysis result structure
        assertTrue(analysisCompleted.results is AnalysisResult.OverallPerformance)
        val overallPerformance = analysisCompleted.results as AnalysisResult.OverallPerformance
        assertEquals(75.0, overallPerformance.averagePerformance)
        assertEquals(80.0, overallPerformance.completionRate)
        assertEquals(100, overallPerformance.totalActivities)
        assertEquals(25, overallPerformance.studentCount)
    }
    
    @Test
    fun `should handle unknown message type`() = runTest {
        // Given
        val unknownMessage = object : Message {}
        
        // When
        val result = teacherActor.receive(unknownMessage)
        
        // Then
        assertEquals(null, result)
    }
    
    @Test
    fun `should validate content generation request structure`() = runTest {
        // Given
        val constraints = ContentConstraints(
            maxDuration = 60,
            requiredResources = listOf("投影仪", "实验器材"),
            accessibility = AccessibilityLevel.HIGH,
            language = "zh-CN"
        )
        
        val message = GenerateContent(
            contentType = ContentType.INTERACTIVE,
            subject = Subject.PHYSICS,
            difficulty = DifficultyLevel.ADVANCED,
            learningObjectives = listOf("理解牛顿定律", "应用物理公式"),
            targetAudience = "高中物理学生",
            constraints = constraints
        )
        
        // When & Then - 验证消息结构
        assertEquals(ContentType.INTERACTIVE, message.contentType)
        assertEquals(Subject.PHYSICS, message.subject)
        assertEquals(DifficultyLevel.ADVANCED, message.difficulty)
        assertEquals(2, message.learningObjectives.size)
        assertEquals("高中物理学生", message.targetAudience)
        assertEquals(60, message.constraints.maxDuration)
        assertEquals(AccessibilityLevel.HIGH, message.constraints.accessibility)
    }
    
    @Test
    fun `should validate class action parameters`() = runTest {
        // Given
        val studentId = StudentId.generate()
        val addStudentMessage = ManageClass(
            action = ClassAction.ADD_STUDENT,
            parameters = mapOf("studentId" to studentId.value)
        )
        
        val removeStudentMessage = ManageClass(
            action = ClassAction.REMOVE_STUDENT,
            parameters = mapOf("studentId" to studentId.value)
        )
        
        val broadcastMessage = ManageClass(
            action = ClassAction.BROADCAST_MESSAGE,
            parameters = mapOf("message" to "今天的作业已发布")
        )
        
        // When & Then
        assertEquals(ClassAction.ADD_STUDENT, addStudentMessage.action)
        assertEquals(studentId.value, addStudentMessage.parameters["studentId"])
        
        assertEquals(ClassAction.REMOVE_STUDENT, removeStudentMessage.action)
        assertEquals(ClassAction.BROADCAST_MESSAGE, broadcastMessage.action)
        assertEquals("今天的作业已发布", broadcastMessage.parameters["message"])
    }
    
    @Test
    fun `should validate analysis types and time ranges`() = runTest {
        // Given
        val currentTime = Clock.System.now()
        val timeRange = ProgressTimeRange(
            startTime = currentTime.minus(30, kotlinx.datetime.DateTimeUnit.DAY),
            endTime = currentTime
        )
        
        val overallAnalysis = AnalyzeClassProgress(
            analysisType = AnalysisType.OVERALL_PERFORMANCE,
            timeRange = timeRange
        )
        
        val individualAnalysis = AnalyzeClassProgress(
            analysisType = AnalysisType.INDIVIDUAL_PROGRESS,
            timeRange = timeRange
        )
        
        val subjectAnalysis = AnalyzeClassProgress(
            analysisType = AnalysisType.SUBJECT_ANALYSIS,
            timeRange = timeRange
        )
        
        // When & Then
        assertEquals(AnalysisType.OVERALL_PERFORMANCE, overallAnalysis.analysisType)
        assertEquals(AnalysisType.INDIVIDUAL_PROGRESS, individualAnalysis.analysisType)
        assertEquals(AnalysisType.SUBJECT_ANALYSIS, subjectAnalysis.analysisType)
        
        // Verify time range
        assertEquals(timeRange, overallAnalysis.timeRange)
        assertTrue(timeRange.startTime < timeRange.endTime)
    }
    
    @Test
    fun `should validate teacher and classroom identifiers`() = runTest {
        // Given & When & Then
        assertTrue(testTeacherId.value.startsWith("teacher_"))
        assertTrue(testClassroomId.value.startsWith("classroom_"))
        
        // Verify identifier uniqueness
        val anotherTeacherId = TeacherId.generate()
        val anotherClassroomId = ClassroomId.generate()
        
        assertNotEquals(testTeacherId, anotherTeacherId)
        assertNotEquals(testClassroomId, anotherClassroomId)
    }
    
    // 辅助方法
    private fun createMockSearchResult(title: String) = ai.kastrax.store.document.DocumentSearchResult(
        document = ai.kastrax.store.document.Document(
            id = "doc_${title.hashCode()}",
            content = "Mock content for $title",
            metadata = mapOf("title" to title)
        ),
        score = 0.8,
        explanation = "Mock search result"
    )
    
    @AfterTest
    fun cleanup() {
        clearAllMocks()
    }
}
