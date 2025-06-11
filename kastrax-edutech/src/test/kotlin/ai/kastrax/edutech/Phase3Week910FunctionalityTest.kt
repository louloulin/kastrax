package ai.kastrax.edutech

import ai.kastrax.edutech.grading.*
import ai.kastrax.core.llm.*
import ai.kastrax.edutech.models.*
// import ai.kastrax.edutech.recommendation.PersonalizedRecommendationEngine
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration

/**
 * 第三阶段Week 9-10功能测试
 * 测试智能作业批改系统的核心功能
 */
class Phase3Week910FunctionalityTest {
    
    private lateinit var mockLlmProvider: LlmProvider
    private lateinit var codeExecutor: CodeExecutor
    private lateinit var qualityAssurance: QualityAssuranceService
    private lateinit var assignmentGradingService: AssignmentGradingService
    private lateinit var feedbackEngine: FeedbackGenerationEngine
    private lateinit var qualityAssuranceSystem: QualityAssuranceSystem
    
    @BeforeTest
    fun setup() {
        mockLlmProvider = mockk<LlmProvider>()
        codeExecutor = SimpleCodeExecutor()
        qualityAssurance = SimpleQualityAssuranceService()
        
        assignmentGradingService = AssignmentGradingService(
            llmProvider = mockLlmProvider,
            codeExecutor = codeExecutor,
            qualityAssurance = qualityAssurance
        )
        
        feedbackEngine = FeedbackGenerationEngine(
            llmProvider = mockLlmProvider
        )
        
        qualityAssuranceSystem = QualityAssuranceSystem(
            humanReviewService = SimpleHumanReviewService(),
            qualityMetricsCollector = SimpleQualityMetricsCollector(),
            continuousImprovementEngine = SimpleContinuousImprovementEngine()
        )
        
        // Mock LLM responses
        coEvery { mockLlmProvider.generate(any(), any()) } returns LlmResponse(
            content = "分析结果",
            usage = LlmUsage(100, 50, 150),
            finishReason = "completed"
        )
        
        // 移除推荐引擎的mock，因为已经简化实现
    }
    
    @Test
    fun `should grade programming assignment successfully`() = runTest {
        // Given
        val submission = createProgrammingSubmission()
        val request = createGradingRequest()
        
        // When
        val result = assignmentGradingService.gradeAssignment(submission, request)
        
        // Then
        assertTrue(result is AssignmentGradingResult.Success)
        val gradingResult = (result as AssignmentGradingResult.Success).result
        
        assertTrue(gradingResult.overallScore >= 0.0)
        assertTrue(gradingResult.overallScore <= 100.0)
        assertNotNull(gradingResult.feedback)
        assertTrue(gradingResult.rubricScores.isNotEmpty())
        assertTrue(gradingResult.confidence > 0.0)
    }
    
    @Test
    fun `should grade math assignment successfully`() = runTest {
        // Given
        val submission = createMathSubmission()
        val request = createGradingRequest()
        
        // When
        val result = assignmentGradingService.gradeAssignment(submission, request)
        
        // Then
        assertTrue(result is AssignmentGradingResult.Success)
        val gradingResult = (result as AssignmentGradingResult.Success).result
        
        assertTrue(gradingResult.overallScore >= 0.0)
        assertTrue(gradingResult.overallScore <= 100.0)
        assertNotNull(gradingResult.feedback)
        assertTrue(gradingResult.confidence > 0.0)
    }
    
    @Test
    fun `should grade writing assignment successfully`() = runTest {
        // Given
        val submission = createWritingSubmission()
        val request = createGradingRequest()
        
        // When
        val result = assignmentGradingService.gradeAssignment(submission, request)
        
        // Then
        assertTrue(result is AssignmentGradingResult.Success)
        val gradingResult = (result as AssignmentGradingResult.Success).result
        
        assertTrue(gradingResult.overallScore >= 0.0)
        assertTrue(gradingResult.overallScore <= 100.0)
        assertNotNull(gradingResult.feedback)
        assertTrue(gradingResult.confidence > 0.0)
    }
    
