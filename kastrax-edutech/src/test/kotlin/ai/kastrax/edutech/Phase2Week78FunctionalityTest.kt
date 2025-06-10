package ai.kastrax.edutech

import ai.kastrax.edutech.assessment.*
import ai.kastrax.edutech.generation.*
import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.progress.*
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmResponse
import ai.kastrax.core.llm.LlmUsage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * 第二阶段Week 7-8功能测试
 * 
 * 测试智能内容生成和基础评估功能
 * 按照ed2.md第二阶段Week 7-8计划验证功能实现
 */
class Phase2Week78FunctionalityTest {
    
    @Test
    fun `should generate text content successfully`() = runTest {
        // Given
        val mockLLMProvider = mockk<LlmProvider>()
        val templateRepository = InMemoryContentTemplateRepository()
        val qualityAssessment = ContentQualityAssessmentImpl()
        
        val contentGenerationService = ContentGenerationService(
            llmProvider = mockLLMProvider,
            templateRepository = templateRepository,
            qualityAssessment = qualityAssessment
        )
        
        coEvery { mockLLMProvider.generate(any(), any()) } returns LlmResponse(
            content = """
                # 数学基础：加法运算
                
                ## 概念介绍
                加法是数学中最基本的运算之一，表示将两个或多个数合并在一起。
                
                ## 基础知识
                加法使用"+"符号表示，例如：2 + 3 = 5
                
                ## 实例演示
                让我们看一个简单的例子：
                如果你有2个苹果，又买了3个苹果，那么你总共有多少个苹果？
                答案：2 + 3 = 5个苹果
                
                ## 练习指导
                尝试计算以下题目：
                1. 4 + 6 = ?
                2. 7 + 2 = ?
                
                ## 总结要点
                - 加法是合并数量的运算
                - 使用"+"符号
                - 结果称为"和"
            """.trimIndent(),
            finishReason = "stop",
            usage = LlmUsage(
                promptTokens = 100,
                completionTokens = 200,
                totalTokens = 300
            )
        )
        
        val request = ContentGenerationRequest(
            contentType = ContentType.TEXT,
            subject = Subject.MATHEMATICS,
            difficulty = DifficultyLevel.BEGINNER,
            topic = Topic("加法运算"),
            learningObjectives = listOf("理解加法概念", "掌握基本加法运算"),
            targetAudience = "小学生"
        )
        
        // When
        val result = contentGenerationService.generateContent(request)
        
        // Then
        assertTrue(result is ContentGenerationResult.Success)
        val generatedContent = (result as ContentGenerationResult.Success).content
        assertEquals("数学基础：加法运算", generatedContent.title)
        assertTrue(generatedContent.content.contains("加法"))
        assertTrue(generatedContent.content.contains("概念介绍"))
        assertEquals(Subject.MATHEMATICS, generatedContent.subject)
        assertEquals(DifficultyLevel.BEGINNER, generatedContent.difficulty)
    }
    
    @Test
    fun `should generate multimodal content successfully`() = runTest {
        // Given
        val mockLLMProvider = mockk<LlmProvider>()
        val templateRepository = InMemoryContentTemplateRepository()
        val qualityAssessment = ContentQualityAssessmentImpl()
        
        val contentGenerationService = ContentGenerationService(
            llmProvider = mockLLMProvider,
            templateRepository = templateRepository,
            qualityAssessment = qualityAssessment
        )
        
        coEvery { mockLLMProvider.generate(any(), any()) } returns LlmResponse(
            content = "生成的内容",
            finishReason = "stop",
            usage = LlmUsage(50, 100, 150)
        )
        
        val request = MultimodalGenerationRequest(
            contentType = ContentType.INTERACTIVE,
            subject = Subject.PHYSICS,
            difficulty = DifficultyLevel.INTERMEDIATE,
            topic = Topic("力学基础"),
            learningObjectives = listOf("理解力的概念", "掌握力的计算"),
            modalities = setOf(ContentModality.TEXT, ContentModality.VIDEO, ContentModality.INTERACTIVE)
        )
        
        // When
        val result = contentGenerationService.generateMultimodalContent(request)
        
        // Then
        assertTrue(result is MultimodalGenerationResult.Success)
        val components = (result as MultimodalGenerationResult.Success).components
        assertTrue(components.containsKey(ContentModality.TEXT))
        assertTrue(components.containsKey(ContentModality.VIDEO))
        assertTrue(components.containsKey(ContentModality.INTERACTIVE))
    }
    
