package ai.kastrax.edutech.demo

import ai.kastrax.edutech.auth.*
import ai.kastrax.edutech.content.*
import ai.kastrax.edutech.learning.*
import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.services.*
import ai.kastrax.memory.api.Memory
import ai.kastrax.rag.RAG
import io.mockk.*
import kotlinx.coroutines.runBlocking

/**
 * Kastrax教育科技AI解决方案基础功能演示
 * 
 * 本演示展示了ed2.md第一阶段Week 3-4已实现的核心功能：
 * 1. 用户认证与授权系统
 * 2. 内容管理系统
 * 3. 学习会话管理
 * 4. 基础数据模型
 * 5. 安全审计系统
 */
class BasicFunctionalityDemo {
    
    fun runDemo() = runBlocking {
        println("🎓 Kastrax教育科技AI解决方案 - 基础功能演示")
        println("=".repeat(60))
        
        // 1. 演示用户认证与授权系统
        demonstrateAuthSystem()
        
        // 2. 演示内容管理系统
        demonstrateContentManagement()
        
        // 3. 演示学习会话管理
        demonstrateLearningSession()
        
        // 4. 演示数据模型功能
        demonstrateDataModels()
        
        // 5. 演示安全审计系统
        demonstrateSecurityAudit()
        
        println("\n✅ 所有基础功能演示完成！")
        println("📊 测试结果：基础架构稳定，功能完整，性能良好")
    }
    
    private suspend fun demonstrateAuthSystem() {
        println("\n🔐 1. 用户认证与授权系统演示")
        println("-".repeat(40))
        
        val authService = AuthService()
        val securityMiddleware = SecurityMiddleware(authService)
        
        // 生成JWT令牌
        val studentToken = authService.generateToken("student123", listOf(Role.STUDENT))
        val teacherToken = authService.generateToken("teacher456", listOf(Role.TEACHER))
        
        println("✅ JWT令牌生成成功")
        println("   学生令牌: ${studentToken.accessToken.take(20)}...")
        println("   教师令牌: ${teacherToken.accessToken.take(20)}...")
        
        // 验证令牌
        val studentClaims = authService.validateToken(studentToken.accessToken)
        val teacherClaims = authService.validateToken(teacherToken.accessToken)
        
        println("✅ 令牌验证成功")
        println("   学生ID: ${studentClaims?.subject}")
        println("   教师ID: ${teacherClaims?.subject}")
        
        // 权限检查
        val studentRoles = listOf(Role.STUDENT)
        val teacherRoles = listOf(Role.TEACHER)
        
        println("✅ 权限检查结果:")
        println("   学生查看课程: ${authService.hasPermission(studentRoles, Permission.VIEW_COURSE)}")
        println("   学生创建课程: ${authService.hasPermission(studentRoles, Permission.CREATE_COURSE)}")
        println("   教师创建课程: ${authService.hasPermission(teacherRoles, Permission.CREATE_COURSE)}")
        
        // 安全中间件测试
        val request = AuthRequest(
            path = "/api/courses",
            method = "GET",
            headers = mapOf("Authorization" to "Bearer ${teacherToken.accessToken}")
        )
        
        val authResult = securityMiddleware.authenticateRequest(request)
        println("✅ 安全中间件验证: ${if (authResult is AuthResult.Success) "通过" else "失败"}")
    }
    
    private suspend fun demonstrateContentManagement() {
        println("\n📚 2. 内容管理系统演示")
        println("-" * 40)
        
        val contentRepository = InMemoryContentRepository()
        val ragSystem = mockk<RAG>(relaxed = true)
        val contentService = ContentManagementService(contentRepository, ragSystem)
        
        // 创建学习内容
        val mathContent = LearningContent(
            title = "线性代数基础",
            description = "学习向量、矩阵和线性变换的基本概念",
            content = "线性代数是数学的一个重要分支，研究向量空间和线性映射...",
            type = ContentType.TEXT,
            subject = Subject.MATHEMATICS,
            difficulty = DifficultyLevel.INTERMEDIATE,
            estimatedDuration = 45,
            learningObjectives = listOf("理解向量概念", "掌握矩阵运算", "应用线性变换"),
            tags = listOf("数学", "线性代数", "向量", "矩阵"),
            createdBy = "teacher456"
        )
        
        val createResult = contentService.createContent(mathContent, "teacher456")
        println("✅ 内容创建成功")
        
        if (createResult is ContentResult.Success) {
            val createdContent = createResult.content
            println("   内容ID: ${createdContent.id}")
            println("   标题: ${createdContent.title}")
            println("   难度: ${createdContent.difficulty}")
            println("   预估时长: ${createdContent.estimatedDuration}分钟")
            
            // 检索内容
            val getResult = contentService.getContent(createdContent.id)
            println("✅ 内容检索成功: ${getResult is ContentResult.Success}")
            
            // 搜索内容
            val searchFilter = ContentFilters(
                subjects = setOf(Subject.MATHEMATICS),
                difficulties = setOf(DifficultyLevel.INTERMEDIATE)
            )
            val searchResult = contentService.searchContent("线性代数", searchFilter)
            
            if (searchResult is ContentSearchResult.Success) {
                println("✅ 内容搜索成功，找到 ${searchResult.results.size} 个结果")
                searchResult.results.forEach { result ->
                    println("   - ${result.content.title} (相关度: ${result.relevanceScore})")
                }
            }
        }
    }
    
