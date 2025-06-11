package ai.kastrax.edutech.grading

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * 代码分析结果
 */
@Serializable
data class CodeAnalysis(
    val codeQuality: Double, // 代码质量 0-1
    val functionality: Double, // 功能正确性 0-1
    val style: Double, // 代码风格 0-1
    val performance: Double, // 性能考虑 0-1
    val suggestions: List<String>, // 改进建议
    val confidence: Double // 分析置信度 0-1
)

/**
 * 数学分析结果
 */
@Serializable
data class MathAnalysis(
    val methodCorrectness: Double, // 方法正确性 0-1
    val calculationAccuracy: Double, // 计算准确性 0-1
    val stepCompleteness: Double, // 步骤完整性 0-1
    val presentation: Double, // 表达规范性 0-1
    val suggestions: List<String>, // 改进建议
    val confidence: Double // 分析置信度 0-1
)

/**
 * 写作分析结果
 */
@Serializable
data class WritingAnalysis(
    val contentQuality: Double, // 内容质量 0-1
    val structure: Double, // 结构组织 0-1
    val language: Double, // 语言表达 0-1
    val logic: Double, // 逻辑性 0-1
    val suggestions: List<String>, // 改进建议
    val confidence: Double // 分析置信度 0-1
)

/**
 * 创意分析结果
 */
@Serializable
data class CreativeAnalysis(
    val creativity: Double, // 创意性 0-1
    val technique: Double, // 技术执行 0-1
    val expression: Double, // 艺术表现 0-1
    val concept: Double, // 概念深度 0-1
    val suggestions: List<String>, // 改进建议
    val confidence: Double // 分析置信度 0-1
)

/**
 * 代码执行结果
 */
@Serializable
data class ExecutionResult(
    val testCase: TestCase,
    val actualOutput: String,
    val passed: Boolean,
    val executionTime: Long, // 毫秒
    val memoryUsage: Long = 0, // 字节
    val error: String? = null
)

/**
 * 代码执行器接口
 */
interface CodeExecutor {
    suspend fun executeCode(
        sourceCode: String,
        language: ProgrammingLanguage,
        testCases: List<TestCase>
    ): List<ExecutionResult>
}

/**
 * 简单的代码执行器实现
 */
class SimpleCodeExecutor : CodeExecutor {
    override suspend fun executeCode(
        sourceCode: String,
        language: ProgrammingLanguage,
        testCases: List<TestCase>
    ): List<ExecutionResult> {
        // 简化实现，实际应该根据语言类型执行代码
        return testCases.map { testCase ->
            // 模拟执行结果
            val passed = when (language) {
                ProgrammingLanguage.PYTHON -> simulatePythonExecution(sourceCode, testCase)
                ProgrammingLanguage.JAVA -> simulateJavaExecution(sourceCode, testCase)
                ProgrammingLanguage.KOTLIN -> simulateKotlinExecution(sourceCode, testCase)
                else -> true // 其他语言默认通过
            }
            
            ExecutionResult(
                testCase = testCase,
                actualOutput = if (passed) testCase.expectedOutput else "错误输出",
                passed = passed,
                executionTime = (50..200)Random.nextInt().toLong(),
                memoryUsage = (1024..4096)Random.nextInt().toLong()
            )
        }
    }
    
    private fun simulatePythonExecution(sourceCode: String, testCase: TestCase): Boolean {
        // 简单的Python代码检查
        return sourceCode.contains("def ") && sourceCode.contains("return")
    }
    
    private fun simulateJavaExecution(sourceCode: String, testCase: TestCase): Boolean {
        // 简单的Java代码检查
        return sourceCode.contains("public ") && sourceCode.contains("class")
    }
    
    private fun simulateKotlinExecution(sourceCode: String, testCase: TestCase): Boolean {
        // 简单的Kotlin代码检查
        return sourceCode.contains("fun ") || sourceCode.contains("class")
    }
}

/**
 * 质量保证服务接口
 */
interface QualityAssuranceService {
    suspend fun assessGradingQuality(
        result: GradingResult,
        submission: AssignmentSubmission
    ): QualityAssessment
}

/**
 * 简单的质量保证服务实现
 */