    @Test
    fun `should grade creative assignment successfully`() = runTest {
        // Given
        val submission = createCreativeSubmission()
        val request = createGradingRequest()
        
        // When
        val result = assignmentGradingService.gradeAssignment(submission, request)
        
        // Then
        assertTrue(result is AssignmentGradingResult.Success)
        val gradingResult = (result as AssignmentGradingResult.Success).result
        
        assertTrue(gradingResult.overallScore >= 0.0)
        assertTrue(gradingResult.overallScore <= 100.0)
        assertNotNull(gradingResult.feedback)
        assertTrue(gradingResult.confidence > 0.0)
    }
    
    @Test
    fun `should execute code and provide test results`() = runTest {
        // Given
        val sourceCode = """
            fun add(a: Int, b: Int): Int {
                return a + b
            }
        """.trimIndent()
        
        val testCases = listOf(
            TestCase("1,2", "3", "基本加法测试"),
            TestCase("0,0", "0", "零值测试"),
            TestCase("-1,1", "0", "负数测试")
        )
        
        // When
        val results = codeExecutor.executeCode(sourceCode, ProgrammingLanguage.KOTLIN, testCases)
        
        // Then
        assertEquals(3, results.size)
        results.forEach { result ->
            assertNotNull(result.actualOutput)
            assertTrue(result.executionTime > 0)
        }
    }
    
    @Test
    fun `should generate enhanced feedback successfully`() = runTest {
        // Given
        val submission = createProgrammingSubmission()
        val gradingResult = createGradingResult()
        val studentProfile = createStudentProfile()
        
        // When
        val enhancedFeedback = feedbackEngine.generateDetailedFeedback(
            submission, gradingResult, studentProfile
        )
        
        // Then
        assertNotNull(enhancedFeedback.errorAnalysis)
        assertNotNull(enhancedFeedback.improvementPlan)
        assertNotNull(enhancedFeedback.personalizedGuidance)
        assertNotNull(enhancedFeedback.actionPlan)
        assertTrue(enhancedFeedback.resourceRecommendations.isNotEmpty())
        assertTrue(enhancedFeedback.actionPlan.steps.isNotEmpty())
    }
    
    @Test
    fun `should perform quality assurance successfully`() = runTest {
        // Given
        val submission = createProgrammingSubmission()
        val gradingResult = createGradingResult()
        
        // When
        val qaResult = qualityAssuranceSystem.performQualityAssurance(gradingResult, submission)
        
        // Then
        assertNotNull(qaResult.qualityAssessment)
        assertTrue(qaResult.qualityAssessment.overallQuality >= 0.0)
        assertTrue(qaResult.qualityAssessment.overallQuality <= 1.0)
        assertTrue(qaResult.qualityAssessment.accuracy >= 0.0)
        assertTrue(qaResult.qualityAssessment.consistency >= 0.0)
        assertTrue(qaResult.qualityAssessment.completeness >= 0.0)
        assertTrue(qaResult.qualityAssessment.fairness >= 0.0)
    }
    
    @Test
    fun `should trigger human review for low quality results`() = runTest {
        // Given
        val submission = createProgrammingSubmission()
        val lowQualityResult = createGradingResult().copy(
            confidence = 0.3, // 低置信度
            needsReview = true
        )
        
        // When
        val qaResult = qualityAssuranceSystem.performQualityAssurance(lowQualityResult, submission)
        
        // Then
        assertNotNull(qaResult.humanReviewResult)
        assertNotNull(qaResult.humanReviewResult!!.reviewerId)
        assertTrue(qaResult.recommendations.isNotEmpty())
    }
    