    @Test
    fun `should assess content quality correctly`() = runTest {
        // Given
        val qualityAssessment = ContentQualityAssessmentImpl()
        
        val content = GeneratedContent(
            id = GeneratedContentId.generate(),
            requestId = "test_request",
            contentType = ContentType.TEXT,
            subject = Subject.MATHEMATICS,
            difficulty = DifficultyLevel.BEGINNER,
            topic = Topic("加法运算"),
            title = "数学基础：加法运算",
            content = """
                # 加法运算基础
                
                加法是数学中最基本的运算之一。它表示将两个或多个数合并在一起的过程。
                
                ## 基本概念
                加法使用"+"符号表示。例如：2 + 3 = 5
                
                ## 实例演示
                如果你有2个苹果，又买了3个苹果，那么你总共有5个苹果。
                
                ## 练习
                1. 4 + 6 = 10
                2. 7 + 2 = 9
                
                ## 总结
                加法帮助我们计算总数量。
            """.trimIndent(),
            learningObjectives = listOf("理解加法概念", "掌握基本加法运算"),
            estimatedDuration = 5,
            generatedAt = Clock.System.now()
        )
        
        // When
        val qualityScore = qualityAssessment.assessContent(content)
        
        // Then
        assertTrue(qualityScore.overallScore > 0.0)
        assertTrue(qualityScore.accuracyScore > 0.0)
        assertTrue(qualityScore.clarityScore > 0.0)
        assertTrue(qualityScore.relevanceScore > 0.0)
        assertTrue(qualityScore.engagementScore > 0.0)
        assertTrue(qualityScore.educationalValueScore > 0.0)
        assertTrue(qualityScore.feedback.isNotBlank())
    }
    
    @Test
    fun `should create and grade multiple choice assessment`() = runTest {
        // Given
        val mockLLMProvider = mockk<LlmProvider>()
        val assessmentRepository = InMemoryAssessmentRepository()
        val assessmentService = AssessmentService(mockLLMProvider, assessmentRepository)
        
        val questions = listOf(
            Question(
                type = QuestionType.MULTIPLE_CHOICE,
                content = "2 + 3 等于多少？",
                options = listOf("4", "5", "6", "7"),
                correctAnswer = "5",
                points = 1.0
            ),
            Question(
                type = QuestionType.MULTIPLE_CHOICE,
                content = "10 - 4 等于多少？",
                options = listOf("5", "6", "7", "8"),
                correctAnswer = "6",
                points = 1.0
            )
        )
        
        val createRequest = AssessmentCreationRequest(
            title = "数学基础测试",
            description = "测试基本的加减法运算",
            subject = Subject.MATHEMATICS,
            difficulty = DifficultyLevel.BEGINNER,
            questions = questions,
            timeLimit = 10,
            passingScore = 60.0,
            createdBy = "teacher1"
        )
        
        // When - 创建评估
        val createResult = assessmentService.createAssessment(createRequest)
        
        // Then
        assertTrue(createResult is AssessmentCreationResult.Success)
        val assessment = (createResult as AssessmentCreationResult.Success).assessment
        assertEquals("数学基础测试", assessment.title)
        assertEquals(2, assessment.questions.size)
        
        // When - 提交答案
        val studentId = StudentId.generate()
        val answers = listOf(
            Answer(questions[0].id, "5"), // 正确
            Answer(questions[1].id, "7")  // 错误
        )
        
        val submission = AssessmentSubmission(
            assessmentId = assessment.id,
            studentId = studentId,
            answers = answers,
            startedAt = Clock.System.now().minus(kotlin.time.Duration.parse("PT5M")),
            timeSpent = 300
        )
        
        val submitResult = assessmentService.submitAssessment(submission)
        
        // Then
        assertTrue(submitResult is AssessmentSubmissionResult.Success)
        val gradingResult = (submitResult as AssessmentSubmissionResult.Success).gradingResult
        assertEquals(1.0, gradingResult.totalScore) // 1题正确
        assertEquals(2.0, gradingResult.maxScore)   // 总共2题
        assertEquals(50.0, gradingResult.percentage) // 50%
        assertFalse(gradingResult.passed) // 未达到60%及格线
    }
    
