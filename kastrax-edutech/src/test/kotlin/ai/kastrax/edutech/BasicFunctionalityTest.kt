package ai.kastrax.edutech

import ai.kastrax.edutech.auth.*
import ai.kastrax.edutech.content.*
import ai.kastrax.edutech.learning.*
import ai.kastrax.edutech.models.*
import ai.kastrax.memory.api.Memory
import ai.kastrax.rag.RAG
import ai.kastrax.edutech.services.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * 基础功能测试
 * 
 * 验证ed2.md第一阶段Week 3-4的基础服务功能
 */
class BasicFunctionalityTest {
    
    @Test
    fun `should generate and validate JWT token`() = runTest {
        // Given
        val authService = AuthService()
        val userId = "user123"
        val roles = listOf(Role.STUDENT)
        
        // When
        val token = authService.generateToken(userId, roles)
        val claims = authService.validateToken(token.accessToken)
        
        // Then
        assertNotNull(token.accessToken)
        assertNotNull(token.refreshToken)
        assertEquals("Bearer", token.tokenType)
        assertTrue(token.expiresIn > 0)
        
        assertNotNull(claims)
        assertEquals(userId, claims.subject)
    }
    
    @Test
    fun `should check user permissions correctly`() = runTest {
        // Given
        val authService = AuthService()
        val studentRoles = listOf(Role.STUDENT)
        val teacherRoles = listOf(Role.TEACHER)
        
        // When & Then
        assertTrue(authService.hasPermission(studentRoles, Permission.VIEW_COURSE))
        assertFalse(authService.hasPermission(studentRoles, Permission.CREATE_COURSE))
        
        assertTrue(authService.hasPermission(teacherRoles, Permission.VIEW_COURSE))
        assertTrue(authService.hasPermission(teacherRoles, Permission.CREATE_COURSE))
        assertFalse(authService.hasPermission(teacherRoles, Permission.MANAGE_USERS))
    }
    
    @Test
    fun `should authenticate request successfully`() = runTest {
        // Given
        val authService = AuthService()
        val securityMiddleware = SecurityMiddleware(authService)
        
        val userId = "user123"
        val roles = listOf(Role.TEACHER)
        val token = authService.generateToken(userId, roles)
        
        val request = AuthRequest(
            path = "/api/courses",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer ${token.accessToken}")
        )
        
        // When
        val result = securityMiddleware.authenticateRequest(request)
        
        // Then
        assertTrue(result is AuthResult.Success)
        val successResult = result as AuthResult.Success
        assertEquals(userId, successResult.user.userId)
        assertEquals(roles, successResult.user.roles)
    }
    
    @Test
    fun `should create and manage learning content`() = runTest {
        // Given
        val contentRepository = InMemoryContentRepository()
        val ragSystem = mockk<RAG>(relaxed = true)
        val contentService = ContentManagementService(contentRepository, ragSystem)
        
        val content = LearningContent(
            title = "数学基础",
            description = "学习数学基础知识",
            content = "这是数学基础课程的内容",
            type = ContentType.TEXT,
            subject = Subject.MATHEMATICS,
            difficulty = DifficultyLevel.BEGINNER,
            estimatedDuration = 30,
            learningObjectives = listOf("理解基础概念", "掌握基本运算"),
            createdBy = "teacher123"
        )
        
        // When
        val createResult = contentService.createContent(content, "teacher123")
        
        // Then
        assertTrue(createResult is ContentResult.Success)
        val createdContent = (createResult as ContentResult.Success).content
        assertNotNull(createdContent.id)
        assertEquals("teacher123", createdContent.createdBy)
        assertEquals(1, createdContent.version)
        
        // Test retrieval
        val getResult = contentService.getContent(createdContent.id)
        assertTrue(getResult is ContentResult.Success)
        assertEquals(createdContent.title, (getResult as ContentResult.Success).content.title)
    }
    
    @Test
    fun `should search content with filters`() = runTest {
        // Given
        val contentRepository = InMemoryContentRepository()
        val ragSystem = mockk<RAG>(relaxed = true)
        val contentService = ContentManagementService(contentRepository, ragSystem)
        
        // Create test content
        val mathContent = LearningContent(
            title = "数学基础",
            description = "数学基础课程",
            content = "数学内容",
            type = ContentType.TEXT,
            subject = Subject.MATHEMATICS,
            difficulty = DifficultyLevel.BEGINNER,
            estimatedDuration = 30,
            learningObjectives = listOf("学习数学"),
            createdBy = "teacher1"
        )
        
        val physicsContent = LearningContent(
            title = "物理入门",
            description = "物理入门课程",
            content = "物理内容",
            type = ContentType.VIDEO,
            subject = Subject.PHYSICS,
            difficulty = DifficultyLevel.INTERMEDIATE,
            estimatedDuration = 45,
            learningObjectives = listOf("学习物理"),
            createdBy = "teacher2"
        )
        
        contentService.createContent(mathContent, "teacher1")
        contentService.createContent(physicsContent, "teacher2")
        
        // When
        val mathFilter = ContentFilters(subjects = setOf(Subject.MATHEMATICS))
        val searchResult = contentService.searchContent("", mathFilter)
        
        // Then
        assertTrue(searchResult is ContentSearchResult.Success)
        val results = (searchResult as ContentSearchResult.Success).results
        assertEquals(1, results.size)
        assertEquals("数学基础", results.first().content.title)
    }
    