    @Test
    fun `should handle different assignment types correctly`() = runTest {
        // Given
        val programmingSubmission = createProgrammingSubmission()
        val mathSubmission = createMathSubmission()
        val writingSubmission = createWritingSubmission()
        val creativeSubmission = createCreativeSubmission()
        val request = createGradingRequest()
        
        // When
        val programmingResult = assignmentGradingService.gradeAssignment(programmingSubmission, request)
        val mathResult = assignmentGradingService.gradeAssignment(mathSubmission, request)
        val writingResult = assignmentGradingService.gradeAssignment(writingSubmission, request)
        val creativeResult = assignmentGradingService.gradeAssignment(creativeSubmission, request)
        
        // Then
        assertTrue(programmingResult is AssignmentGradingResult.Success)
        assertTrue(mathResult is AssignmentGradingResult.Success)
        assertTrue(writingResult is AssignmentGradingResult.Success)
        assertTrue(creativeResult is AssignmentGradingResult.Success)
        
        // 验证每种类型都有适当的反馈
        val programmingFeedback = (programmingResult as AssignmentGradingResult.Success).result.feedback
        val mathFeedback = (mathResult as AssignmentGradingResult.Success).result.feedback
        val writingFeedback = (writingResult as AssignmentGradingResult.Success).result.feedback
        val creativeFeedback = (creativeResult as AssignmentGradingResult.Success).result.feedback
        
        assertNotEquals(programmingFeedback.summary, mathFeedback.summary)
        assertNotEquals(writingFeedback.summary, creativeFeedback.summary)
    }
    
    @Test
    fun `should provide comprehensive improvement suggestions`() = runTest {
        // Given
        val submission = createProgrammingSubmission()
        val gradingResult = createGradingResult()
        
        // When
        val enhancedFeedback = feedbackEngine.generateDetailedFeedback(submission, gradingResult)
        
        // Then
        val improvementPlan = enhancedFeedback.improvementPlan
        assertTrue(improvementPlan.shortTermGoals.isNotEmpty())
        assertTrue(improvementPlan.mediumTermGoals.isNotEmpty())
        assertTrue(improvementPlan.longTermGoals.isNotEmpty())
        
        val actionPlan = enhancedFeedback.actionPlan
        assertTrue(actionPlan.steps.isNotEmpty())
        assertTrue(actionPlan.estimatedTotalTime > 0)
        
        actionPlan.steps.forEach { step ->
            assertTrue(step.stepNumber > 0)
            assertTrue(step.title.isNotBlank())
            assertTrue(step.description.isNotBlank())
        }
    }
    
    // 辅助方法
    private fun createProgrammingSubmission(): AssignmentSubmission {
        return AssignmentSubmission(
            id = AssignmentSubmissionId.generate(),
            assignmentId = AssignmentId.generate(),
            studentId = StudentId("student_001"),
            type = AssignmentType.PROGRAMMING_EXERCISE,
            content = AssignmentContent.ProgrammingContent(
                sourceCode = """
                    fun fibonacci(n: Int): Int {
                        if (n <= 1) return n
                        return fibonacci(n - 1) + fibonacci(n - 2)
                    }
                """.trimIndent(),
                language = ProgrammingLanguage.KOTLIN,
                testCases = listOf(
                    TestCase("5", "5", "斐波那契数列测试")
                )
            ),
            submittedAt = Clock.System.now(),
            timeSpent = Duration.parse("PT30M")
        )
    }
    
    private fun createMathSubmission(): AssignmentSubmission {
        return AssignmentSubmission(
            id = AssignmentSubmissionId.generate(),
            assignmentId = AssignmentId.generate(),
            studentId = StudentId("student_001"),
            type = AssignmentType.MATH_PROBLEM_SOLVING,
            content = AssignmentContent.MathContent(
                solution = "使用二次公式求解方程",
                workingSteps = listOf(
                    "识别a=1, b=-5, c=6",
                    "应用二次公式",
                    "计算判别式",
                    "求解x值"
                ),
                finalAnswer = "x = 2 或 x = 3"
            ),
            submittedAt = Clock.System.now(),
            timeSpent = Duration.parse("PT20M")
        )
    }
    
    private fun createWritingSubmission(): AssignmentSubmission {
        return AssignmentSubmission(
            id = AssignmentSubmissionId.generate(),
            assignmentId = AssignmentId.generate(),
            studentId = StudentId("student_001"),
            type = AssignmentType.ESSAY_WRITING,
            content = AssignmentContent.WritingContent(
                text = "这是一篇关于人工智能发展的论文...",
                wordCount = 800,
                references = listOf("参考文献1", "参考文献2")
            ),
            submittedAt = Clock.System.now(),
            timeSpent = Duration.parse("PT60M")
        )
    }
    