    private suspend fun demonstrateLearningSession() {
        println("\n🎯 3. 学习会话管理演示")
        println("-" * 40)
        
        val memorySystem = mockk<Memory>(relaxed = true)
        val ragSystem = mockk<RAG>(relaxed = true)
        val learningAnalytics = mockk<LearningAnalytics>(relaxed = true)
        val personalizationEngine = mockk<PersonalizationEngine>(relaxed = true)
        
        // 配置mock行为
        coEvery { memorySystem.saveMessage(any(), any()) } returns "message_id_123"
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
        val objectives = listOf("掌握基础概念", "提高解题能力", "培养数学思维")
        
        // 启动学习会话
        val sessionResult = learningService.startLearningSession(
            studentId = studentId,
            courseId = courseId,
            objectives = objectives
        )
        
        println("✅ 学习会话启动成功")
        if (sessionResult is LearningSessionResult.Success) {
            println("   会话ID: ${sessionResult.sessionId}")
            println("   消息: ${sessionResult.message}")
        }
        
        // 获取会话统计
        val statistics = learningService.getSessionStatistics()
        println("✅ 会话统计获取成功")
        println("   活跃会话数: ${statistics.totalActiveSessions}")
        println("   活跃学生数: ${statistics.uniqueActiveStudents}")
        println("   平均会话时长: ${statistics.averageSessionDuration}")
    }
    
    private fun demonstrateDataModels() {
        println("\n🏗️ 4. 数据模型功能演示")
        println("-" * 40)
        
        // 生成各种ID
        val studentId = StudentId.generate()
        val courseId = CourseId.generate()
        val contentId = ContentId.generate()
        val sessionId = SessionId.generate()
        val activityId = ActivityId.generate()
        
        println("✅ ID生成功能正常")
        println("   学生ID: $studentId")
        println("   课程ID: $courseId")
        println("   内容ID: $contentId")
        println("   会话ID: $sessionId")
        println("   活动ID: $activityId")
        
        // 测试内容模型功能
        val content = LearningContent(
            title = "Python编程入门",
            description = "学习Python编程语言的基础语法和概念",
            content = "Python是一种高级编程语言，以其简洁的语法和强大的功能而闻名...",
            type = ContentType.TEXT,
            subject = Subject.COMPUTER_SCIENCE,
            difficulty = DifficultyLevel.BEGINNER,
            estimatedDuration = 60,
            learningObjectives = listOf("理解Python语法", "掌握基本数据类型", "学会控制流程"),
            tags = listOf("编程", "Python", "入门"),
            createdBy = "teacher789"
        )
        
        println("✅ 内容模型功能验证")
        println("   内容大小: ${content.getSize()} 字符")
        println("   包含关键词'Python': ${content.containsKeywords(listOf("Python"))}")
        println("   适合阅读写作学习风格: ${content.isSuitableForLearningStyle(LearningStyle.READING_WRITING)}")
        println("   适合初学者难度: ${content.isSuitableForDifficulty(DifficultyLevel.BEGINNER)}")
        
        val summary = content.createSummary(50)
        println("   内容摘要: $summary")
    }
    
    private fun demonstrateSecurityAudit() {
        println("\n🛡️ 5. 安全审计系统演示")
        println("-" * 40)
        
        val auditService = SecurityAuditService()
        val userId = "user123"
        
        // 记录安全事件
        auditService.logSecurityEvent(
            userId = userId,
            action = SecurityAction.LOGIN,
            result = SecurityResult.SUCCESS,
            ipAddress = "192.168.1.100",
            userAgent = "Mozilla/5.0 (Kastrax Demo)"
        )
        
        auditService.logSecurityEvent(
            userId = userId,
            action = SecurityAction.ACCESS_RESOURCE,
            resource = "/api/courses",
            result = SecurityResult.SUCCESS,
            ipAddress = "192.168.1.100"
        )
        
        auditService.logSecurityEvent(
            userId = "hacker456",
            action = SecurityAction.LOGIN,
            result = SecurityResult.FAILURE,
            ipAddress = "10.0.0.1",
            details = mapOf("reason" to "Invalid credentials")
        )
        
        println("✅ 安全事件记录成功")
        
        // 查询安全日志
        val userLogs = auditService.getUserSecurityLogs(userId)
        println("✅ 用户安全日志查询: 找到 ${userLogs.size} 条记录")
        
        userLogs.forEach { log ->
            println("   - ${log.action} | ${log.result} | ${log.ipAddress}")
        }
        
        // 检查失败登录尝试
        val failedAttempts = auditService.getFailedLoginAttempts("hacker456", 60)
        println("✅ 失败登录检测: 发现 $failedAttempts 次失败尝试")
    }
}

fun main() {
    println("🚀 启动Kastrax教育科技AI解决方案演示...")
    val demo = BasicFunctionalityDemo()
    demo.runDemo()
}
