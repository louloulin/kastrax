package ai.kastrax.edutech.acceptance

import ai.kastrax.edutech.auth.*
import ai.kastrax.edutech.content.*
import ai.kastrax.edutech.learning.*
import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.services.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.time.Duration.Companion.minutes

/**
 * 用户验收测试框架
 * 
 * 根据ed2.md Week 15-16计划实施的用户验收测试，包括：
 * - 功能验收测试
 * - 用户体验测试  
 * - 业务流程验证
 * - 培训效果评估
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserAcceptanceTestFramework {
    
    private lateinit var testEnvironment: AcceptanceTestEnvironment
    private val testResults = mutableListOf<AcceptanceTestResult>()
    
    @BeforeAll
    fun setupAcceptanceTests() {
        println("🚀 初始化用户验收测试环境...")
        testEnvironment = AcceptanceTestEnvironment()
        testEnvironment.initialize()
        println("✅ 用户验收测试环境初始化完成")
    }
    
    @AfterAll
    fun generateAcceptanceReport() {
        println("\n📊 生成用户验收测试报告...")
        val report = AcceptanceTestReport(testResults)
        report.generateReport()
        println("✅ 用户验收测试报告已生成")
    }
    
    @Test
    @DisplayName("UAT-001: 学生用户完整学习流程验收测试")
    fun testStudentLearningWorkflow() = runBlocking {
        val testCase = "UAT-001"
        val startTime = Clock.System.now()
        
        try {
            println("\n🎓 执行学生学习流程验收测试...")
            
            // 1. 学生登录
            val studentAuth = testEnvironment.authenticateStudent("test_student_001")
            assertTrue(studentAuth.isSuccess, "学生登录应该成功")
            
            // 2. 浏览课程目录
            val courses = testEnvironment.browseCourses(studentAuth.token)
            assertTrue(courses.isNotEmpty(), "应该能够浏览到课程")
            
            // 3. 选择课程并开始学习
            val selectedCourse = courses.first()
            val learningSession = testEnvironment.startLearningSession(
                studentAuth.token, 
                selectedCourse.id
            )
            assertTrue(learningSession is AcceptanceLearningSessionResult.Success, "应该能够成功开始学习会话")
            
            // 4. 完成学习活动
            val activities = testEnvironment.getLearningActivities(selectedCourse.id)
            activities.take(3).forEach { activity ->
                val result = testEnvironment.completeLearningActivity(
                    studentAuth.token,
                    (learningSession as AcceptanceLearningSessionResult.Success).sessionId,
                    activity
                )
                assertTrue(result.isSuccess, "学习活动应该能够成功完成")
            }
            
            // 5. 查看学习进度
            val progress = testEnvironment.getStudentProgress(
                studentAuth.token,
                selectedCourse.id
            )
            assertTrue(progress.completionRate > 0, "学习进度应该有所提升")
            
            // 6. 获取个性化推荐
            val recommendations = testEnvironment.getPersonalizedRecommendations(
                studentAuth.token
            )
            assertTrue(recommendations.isNotEmpty(), "应该能够获取个性化推荐")
            
            recordTestResult(testCase, true, "学生学习流程验收测试通过", startTime)
            
        } catch (e: Exception) {
            recordTestResult(testCase, false, "学生学习流程验收测试失败: ${e.message}", startTime)
            throw e
        }
    }
    
    @Test
    @DisplayName("UAT-002: 教师用户课程管理流程验收测试")
    fun testTeacherCourseManagementWorkflow() = runBlocking {
        val testCase = "UAT-002"
        val startTime = Clock.System.now()
        
        try {
            println("\n👨‍🏫 执行教师课程管理流程验收测试...")
            
            // 1. 教师登录
            val teacherAuth = testEnvironment.authenticateTeacher("test_teacher_001")
            assertTrue(teacherAuth.isSuccess, "教师登录应该成功")
            
            // 2. 创建新课程
            val newCourse = testEnvironment.createCourse(
                teacherAuth.token,
                "验收测试课程",
                "这是一个用于验收测试的课程"
            )
            assertTrue(newCourse.isSuccess, "应该能够成功创建课程")
            
            // 3. 添加学习内容
            val content = testEnvironment.addLearningContent(
                teacherAuth.token,
                newCourse.courseId,
                "测试内容",
                "这是测试用的学习内容"
            )
            assertTrue(content.isSuccess, "应该能够成功添加学习内容")
            
            // 4. 设置评估任务
            val assessment = testEnvironment.createAssessment(
                teacherAuth.token,
                newCourse.courseId,
                "期中测试",
                listOf("问题1", "问题2", "问题3")
            )
            assertTrue(assessment.isSuccess, "应该能够成功创建评估任务")
            
            // 5. 查看学生进度
            val classProgress = testEnvironment.getClassProgress(
                teacherAuth.token,
                newCourse.courseId
            )
            assertNotNull(classProgress, "应该能够查看班级进度")
            
            // 6. 生成学习分析报告
            val analyticsReport = testEnvironment.generateAnalyticsReport(
                teacherAuth.token,
                newCourse.courseId
            )
            assertTrue(analyticsReport.isSuccess, "应该能够生成学习分析报告")
            
            recordTestResult(testCase, true, "教师课程管理流程验收测试通过", startTime)
            
        } catch (e: Exception) {
            recordTestResult(testCase, false, "教师课程管理流程验收测试失败: ${e.message}", startTime)
            throw e
        }
    }
    
    @Test
    @DisplayName("UAT-003: 管理员系统管理流程验收测试")
    fun testAdminSystemManagementWorkflow() = runBlocking {
        val testCase = "UAT-003"
        val startTime = Clock.System.now()
        
        try {
            println("\n👨‍💼 执行管理员系统管理流程验收测试...")
            
            // 1. 管理员登录
            val adminAuth = testEnvironment.authenticateAdmin("test_admin_001")
            assertTrue(adminAuth.isSuccess, "管理员登录应该成功")
            
            // 2. 查看系统状态
            val systemStatus = testEnvironment.getSystemStatus(adminAuth.token)
            assertTrue(systemStatus.isHealthy, "系统状态应该健康")
            
            // 3. 管理用户账户
            val userManagement = testEnvironment.manageUsers(adminAuth.token)
            assertTrue(userManagement.isSuccess, "应该能够管理用户账户")
            
            // 4. 配置系统设置
            val systemConfig = testEnvironment.updateSystemConfiguration(
                adminAuth.token,
                mapOf(
                    "max_concurrent_sessions" to "1000",
                    "session_timeout" to "30",
                    "enable_analytics" to "true"
                )
            )
            assertTrue(systemConfig.isSuccess, "应该能够更新系统配置")
            
            // 5. 查看系统日志
            val systemLogs = testEnvironment.getSystemLogs(adminAuth.token)
            assertTrue(systemLogs.isNotEmpty(), "应该能够查看系统日志")
            
            // 6. 执行系统备份
            val backupResult = testEnvironment.performSystemBackup(adminAuth.token)
            assertTrue(backupResult.isSuccess, "应该能够执行系统备份")
            
            recordTestResult(testCase, true, "管理员系统管理流程验收测试通过", startTime)
            
        } catch (e: Exception) {
            recordTestResult(testCase, false, "管理员系统管理流程验收测试失败: ${e.message}", startTime)
            throw e
        }
    }
    
    @Test
    @DisplayName("UAT-004: 用户体验和界面响应性验收测试")
    fun testUserExperienceAndResponsiveness() = runBlocking {
        val testCase = "UAT-004"
        val startTime = Clock.System.now()
        
        try {
            println("\n🎨 执行用户体验和界面响应性验收测试...")
            
            // 1. 页面加载性能测试
            val pageLoadTimes = testEnvironment.measurePageLoadTimes()
            assertTrue(pageLoadTimes.all { it < 3000 }, "所有页面加载时间应该小于3秒")
            
            // 2. API响应时间测试
            val apiResponseTimes = testEnvironment.measureApiResponseTimes()
            assertTrue(apiResponseTimes.all { it < 1000 }, "所有API响应时间应该小于1秒")
            
            // 3. 并发用户测试
            val concurrentUserTest = testEnvironment.testConcurrentUsers(100)
            assertTrue(concurrentUserTest.successRate > 0.95, "并发用户测试成功率应该大于95%")
            
            // 4. 移动端兼容性测试
            val mobileCompatibility = testEnvironment.testMobileCompatibility()
            assertTrue(mobileCompatibility.isCompatible, "应该支持移动端访问")
            
            // 5. 浏览器兼容性测试
            val browserCompatibility = testEnvironment.testBrowserCompatibility()
            assertTrue(browserCompatibility.supportedBrowsers.size >= 3, "应该支持至少3种主流浏览器")
            
            recordTestResult(testCase, true, "用户体验和界面响应性验收测试通过", startTime)
            
        } catch (e: Exception) {
            recordTestResult(testCase, false, "用户体验和界面响应性验收测试失败: ${e.message}", startTime)
            throw e
        }
    }
    
    @Test
    @DisplayName("UAT-005: 业务流程完整性验收测试")
    fun testBusinessProcessIntegrity() = runBlocking {
        val testCase = "UAT-005"
        val startTime = Clock.System.now()
        
        try {
            println("\n💼 执行业务流程完整性验收测试...")
            
            // 1. 完整的课程生命周期测试
            val courseLifecycle = testEnvironment.testCompleteCourseLifecycle()
            assertTrue(courseLifecycle.isSuccess, "完整课程生命周期应该正常运行")
            
            // 2. 学生学习路径测试
            val learningPath = testEnvironment.testStudentLearningPath()
            assertTrue(learningPath.isSuccess, "学生学习路径应该完整可用")
            
            // 3. 评估和反馈流程测试
            val assessmentFlow = testEnvironment.testAssessmentAndFeedbackFlow()
            assertTrue(assessmentFlow.isSuccess, "评估和反馈流程应该正常工作")
            
            // 4. 数据同步和一致性测试
            val dataSyncTest = testEnvironment.testDataSynchronization()
            assertTrue(dataSyncTest.isConsistent, "数据同步应该保持一致性")
            
            // 5. 权限和安全流程测试
            val securityFlow = testEnvironment.testSecurityAndPermissionFlow()
            assertTrue(securityFlow.isSecure, "权限和安全流程应该正常工作")
            
            recordTestResult(testCase, true, "业务流程完整性验收测试通过", startTime)
            
        } catch (e: Exception) {
            recordTestResult(testCase, false, "业务流程完整性验收测试失败: ${e.message}", startTime)
            throw e
        }
    }
    
    private fun recordTestResult(
        testCase: String,
        passed: Boolean,
        message: String,
        startTime: Instant
    ) {
        val endTime = Clock.System.now()
        val duration = endTime - startTime
        
        testResults.add(
            AcceptanceTestResult(
                testCase = testCase,
                passed = passed,
                message = message,
                duration = duration,
                timestamp = endTime
            )
        )
        
        val status = if (passed) "✅ 通过" else "❌ 失败"
        println("$status $testCase: $message (耗时: ${duration.inWholeSeconds}秒)")
    }
}

/**
 * 验收测试结果数据类
 */
data class AcceptanceTestResult(
    val testCase: String,
    val passed: Boolean,
    val message: String,
    val duration: kotlin.time.Duration,
    val timestamp: Instant
)