    @Test
    fun `should grade short answer question with LLM`() = runTest {
        // Given
        val mockLLMProvider = mockk<LlmProvider>()
        val assessmentRepository = InMemoryAssessmentRepository()
        val assessmentService = AssessmentService(mockLLMProvider, assessmentRepository)
        
        coEvery { mockLLMProvider.generate(any(), any()) } returns LlmResponse(
            content = """
                {
                    "score": 8,
                    "isCorrect": true,
                    "feedback": "答案基本正确，很好地解释了加法的概念和应用。"
                }
            """.trimIndent(),
            finishReason = "stop",
            usage = LlmUsage(30, 50, 80)
        )
        
        val question = Question(
            type = QuestionType.SHORT_ANSWER,
            content = "请解释什么是加法，并举一个例子。",
            correctAnswer = "加法是将两个或多个数合并的运算。例如：2+3=5表示将2和3合并得到5。",
            points = 10.0
        )
        
        val answer = Answer(
            questionId = question.id,
            content = "加法就是把数字加起来。比如2个苹果加3个苹果等于5个苹果。"
        )
        
        // When
        val grade = assessmentService.gradeQuestion(question, answer)
        
        // Then
        assertEquals(8.0, grade.score)
        assertEquals(10.0, grade.maxScore)
        assertTrue(grade.isCorrect)
        assertTrue(grade.feedback.contains("基本正确"))
    }
    
    @Test
    fun `should track learning progress correctly`() = runTest {
        // Given
        val progressTracker = LearningProgressTracker()
        val studentId = StudentId.generate()
        
        val activity = ai.kastrax.edutech.models.LearningActivity.create(
            type = ActivityType.READING,
            topic = Topic("数学基础"),
            difficulty = DifficultyLevel.BEGINNER,
            skillsInvolved = setOf(Skill.LOGICAL_REASONING),
            subject = Subject.MATHEMATICS
        )
        
        val performance = ai.kastrax.edutech.progress.ActivityPerformance(
            accuracy = 0.85,
            completionTime = 300,
            engagementLevel = 0.9,
            completed = true
        )
        
        // When
        val updateResult = progressTracker.recordLearningProgress(studentId, activity, performance)
        
        // Then
        assertTrue(updateResult is ProgressUpdateResult.Success)
        
        // 验证进度记录
        val progressResult = progressTracker.getStudentProgress(studentId)
        assertTrue(progressResult is StudentProgressResult.Success)
        
        val progress = (progressResult as StudentProgressResult.Success).progress
        assertEquals(1, progress.overallProgress.totalActivities)
        assertEquals(1, progress.overallProgress.completedActivities)
        assertEquals(0.85, progress.overallProgress.averagePerformance)
        assertEquals(100.0, progress.overallProgress.completionRate)
    }
    
    @Test
    fun `should generate learning trajectory`() = runTest {
        // Given
        val progressTracker = LearningProgressTracker()
        val studentId = StudentId.generate()
        
        // 记录多个学习活动
        val activities = listOf(
            ai.kastrax.edutech.models.LearningActivity.create(ActivityType.READING, Topic("加法"), DifficultyLevel.BEGINNER, setOf(Skill.LOGICAL_REASONING), Subject.MATHEMATICS),
            ai.kastrax.edutech.models.LearningActivity.create(ActivityType.PRACTICE, Topic("减法"), DifficultyLevel.BEGINNER, setOf(Skill.PROBLEM_SOLVING), Subject.MATHEMATICS),
            ai.kastrax.edutech.models.LearningActivity.create(ActivityType.QUIZ, Topic("乘法"), DifficultyLevel.INTERMEDIATE, setOf(Skill.ANALYTICAL_THINKING), Subject.MATHEMATICS)
        )
        
        val performances = listOf(
            ai.kastrax.edutech.progress.ActivityPerformance(0.8, 200, 0.9, true),
            ai.kastrax.edutech.progress.ActivityPerformance(0.9, 250, 0.8, true),
            ai.kastrax.edutech.progress.ActivityPerformance(0.7, 300, 0.85, true)
        )

        activities.zip(performances).forEach { (activity: ai.kastrax.edutech.models.LearningActivity, performance: ai.kastrax.edutech.progress.ActivityPerformance) ->
            progressTracker.recordLearningProgress(studentId, activity, performance)
        }
        
        // When
        val trajectoryResult = progressTracker.getLearningTrajectory(studentId)
        
        // Then
        assertTrue(trajectoryResult is LearningTrajectoryResult.Success)
        val trajectory = (trajectoryResult as LearningTrajectoryResult.Success).trajectory
        assertEquals(3, trajectory.totalPoints)
        assertEquals(3, trajectory.points.size)
        
        // 验证轨迹点数据
        val firstPoint = trajectory.points[0]
        assertEquals(ActivityType.READING, firstPoint.activityType)
        assertEquals(Topic("加法"), firstPoint.topic)
        assertEquals(0.8, firstPoint.performance)
    }
    