    private fun createCreativeSubmission(): AssignmentSubmission {
        return AssignmentSubmission(
            id = AssignmentSubmissionId.generate(),
            assignmentId = AssignmentId.generate(),
            studentId = StudentId("student_001"),
            type = AssignmentType.CREATIVE_WRITING,
            content = AssignmentContent.CreativeContent(
                description = "一个关于未来世界的创意故事",
                artisticStatement = "探索科技与人性的关系",
                techniques = listOf("叙事技巧", "想象力运用")
            ),
            submittedAt = Clock.System.now(),
            timeSpent = Duration.parse("PT45M")
        )
    }
    
    private fun createGradingRequest(): GradingRequest {
        return GradingRequest(
            submissionId = AssignmentSubmissionId.generate(),
            rubric = GradingRubric(
                criteria = listOf(
                    GradingCriterion(
                        name = "代码质量",
                        description = "代码的整体质量",
                        weight = 0.3,
                        maxPoints = 25.0,
                        levels = listOf(
                            PerformanceLevel("优秀", "代码质量很高", 25.0),
                            PerformanceLevel("良好", "代码质量较好", 20.0),
                            PerformanceLevel("一般", "代码质量一般", 15.0)
                        )
                    ),
                    GradingCriterion(
                        name = "功能正确性",
                        description = "功能实现的正确性",
                        weight = 0.4,
                        maxPoints = 25.0,
                        levels = listOf(
                            PerformanceLevel("完全正确", "功能完全正确", 25.0),
                            PerformanceLevel("基本正确", "功能基本正确", 20.0),
                            PerformanceLevel("部分正确", "功能部分正确", 15.0)
                        )
                    )
                ),
                totalPoints = 100.0,
                passingScore = 60.0
            )
        )
    }
    
    private fun createGradingResult(): GradingResult {
        return GradingResult(
            id = GradingResultId.generate(),
            submissionId = AssignmentSubmissionId.generate(),
            overallScore = 85.0,
            passed = true,
            feedback = DetailedFeedback(
                summary = "整体表现良好",
                strengths = listOf("逻辑清晰", "代码规范"),
                weaknesses = listOf("缺少注释", "性能可优化"),
                improvements = listOf(
                    ImprovementSuggestion(
                        category = "代码质量",
                        description = "添加更多注释",
                        priority = Priority.MEDIUM,
                        actionItems = listOf("为关键函数添加注释")
                    )
                ),
                resources = listOf(
                    LearningResource(
                        title = "Kotlin编程指南",
                        type = ResourceType.TUTORIAL,
                        url = "https://example.com/kotlin-guide",
                        description = "学习Kotlin编程最佳实践",
                        relevance = 0.9
                    )
                )
            ),
            rubricScores = mapOf(
                "代码质量" to 20.0,
                "功能正确性" to 23.0
            ),
            gradedAt = Clock.System.now(),
            gradingTime = Duration.parse("PT2M"),
            confidence = 0.85
        )
    }
    
    private fun createStudentProfile(): ai.kastrax.edutech.grading.StudentProfile {
        return ai.kastrax.edutech.grading.StudentProfile(
            studentId = "student_001",
            learningStyle = ai.kastrax.edutech.grading.LearningStyle.VISUAL,
            skillLevels = mapOf(
                "编程" to ai.kastrax.edutech.grading.SkillLevel.INTERMEDIATE,
                "数学" to ai.kastrax.edutech.grading.SkillLevel.ADVANCED
            ),
            preferences = ai.kastrax.edutech.grading.LearningPreferences(
                preferredContentTypes = listOf(ai.kastrax.edutech.grading.ContentType.VIDEO, ai.kastrax.edutech.grading.ContentType.INTERACTIVE),
                preferredDifficulty = DifficultyLevel.INTERMEDIATE,
                learningPace = ai.kastrax.edutech.grading.LearningPace.MODERATE,
                feedbackFrequency = ai.kastrax.edutech.grading.FeedbackFrequency.FREQUENT
            ),
            weakAreas = listOf("算法优化", "代码注释"),
            strongAreas = listOf("逻辑思维", "问题分析")
        )
    }
}
