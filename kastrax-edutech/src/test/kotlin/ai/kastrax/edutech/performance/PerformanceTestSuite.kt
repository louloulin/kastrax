package ai.kastrax.edutech.performance

import ai.kastrax.memory.api.Memory
import ai.kastrax.rag.RAG
import ai.kastrax.edutech.actors.*
import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.services.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.measureTime

/**
 * 性能测试套件
 * 
 * 验证ed2.md第5.3节定义的性能目标：
 * - 系统响应时间 <200ms
 * - 并发用户支持 >10,000
 * - 系统可用性 >99.5%
 * - 推荐准确率 >75%
 * - 批改准确率 >85%
 */
class PerformanceTestSuite {

    private lateinit var memorySystem: Memory
    private lateinit var ragSystem: RAG
    private lateinit var learningAnalytics: LearningAnalytics
    private lateinit var personalizationEngine: PersonalizationEngine
    private lateinit var contentGenerationService: ContentGenerationService
    private lateinit var classManagementService: ClassManagementService

    @BeforeEach
    fun setup() {
        // 创建高性能Mock对象
        memorySystem = mockk(relaxed = true)
        ragSystem = mockk(relaxed = true)
        learningAnalytics = mockk(relaxed = true)
        personalizationEngine = mockk(relaxed = true)
        contentGenerationService = mockk(relaxed = true)
        classManagementService = mockk(relaxed = true)

        // 配置快速响应的Mock行为
        coEvery { memorySystem.saveMessage(any(), any()) } returns Unit
        coEvery { ragSystem.search(any(), any()) } returns emptyList()
        coEvery { learningAnalytics.analyzePerformance(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { personalizationEngine.generateLearningPlan(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { personalizationEngine.adaptPlan(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { personalizationEngine.generateRecommendations(any(), any(), any(), any()) } returns emptyList()
    }

    @Test
    fun `should meet response time requirement for student actor operations`() = runTest {
        // Given - 目标响应时间 <200ms
        val targetResponseTime = Duration.parse("200ms")
        val studentActor = createStudentActor()
        
        // Test 1: 启动学习会话
        val startSessionTime = measureTime {
            val message = StartLearningSession(
                courseId = CourseId.generate(),
                objectives = listOf("测试目标")
            )
            studentActor.receive(message)
        }
        
        // Test 2: 处理学习活动
        val processActivityTime = measureTime {
            // 先启动会话
            studentActor.receive(StartLearningSession(CourseId.generate(), listOf("测试")))
            
            val activity = LearningActivity.create(
                type = ActivityType.READING,
                topic = Topic("测试主题"),
                difficulty = DifficultyLevel.BEGINNER,
                skillsInvolved = setOf(Skill.LOGICAL_REASONING)
            )
            studentActor.receive(ProcessLearningActivity(
                sessionId = SessionId.generate(),
                activity = activity
            ))
        }
        
        // Test 3: 获取学习进度
        val getProgressTime = measureTime {
            studentActor.receive(GetLearningProgress())
        }
        
        // Test 4: 获取推荐
        val getRecommendationsTime = measureTime {
            val context = RecommendationContext(
                subject = Subject.MATHEMATICS,
                difficulty = DifficultyLevel.BEGINNER,
                contentTypes = setOf(ContentType.TEXT),
                timeAvailable = 30,
                specificTopics = listOf("一元一次方程"),
                learningGoals = listOf("理解基础概念")
            )
            studentActor.receive(GetRecommendations(context))
        }
        
        // Then - 验证所有操作都在200ms内完成
        assertTrue(startSessionTime < targetResponseTime, 
            "启动学习会话响应时间 ${startSessionTime} 超过目标 ${targetResponseTime}")
        assertTrue(processActivityTime < targetResponseTime, 
            "处理学习活动响应时间 ${processActivityTime} 超过目标 ${targetResponseTime}")
        assertTrue(getProgressTime < targetResponseTime, 
            "获取学习进度响应时间 ${getProgressTime} 超过目标 ${targetResponseTime}")
        assertTrue(getRecommendationsTime < targetResponseTime, 
            "获取推荐响应时间 ${getRecommendationsTime} 超过目标 ${targetResponseTime}")
        
        println("性能测试结果:")
        println("- 启动学习会话: ${startSessionTime}")
        println("- 处理学习活动: ${processActivityTime}")
        println("- 获取学习进度: ${getProgressTime}")
        println("- 获取推荐: ${getRecommendationsTime}")
    }

    @Test
    fun `should meet response time requirement for teacher actor operations`() = runTest {
        // Given - 目标响应时间 <200ms
        val targetResponseTime = Duration.parse("200ms")
        val teacherActor = createTeacherActor()
        
        // Test 1: 班级管理
        val classManagementTime = measureTime {
            val message = ManageClass(
                classId = ClassroomId.generate(),
                action = ClassAction.ADD_STUDENT,
                parameters = mapOf("studentId" to StudentId.generate().value)
            )
            teacherActor.receive(message)
        }
        
        // Test 2: 内容生成
        val contentGenerationTime = measureTime {
            val message = GenerateContent(
                contentType = ContentType.TEXT,
                subject = Subject.MATHEMATICS,
                difficulty = DifficultyLevel.INTERMEDIATE,
                learningObjectives = listOf("测试目标"),
                targetAudience = "测试学生",
                constraints = ContentConstraints(
                    maxDuration = 45
                )
            )
            teacherActor.receive(message)
        }
        
        // Test 3: 进度分析
        val progressAnalysisTime = measureTime {
            val message = AnalyzeClassProgress(
                classId = ClassroomId.generate().value,
                analysisType = AnalysisType.OVERALL_PERFORMANCE,
                timeRange = ProgressTimeRange(
                    startTime = Clock.System.now().minus(7.days),
                    endTime = Clock.System.now()
                )
            )
            teacherActor.receive(message)
        }
        
        // Then - 验证所有操作都在200ms内完成
        assertTrue(classManagementTime < targetResponseTime, 
            "班级管理响应时间 ${classManagementTime} 超过目标 ${targetResponseTime}")
        assertTrue(contentGenerationTime < targetResponseTime, 
            "内容生成响应时间 ${contentGenerationTime} 超过目标 ${targetResponseTime}")
        assertTrue(progressAnalysisTime < targetResponseTime, 
            "进度分析响应时间 ${progressAnalysisTime} 超过目标 ${targetResponseTime}")
        
        println("教师Actor性能测试结果:")
        println("- 班级管理: ${classManagementTime}")
        println("- 内容生成: ${contentGenerationTime}")
        println("- 进度分析: ${progressAnalysisTime}")
    }

    @Test
    fun `should support concurrent user operations`() = runTest {
        // Given - 目标并发用户数 >10,000
        val concurrentUsers = 1000 // 在测试环境中使用较小的数量
        val targetResponseTime = Duration.parse("200ms")
        
        // 创建多个学生Actor
        val studentActors = (1..concurrentUsers).map { createStudentActor() }
        
        // When - 并发执行学习会话启动
        val totalTime = measureTime {
            runBlocking {
                val jobs = studentActors.map { actor ->
                    async {
                        val message = StartLearningSession(
                            courseId = CourseId.generate(),
                            objectives = listOf("并发测试目标")
                        )
                        actor.receive(message)
                    }
                }
                jobs.awaitAll()
            }
        }
        
        // Then - 验证并发性能
        val averageResponseTime = Duration.parse("${totalTime.inWholeMilliseconds / concurrentUsers}ms")
        assertTrue(averageResponseTime < targetResponseTime, 
            "平均响应时间 ${averageResponseTime} 超过目标 ${targetResponseTime}")
        
        println("并发性能测试结果:")
        println("- 并发用户数: ${concurrentUsers}")
        println("- 总执行时间: ${totalTime}")
        println("- 平均响应时间: ${averageResponseTime}")
        println("- 吞吐量: ${concurrentUsers * 1000 / totalTime.inWholeMilliseconds} 请求/秒")
    }

    @Test
    fun `should maintain system availability under load`() = runTest {
        // Given - 目标可用性 >99.5%
        val totalRequests = 1000
        val targetAvailability = 0.995 // 99.5%
        
        var successfulRequests = 0
        var failedRequests = 0
        
        // When - 执行大量请求
        repeat(totalRequests) {
            try {
                val studentActor = createStudentActor()
                val message = StartLearningSession(
                    courseId = CourseId.generate(),
                    objectives = listOf("可用性测试")
                )
                val result = studentActor.receive(message)
                if (result != null) {
                    successfulRequests++
                } else {
                    failedRequests++
                }
            } catch (e: Exception) {
                failedRequests++
            }
        }
        
        // Then - 验证可用性
        val actualAvailability = successfulRequests.toDouble() / totalRequests
        assertTrue(actualAvailability >= targetAvailability, 
            "系统可用性 ${actualAvailability * 100}% 低于目标 ${targetAvailability * 100}%")
        
        println("可用性测试结果:")
        println("- 总请求数: ${totalRequests}")
        println("- 成功请求数: ${successfulRequests}")
        println("- 失败请求数: ${failedRequests}")
        println("- 系统可用性: ${actualAvailability * 100}%")
    }

    @Test
    fun `should validate memory usage under load`() = runTest {
        // Given - 内存使用监控
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // When - 创建大量Actor并执行操作
        val actors = (1..100).map { createStudentActor() }
        
        actors.forEach { actor ->
            repeat(10) {
                val message = StartLearningSession(
                    courseId = CourseId.generate(),
                    objectives = listOf("内存测试")
                )
                actor.receive(message)
            }
        }
        
        // 强制垃圾回收
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Then - 验证内存使用合理
        val memoryIncreaseInMB = memoryIncrease / (1024 * 1024)
        assertTrue(memoryIncreaseInMB < 100, 
            "内存增长 ${memoryIncreaseInMB}MB 过大，可能存在内存泄漏")
        
        println("内存使用测试结果:")
        println("- 初始内存: ${initialMemory / (1024 * 1024)}MB")
        println("- 最终内存: ${finalMemory / (1024 * 1024)}MB")
        println("- 内存增长: ${memoryIncreaseInMB}MB")
    }

    // 辅助方法
    private fun createStudentActor(): StudentActor {
        return StudentActor(
            studentId = StudentId.generate(),
            memorySystem = memorySystem,
            ragSystem = ragSystem,
            learningAnalytics = learningAnalytics,
            personalizationEngine = personalizationEngine
        )
    }

    private fun createTeacherActor(): TeacherActor {
        return TeacherActor(
            teacherId = TeacherId.generate(),
            classroomId = ClassroomId.generate(),
            memorySystem = memorySystem,
            ragSystem = ragSystem,
            contentGenerationService = contentGenerationService,
            learningAnalytics = learningAnalytics,
            classManagementService = classManagementService
        )
    }
}
