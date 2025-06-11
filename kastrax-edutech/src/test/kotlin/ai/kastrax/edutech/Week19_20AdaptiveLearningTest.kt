package ai.kastrax.edutech

import ai.kastrax.edutech.adaptive.*
import ai.kastrax.edutech.models.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.time.Duration.Companion.minutes

/**
 * Week 19-20 AI驱动的自适应学习引擎测试
 * 
 * 测试范围：
 * - 实时学习能力评估
 * - 动态难度调整
 * - 个性化学习节奏控制
 * - 智能学习干预
 * - 学习效果预测和优化
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Week19_20AdaptiveLearningTest {
    
    private lateinit var adaptiveEngine: AdaptiveLearningEngine
    private lateinit var adaptiveService: AdaptiveLearningService
    
    @BeforeAll
    fun setup() {
        println("🚀 初始化Week 19-20自适应学习引擎测试环境...")
        
        adaptiveEngine = AdaptiveLearningEngine()
        adaptiveService = AdaptiveLearningService()
        
        println("✅ Week 19-20自适应学习引擎测试环境初始化完成")
    }
    
    @Test
    @DisplayName("AL-001: 实时学习能力评估测试")
    fun testRealTimeLearningCapabilityAssessment() = runBlocking {
        println("\n🧠 测试实时学习能力评估...")
        
        // 1. 准备测试数据
        val studentId = StudentId.generate()
        val currentActivity = ai.kastrax.edutech.adaptive.LearningActivity(
            id = "activity_001",
            title = "数学基础练习",
            type = "exercise",
            difficulty = DifficultyLevel.INTERMEDIATE,
            estimatedTime = 30
        )
        
        val performanceHistory = listOf(
            PerformanceRecord(
                activityId = "prev_001",
                accuracy = 0.8,
                completionTime = 25,
                timestamp = Clock.System.now().minus(10.minutes),
                difficulty = DifficultyLevel.BEGINNER
            ),
            PerformanceRecord(
                activityId = "prev_002",
                accuracy = 0.75,
                completionTime = 30,
                timestamp = Clock.System.now().minus(5.minutes),
                difficulty = DifficultyLevel.INTERMEDIATE
            )
        )
        
        // 2. 执行能力评估
        val assessmentResult = adaptiveEngine.assessLearningCapability(
            studentId = studentId,
            currentActivity = currentActivity,
            performanceHistory = performanceHistory
        )
        
        // 3. 验证结果
        assertTrue(assessmentResult is CapabilityAssessmentResult.Success, "学习能力评估应该成功")
        val assessment = (assessmentResult as CapabilityAssessmentResult.Success).assessment
        
        assertNotNull(assessment, "评估结果不应为空")
        assertEquals(studentId, assessment.studentId, "评估应该属于正确的学生")
        assertTrue(assessment.capabilityMetrics.overallCapability >= 0.0, "整体能力应该为非负值")
        assertTrue(assessment.capabilityMetrics.overallCapability <= 1.0, "整体能力应该不超过1.0")
        assertTrue(assessment.recommendations.isNotEmpty(), "应该提供能力提升建议")
        
        println("✅ 实时学习能力评估测试通过")
        println("   整体能力: ${String.format("%.2f", assessment.capabilityMetrics.overallCapability)}")
        println("   处理速度: ${String.format("%.2f", assessment.capabilityMetrics.processingSpeed)}")
        println("   工作记忆: ${String.format("%.2f", assessment.capabilityMetrics.workingMemory)}")
        println("   建议数量: ${assessment.recommendations.size}")
    }
    
    @Test
    @DisplayName("AL-002: 动态难度调整测试")
    fun testDynamicDifficultyAdjustment() = runBlocking {
        println("\n⚖️ 测试动态难度调整...")
        
        val studentId = StudentId.generate()
        val currentActivity = ai.kastrax.edutech.adaptive.LearningActivity(
            id = "activity_002",
            title = "编程练习",
            type = "coding",
            difficulty = DifficultyLevel.INTERMEDIATE,
            estimatedTime = 45
        )
        
        // 模拟学生表现过好，需要增加难度
        val realtimePerformance = RealtimePerformance(
            currentAccuracy = 0.95,
            currentSpeed = 1.2,
            frustrationLevel = 0.1,
            confidenceLevel = 0.9
        )
        
        // 执行难度调整
        val adjustmentResult = adaptiveEngine.adjustDifficultyDynamically(
            studentId = studentId,
            currentActivity = currentActivity,
            realtimePerformance = realtimePerformance
        )
        
        assertTrue(adjustmentResult is DifficultyAdjustmentResult.Success, "难度调整应该成功")
        val adjustment = (adjustmentResult as DifficultyAdjustmentResult.Success).adjustment
        val adjustedActivity = adjustmentResult.adjustedActivity
        
        assertNotNull(adjustment, "调整结果不应为空")
        assertEquals(studentId, adjustment.studentId, "调整应该属于正确的学生")
        assertEquals(currentActivity.difficulty, adjustment.originalDifficulty, "原始难度应该匹配")
        assertNotNull(adjustment.adjustmentReason, "应该提供调整原因")
        assertNotNull(adjustedActivity, "调整后的活动不应为空")
        
        println("✅ 动态难度调整测试通过")
        println("   原始难度: ${adjustment.originalDifficulty}")
        println("   调整后难度: ${adjustment.adjustedDifficulty}")
        println("   调整原因: ${adjustment.adjustmentReason}")
        println("   预期影响: ${adjustment.expectedImpact}")
    }
    
    @Test
    @DisplayName("AL-003: 个性化学习节奏控制测试")
    fun testPersonalizedPaceControl() = runBlocking {
        println("\n⏱️ 测试个性化学习节奏控制...")
        
        val studentId = StudentId.generate()
        val currentSession = ai.kastrax.edutech.adaptive.LearningSession(
            id = "session_003",
            studentId = studentId,
            startTime = Clock.System.now().minus(30.minutes),
            activities = emptyList(),
            performance = SessionPerformance(
                overallScore = 0.8,
                completionRate = 0.7,
                engagementLevel = 0.6
            )
        )
        
        val pacePreferences = PacePreferences(
            preferredIntensity = PaceIntensity.MODERATE,
            maxSessionLength = 60,
            preferredBreakFrequency = 3,
            adaptiveAdjustment = true
        )
        
        // 执行节奏控制
        val paceResult = adaptiveEngine.controlLearningPace(
            studentId = studentId,
            currentSession = currentSession,
            pacePreferences = pacePreferences
        )
        
        assertTrue(paceResult is PaceControlResult.Success, "学习节奏控制应该成功")
        val paceControl = (paceResult as PaceControlResult.Success).control
        
        assertNotNull(paceControl, "节奏控制结果不应为空")
        assertEquals(studentId, paceControl.studentId, "节奏控制应该属于正确的学生")
        assertEquals(currentSession.id, paceControl.sessionId, "会话ID应该匹配")
        assertNotNull(paceControl.currentPace, "当前节奏不应为空")
        assertNotNull(paceControl.optimalPace, "最优节奏不应为空")
        assertTrue(paceControl.effectiveness.effectiveness >= 0.0, "效果评估应该为非负值")
        
        println("✅ 个性化学习节奏控制测试通过")
        println("   当前节奏: ${paceControl.currentPace.activitiesPerHour}活动/小时")
        println("   最优节奏: ${paceControl.optimalPace.activitiesPerHour}活动/小时")
        println("   调整数量: ${paceControl.adjustments.size}")
        println("   效果评估: ${String.format("%.2f", paceControl.effectiveness.effectiveness)}")
    }
    
    @Test
    @DisplayName("AL-004: 智能学习干预测试")
    fun testIntelligentLearningIntervention() = runBlocking {
        println("\n🆘 测试智能学习干预...")
        
        val studentId = StudentId.generate()
        
        // 模拟需要干预的情况：表现下降
        val interventionTrigger = InterventionTrigger(
            type = TriggerType.PERFORMANCE_DROP,
            threshold = 0.7,
            currentValue = 0.5,
            triggerTime = Clock.System.now()
        )
        
        val interventionContext = InterventionContext(
            currentActivity = ai.kastrax.edutech.adaptive.LearningActivity(
                id = "activity_004",
                title = "困难数学题",
                type = "problem_solving",
                difficulty = DifficultyLevel.ADVANCED,
                estimatedTime = 60
            ),
            sessionProgress = 0.4,
            studentState = StudentState(
                energy = 0.3,
                motivation = 0.4,
                focus = 0.5,
                stress = 0.8
            ),
            environmentFactors = mapOf("time_of_day" to "afternoon", "noise_level" to "high")
        )
        
        // 执行学习干预
        val interventionResult = adaptiveEngine.provideLearningIntervention(
            studentId = studentId,
            interventionTrigger = interventionTrigger,
            contextData = interventionContext
        )
        
        assertTrue(interventionResult is InterventionResult.Success, "学习干预应该成功")
        val intervention = (interventionResult as InterventionResult.Success).intervention
        
        assertNotNull(intervention, "干预结果不应为空")
        assertEquals(studentId, intervention.studentId, "干预应该属于正确的学生")
        assertNotNull(intervention.strategy, "干预策略不应为空")
        assertNotNull(intervention.content, "干预内容不应为空")
        assertTrue(intervention.content.message.isNotEmpty(), "干预消息不应为空")
        assertEquals(InterventionStatus.EXECUTED, intervention.status, "干预状态应该为已执行")
        
        println("✅ 智能学习干预测试通过")
        println("   干预策略: ${intervention.strategy.approach}")
        println("   干预时机: ${intervention.strategy.timing}")
        println("   干预消息: ${intervention.content.message}")
        println("   行动项目: ${intervention.content.actionItems.size}")
    }
    
    @Test
    @DisplayName("AL-005: 学习效果预测和优化测试")
    fun testLearningOutcomePredictionAndOptimization() = runBlocking {
        println("\n🔮 测试学习效果预测和优化...")
        
        val studentId = StudentId.generate()
        val learningPlan = LearningPlan(
            id = "plan_005",
            studentId = studentId,
            activities = listOf(
                ai.kastrax.edutech.adaptive.LearningActivity("act1", "基础概念", "reading", DifficultyLevel.BEGINNER, 20),
                ai.kastrax.edutech.adaptive.LearningActivity("act2", "练习题", "exercise", DifficultyLevel.INTERMEDIATE, 30),
                ai.kastrax.edutech.adaptive.LearningActivity("act3", "综合应用", "project", DifficultyLevel.ADVANCED, 60)
            ),
            estimatedDuration = 110,
            objectives = listOf("掌握基础概念", "提高解题能力", "应用知识解决问题")
        )
        
        val historicalData = HistoricalLearningData(
            studentId = studentId,
            performanceHistory = emptyList(),
            sessionHistory = emptyList(),
            learningPatterns = listOf("视觉学习者", "需要频繁休息", "下午效率较低")
        )
        
        // 执行预测和优化
        val optimizationResult = adaptiveEngine.predictAndOptimizeLearningOutcome(
            studentId = studentId,
            learningPlan = learningPlan,
            historicalData = historicalData
        )
        
        assertTrue(optimizationResult is OptimizationResult.Success, "学习效果优化应该成功")
        val optimization = (optimizationResult as OptimizationResult.Success).optimization
        
        assertNotNull(optimization, "优化结果不应为空")
        assertEquals(studentId, optimization.studentId, "优化应该属于正确的学生")
        assertEquals(learningPlan, optimization.originalPlan, "原始计划应该匹配")
        assertNotNull(optimization.optimizedPlan, "优化后计划不应为空")
        assertTrue(optimization.prediction.successProbability >= 0.0, "成功概率应该为非负值")
        assertTrue(optimization.prediction.successProbability <= 1.0, "成功概率应该不超过1.0")
        assertTrue(optimization.expectedImprovement >= 0.0, "预期改进应该为非负值")
        
        println("✅ 学习效果预测和优化测试通过")
        println("   成功概率: ${String.format("%.2f", optimization.prediction.successProbability * 100)}%")
        println("   预期完成时间: ${optimization.prediction.expectedCompletionTime}分钟")
        println("   预测表现: ${String.format("%.2f", optimization.prediction.predictedPerformance)}")
        println("   预期改进: ${String.format("%.2f", optimization.expectedImprovement * 100)}%")
        println("   优化策略数量: ${optimization.optimizationStrategies.size}")
    }
    
    @Test
    @DisplayName("AL-006: 自适应学习服务集成测试")
    fun testAdaptiveLearningServiceIntegration() = runBlocking {
        println("\n🔗 测试自适应学习服务集成...")
        
        val studentId = StudentId.generate()
        val learningObjectives = listOf("掌握Kotlin基础", "理解面向对象编程", "完成项目实践")
        val preferences = AdaptivePreferences(
            difficultyAdaptation = true,
            paceAdaptation = true,
            interventionEnabled = true,
            personalizedFeedback = true
        )
        
        // 1. 启动自适应学习会话
        val sessionResult = adaptiveService.startAdaptiveSession(
            studentId = studentId,
            learningObjectives = learningObjectives,
            preferences = preferences
        )
        
        assertTrue(sessionResult is AdaptiveSessionResult.Success, "自适应会话启动应该成功")
        val session = (sessionResult as AdaptiveSessionResult.Success).session
        
        assertNotNull(session, "会话不应为空")
        assertEquals(studentId, session.studentId, "会话应该属于正确的学生")
        assertEquals(ai.kastrax.edutech.adaptive.SessionStatus.ACTIVE, session.status, "会话状态应该为活跃")
        assertEquals(learningObjectives, session.learningPlan.objectives, "学习目标应该匹配")
        
        // 2. 生成自适应学习报告
        val reportResult = adaptiveService.generateAdaptiveLearningReport(
            studentId = studentId,
            sessionId = session.sessionId,
            reportType = AdaptiveReportType.SESSION_SUMMARY
        )
        
        assertTrue(reportResult is AdaptiveReportResult.Success, "报告生成应该成功")
        val report = (reportResult as AdaptiveReportResult.Success).report
        
        assertNotNull(report, "报告不应为空")
        assertEquals(studentId, report.studentId, "报告应该属于正确的学生")
        assertEquals(session.sessionId, report.sessionId, "会话ID应该匹配")
        assertEquals(AdaptiveReportType.SESSION_SUMMARY, report.type, "报告类型应该匹配")
        
        println("✅ 自适应学习服务集成测试通过")
        println("   会话ID: ${session.sessionId}")
        println("   学习目标数量: ${session.learningPlan.objectives.size}")
        println("   预估时长: ${session.learningPlan.estimatedDuration}分钟")
        println("   报告类型: ${report.type}")
        println("   报告生成时间: ${report.generatedAt}")
    }
    
    @AfterAll
    fun cleanup() {
        println("\n🧹 清理测试环境...")
        println("✅ Week 19-20自适应学习引擎测试完成")
    }
}