    @Test
    fun `should detect progress alerts`() = runTest {
        // Given
        val progressTracker = LearningProgressTracker()
        val studentId = StudentId.generate()
        
        // 记录低表现的学习活动
        val activity = ai.kastrax.edutech.models.LearningActivity.create(
            type = ActivityType.PRACTICE,
            topic = Topic("数学练习"),
            difficulty = DifficultyLevel.BEGINNER,
            skillsInvolved = setOf(Skill.LOGICAL_REASONING),
            subject = Subject.MATHEMATICS
        )
        
        val lowPerformance = ai.kastrax.edutech.progress.ActivityPerformance(
            accuracy = 0.4, // 低表现
            completionTime = 100,
            engagementLevel = 0.5,
            completed = true
        )
        
        // When
        progressTracker.recordLearningProgress(studentId, activity, lowPerformance)
        val alertsResult = progressTracker.getProgressAlerts(studentId)
        
        // Then
        assertTrue(alertsResult is ProgressAlertsResult.Success)
        val alerts = (alertsResult as ProgressAlertsResult.Success).alerts
        assertTrue(alerts.isNotEmpty())
        
        val lowPerformanceAlert = alerts.find { it.type == AlertType.LOW_PERFORMANCE }
        assertNotNull(lowPerformanceAlert)
        assertEquals(AlertType.LOW_PERFORMANCE, lowPerformanceAlert.type)
        assertTrue(lowPerformanceAlert.message.contains("表现需要改进"))
    }
    
    @Test
    fun `should generate progress report`() = runTest {
        // Given
        val progressTracker = LearningProgressTracker()
        val studentId = StudentId.generate()
        
        // 记录一些学习活动
        val activity = ai.kastrax.edutech.models.LearningActivity.create(
            type = ActivityType.READING,
            topic = Topic("物理基础"),
            difficulty = DifficultyLevel.INTERMEDIATE,
            skillsInvolved = setOf(Skill.ANALYTICAL_THINKING),
            subject = Subject.PHYSICS
        )

        val performance = ai.kastrax.edutech.progress.ActivityPerformance(0.85, 400, 0.9, true)
        progressTracker.recordLearningProgress(studentId, activity, performance)
        
        // When
        val reportResult = progressTracker.generateProgressReport(studentId, ProgressReportType.SUMMARY)
        
        // Then
        assertTrue(reportResult is ProgressReportResult.Success)
        val report = (reportResult as ProgressReportResult.Success).report
        assertEquals(ProgressReportType.SUMMARY, report.reportType)
        assertEquals("学习进度总结", report.title)
        assertTrue(report.content.contains("整体表现"))
        assertTrue(report.content.contains("完成活动"))
    }
    
    @Test
    fun `should set and track learning goals`() = runTest {
        // Given
        val progressTracker = LearningProgressTracker()
        val studentId = StudentId.generate()
        
        val goals = listOf(
            LearningGoal(
                title = "完成10个数学练习",
                description = "在本周内完成10个数学基础练习",
                targetValue = 10.0,
                unit = "个活动",
                priority = GoalPriority.HIGH
            ),
            LearningGoal(
                title = "学习时间达到5小时",
                description = "本月学习时间累计达到5小时",
                targetValue = 300.0, // 5小时 = 300分钟
                unit = "分钟",
                priority = GoalPriority.MEDIUM
            )
        )
        
        // When
        val goalResult = progressTracker.setLearningGoals(studentId, goals)
        
        // Then
        assertTrue(goalResult is GoalSettingResult.Success)
        
        // 验证目标已设置
        val progressResult = progressTracker.getStudentProgress(studentId)
        assertTrue(progressResult is StudentProgressResult.Success)
        
        val progress = (progressResult as StudentProgressResult.Success).progress
        assertEquals(2, progress.learningGoals.size)
        assertEquals("完成10个数学练习", progress.learningGoals[0].title)
        assertEquals(GoalPriority.HIGH, progress.learningGoals[0].priority)
    }
}