class SimpleQualityAssuranceService : QualityAssuranceService {
    override suspend fun assessGradingQuality(
        result: GradingResult,
        submission: AssignmentSubmission
    ): QualityAssessment {
        val accuracy = assessAccuracy(result, submission)
        val consistency = assessConsistency(result)
        val completeness = assessCompleteness(result)
        val fairness = assessFairness(result, submission)
        
        val overallQuality = (accuracy + consistency + completeness + fairness) / 4
        
        val issues = mutableListOf<QualityIssue>()
        
        if (accuracy < 0.7) {
            issues.add(
                QualityIssue(
                    type = "准确性",
                    description = "批改准确性较低",
                    severity = Severity.MAJOR,
                    recommendation = "需要人工审核"
                )
            )
        }
        
        if (consistency < 0.7) {
            issues.add(
                QualityIssue(
                    type = "一致性",
                    description = "评分标准应用不一致",
                    severity = Severity.MINOR,
                    recommendation = "检查评分标准"
                )
            )
        }
        
        return QualityAssessment(
            accuracy = accuracy,
            consistency = consistency,
            completeness = completeness,
            fairness = fairness,
            overallQuality = overallQuality,
            issues = issues
        )
    }
    
    private fun assessAccuracy(result: GradingResult, submission: AssignmentSubmission): Double {
        // 基于置信度和分数合理性评估准确性
        val confidenceScore = result.confidence
        val scoreReasonableness = if (result.overallScore in 0.0..100.0) 1.0 else 0.5
        
        return (confidenceScore + scoreReasonableness) / 2
    }
    
    private fun assessConsistency(result: GradingResult): Double {
        // 评估各项评分的一致性
        val scores = result.rubricScores.values
        if (scores.isEmpty()) return 1.0
        
        val mean = scores.average()
        val variance = scores.map { (it - mean) * (it - mean) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        
        // 标准差越小，一致性越高
        return kotlin.math.max(0.0, 1.0 - standardDeviation / mean)
    }
    
    private fun assessCompleteness(result: GradingResult): Double {
        // 评估反馈的完整性
        val feedback = result.feedback
        var completenessScore = 0.0
        
        if (feedback.summary.isNotBlank()) completenessScore += 0.3
        if (feedback.strengths.isNotEmpty()) completenessScore += 0.2
        if (feedback.weaknesses.isNotEmpty()) completenessScore += 0.2
        if (feedback.improvements.isNotEmpty()) completenessScore += 0.2
        if (feedback.resources.isNotEmpty()) completenessScore += 0.1
        
        return completenessScore
    }
    
    private fun assessFairness(result: GradingResult, submission: AssignmentSubmission): Double {
        // 评估评分的公平性
        // 简化实现，实际应该考虑更多因素
        val timeSpentFactor = when {
            submission.timeSpent.inWholeMinutes < 5 -> 0.7 // 时间过短可能不够认真
            submission.timeSpent.inWholeMinutes > 180 -> 0.9 // 时间充足
            else -> 0.8
        }
        
        val scoreFactor = when {
            result.overallScore < 20 -> 0.8 // 极低分需要仔细检查
            result.overallScore > 95 -> 0.9 // 高分相对安全
            else -> 1.0
        }
        
        return (timeSpentFactor + scoreFactor) / 2
    }
}

/**
 * 批改统计信息
 */
@Serializable
data class GradingStatistics(
    val totalSubmissions: Int,
    val averageScore: Double,
    val passRate: Double,
    val averageGradingTime: Duration,
    val qualityMetrics: QualityMetrics
)

/**
 * 质量指标
 */
@Serializable
data class QualityMetrics(
    val averageAccuracy: Double,
    val averageConsistency: Double,
    val averageCompleteness: Double,
    val averageFairness: Double,
    val humanReviewRate: Double // 需要人工审核的比例
)

/**
 * 批改历史记录
 */
@Serializable
data class GradingHistory(
    val submissionId: AssignmentSubmissionId,
    val gradingResults: List<GradingResult>,
    val qualityAssessments: List<QualityAssessment>,
    val humanReviews: List<HumanReview> = emptyList()
)

/**
 * 人工审核记录
 */
@Serializable
data class HumanReview(
    val reviewerId: String,
    val reviewedAt: Instant,
    val originalResult: GradingResult,
    val revisedResult: GradingResult?,
    val reviewComments: String,
    val approved: Boolean
)