    @Test
    fun `should manage learning sessions`() = runTest {
        // Given
        val memorySystem = mockk<Memory>(relaxed = true)
        val ragSystem = mockk<RAG>(relaxed = true)
        val learningAnalytics = mockk<LearningAnalytics>(relaxed = true)
        val personalizationEngine = mockk<PersonalizationEngine>(relaxed = true)
        
        coEvery { memorySystem.saveMessage(any(), any()) } returns ""
        coEvery { ragSystem.search(any(), any()) } returns emptyList()
        coEvery { learningAnalytics.analyzePerformance(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { personalizationEngine.generateLearningPlan(any(), any(), any()) } returns mockk(relaxed = true)
        
        val learningService = LearningService(
            memorySystem = memorySystem,
            ragSystem = ragSystem,
            learningAnalytics = learningAnalytics,
            personalizationEngine = personalizationEngine
        )
        
        val studentId = StudentId.generate()
        val courseId = CourseId.generate()
        val objectives = listOf("学习基础知识", "提高技能")
        
        // When
        val sessionResult = learningService.startLearningSession(
            studentId = studentId,
            courseId = courseId,
            objectives = objectives
        )
        
        // Then
        assertTrue(sessionResult is LearningSessionResult.Success)
        val successResult = sessionResult as LearningSessionResult.Success
        assertNotNull(successResult.sessionId)
        assertEquals("学习会话启动成功", successResult.message)
        
        // Test session statistics
        val statistics = learningService.getSessionStatistics()
        assertEquals(1, statistics.totalActiveSessions)
        assertEquals(1, statistics.uniqueActiveStudents)
    }
    
    @Test
    fun `should validate content properties`() = runTest {
        // Given
        val content = LearningContent(
            title = "测试内容",
            description = "这是一个测试内容的描述",
            content = "这是测试内容的正文部分，包含了很多有用的信息。",
            type = ContentType.TEXT,
            subject = Subject.COMPUTER_SCIENCE,
            difficulty = DifficultyLevel.INTERMEDIATE,
            estimatedDuration = 60,
            learningObjectives = listOf("目标1", "目标2"),
            tags = listOf("编程", "算法", "数据结构"),
            createdBy = "teacher123"
        )
        
        // When & Then
        assertTrue(content.getSize() > 0)
        assertTrue(content.containsKeywords(listOf("测试", "内容")))
        assertFalse(content.containsKeywords(listOf("不存在的关键词")))
        
        assertTrue(content.isSuitableForLearningStyle(LearningStyle.READING_WRITING))
        assertFalse(content.isSuitableForLearningStyle(LearningStyle.KINESTHETIC))
        
        assertTrue(content.isSuitableForDifficulty(DifficultyLevel.INTERMEDIATE))
        assertTrue(content.isSuitableForDifficulty(DifficultyLevel.BEGINNER, allowAdjacent = true))
        assertFalse(content.isSuitableForDifficulty(DifficultyLevel.BEGINNER, allowAdjacent = false))
        
        val summary = content.createSummary(50)
        assertTrue(summary.length <= 53) // 50 + "..."
    }
    
    @Test
    fun `should handle security audit logging`() = runTest {
        // Given
        val auditService = SecurityAuditService()
        val userId = "user123"
        
        // When
        auditService.logSecurityEvent(
            userId = userId,
            action = SecurityAction.LOGIN,
            result = SecurityResult.SUCCESS,
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )
        
        auditService.logSecurityEvent(
            userId = userId,
            action = SecurityAction.LOGIN,
            result = SecurityResult.FAILURE,
            ipAddress = "192.168.1.1"
        )
        
        // Then
        val userLogs = auditService.getUserSecurityLogs(userId)
        assertEquals(2, userLogs.size)
        
        val failedAttempts = auditService.getFailedLoginAttempts(userId, 60)
        assertEquals(1, failedAttempts)
    }
    
    @Test
    fun `should validate model ID generation`() = runTest {
        // Given & When
        val studentId1 = StudentId.generate()
        val studentId2 = StudentId.generate()
        val courseId = CourseId.generate()
        val contentId = ContentId.generate()
        val sessionId = SessionId.generate()
        val activityId = ActivityId.generate()
        
        // Then
        assertTrue(studentId1.value.startsWith("student_"))
        assertTrue(studentId2.value.startsWith("student_"))
        assertTrue(courseId.value.startsWith("course_"))
        assertTrue(contentId.value.startsWith("content_"))
        assertTrue(sessionId.value.startsWith("session_"))
        assertTrue(activityId.value.startsWith("activity_"))
        
        // IDs should be unique
        assertNotEquals(studentId1, studentId2)
    }
}
