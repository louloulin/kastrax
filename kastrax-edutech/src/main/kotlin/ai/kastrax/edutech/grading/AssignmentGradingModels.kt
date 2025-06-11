package ai.kastrax.edutech.grading

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * 作业类型枚举
 */
@Serializable
enum class AssignmentType(val displayName: String, val category: AssignmentCategory) {
    // 编程作业
    PROGRAMMING_EXERCISE("编程练习", AssignmentCategory.PROGRAMMING),
    CODE_REVIEW("代码审查", AssignmentCategory.PROGRAMMING),
    ALGORITHM_IMPLEMENTATION("算法实现", AssignmentCategory.PROGRAMMING),
    
    // 数学作业
    MATH_PROBLEM_SOLVING("数学解题", AssignmentCategory.MATHEMATICS),
    PROOF_WRITING("证明题", AssignmentCategory.MATHEMATICS),
    CALCULATION_EXERCISE("计算练习", AssignmentCategory.MATHEMATICS),
    
    // 论文作业
    ESSAY_WRITING("论文写作", AssignmentCategory.WRITING),
    RESEARCH_PAPER("研究报告", AssignmentCategory.WRITING),
    LITERATURE_REVIEW("文献综述", AssignmentCategory.WRITING),
    
    // 创意作业
    CREATIVE_WRITING("创意写作", AssignmentCategory.CREATIVE),
    DESIGN_PROJECT("设计项目", AssignmentCategory.CREATIVE),
    MULTIMEDIA_PRESENTATION("多媒体展示", AssignmentCategory.CREATIVE)
}

@Serializable
enum class AssignmentCategory(val displayName: String) {
    PROGRAMMING("编程类"),
    MATHEMATICS("数学类"),
    WRITING("写作类"),
    CREATIVE("创意类")
}

/**
 * 作业提交
 */
@Serializable
data class AssignmentSubmission(
    val id: AssignmentSubmissionId,
    val assignmentId: AssignmentId,
    val studentId: StudentId,
    val type: AssignmentType,
    val content: AssignmentContent,
    val submittedAt: Instant,
    val timeSpent: Duration,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 作业内容
 */
@Serializable
sealed class AssignmentContent {
    @Serializable
    data class ProgrammingContent(
        val sourceCode: String,
        val language: ProgrammingLanguage,
        val testCases: List<TestCase> = emptyList(),
        val documentation: String = ""
    ) : AssignmentContent()
    
    @Serializable
    data class MathContent(
        val solution: String,
        val workingSteps: List<String> = emptyList(),
        val diagrams: List<String> = emptyList(),
        val finalAnswer: String
    ) : AssignmentContent()
    
    @Serializable
    data class WritingContent(
        val text: String,
        val wordCount: Int,
        val references: List<String> = emptyList(),
        val attachments: List<String> = emptyList()
    ) : AssignmentContent()
    
    @Serializable
    data class CreativeContent(
        val description: String,
        val mediaFiles: List<String> = emptyList(),
        val artisticStatement: String = "",
        val techniques: List<String> = emptyList()
    ) : AssignmentContent()
}

/**
 * 编程语言枚举
 */
@Serializable
enum class ProgrammingLanguage(val displayName: String, val extension: String) {
    KOTLIN("Kotlin", "kt"),
    JAVA("Java", "java"),
    PYTHON("Python", "py"),
    JAVASCRIPT("JavaScript", "js"),
    TYPESCRIPT("TypeScript", "ts"),
    CPP("C++", "cpp"),
    C("C", "c"),
    CSHARP("C#", "cs"),
    GO("Go", "go"),
    RUST("Rust", "rs")
}

/**
 * 测试用例
 */
@Serializable
data class TestCase(
    val input: String,
    val expectedOutput: String,
    val description: String = ""
)

/**
 * 作业批改结果
 */
@Serializable
data class GradingResult(
    val id: GradingResultId,
    val submissionId: AssignmentSubmissionId,
    val overallScore: Double, // 0-100
    val maxScore: Double = 100.0,
    val passed: Boolean,
    val feedback: DetailedFeedback,
    val rubricScores: Map<String, Double> = emptyMap(),
    val gradedAt: Instant,
    val gradingTime: Duration,
    val confidence: Double = 1.0, // 0-1, 批改置信度
    val needsReview: Boolean = false // 是否需要人工审核
)

/**
 * 详细反馈
 */
@Serializable
data class DetailedFeedback(
    val summary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val improvements: List<ImprovementSuggestion>,
    val resources: List<LearningResource>,
    val specificComments: List<SpecificComment> = emptyList()
)

/**
 * 改进建议
 */
@Serializable
data class ImprovementSuggestion(
    val category: String,
    val description: String,
    val priority: Priority,
    val actionItems: List<String>
)

@Serializable
enum class Priority(val displayName: String) {
    HIGH("高优先级"),
    MEDIUM("中优先级"),
    LOW("低优先级")
}

/**
 * 学习资源推荐
 */
@Serializable
data class LearningResource(
    val title: String,
    val type: ResourceType,
    val url: String,
    val description: String,
    val relevance: Double // 0-1, 相关性评分
)

@Serializable
enum class ResourceType(val displayName: String) {
    TUTORIAL("教程"),
    DOCUMENTATION("文档"),
    VIDEO("视频"),
    BOOK("书籍"),
    PRACTICE("练习"),
    EXAMPLE("示例")
}

/**
 * 具体评论
 */
@Serializable
data class SpecificComment(
    val lineNumber: Int? = null,
    val section: String? = null,
    val comment: String,
    val type: CommentType,
    val severity: Severity
)

@Serializable
enum class CommentType(val displayName: String) {
    ERROR("错误"),
    WARNING("警告"),
    SUGGESTION("建议"),
    PRAISE("表扬")
}

@Serializable
enum class Severity(val displayName: String) {
    CRITICAL("严重"),
    MAJOR("重要"),
    MINOR("轻微"),
    INFO("信息")
}

/**
 * 强类型ID
 */
@Serializable
@JvmInline
value class AssignmentId(val value: String) {
    companion object {
        fun generate(): AssignmentId = AssignmentId("assignment_${java.util.UUID.randomUUID()}")
    }
    override fun toString(): String = value
}

@Serializable
@JvmInline
value class AssignmentSubmissionId(val value: String) {
    companion object {
        fun generate(): AssignmentSubmissionId = AssignmentSubmissionId("submission_${java.util.UUID.randomUUID()}")
    }
    override fun toString(): String = value
}

@Serializable
@JvmInline
value class GradingResultId(val value: String) {
    companion object {
        fun generate(): GradingResultId = GradingResultId("grading_${java.util.UUID.randomUUID()}")
    }
    override fun toString(): String = value
}

/**
 * 批改请求
 */
@Serializable
data class GradingRequest(
    val submissionId: AssignmentSubmissionId,
    val rubric: GradingRubric,
    val options: GradingOptions = GradingOptions()
)

/**
 * 评分标准
 */
@Serializable
data class GradingRubric(
    val criteria: List<GradingCriterion>,
    val totalPoints: Double = 100.0,
    val passingScore: Double = 60.0
)

/**
 * 评分标准项
 */
@Serializable
data class GradingCriterion(
    val name: String,
    val description: String,
    val weight: Double, // 权重 0-1
    val maxPoints: Double,
    val levels: List<PerformanceLevel>
)

/**
 * 表现水平
 */
@Serializable
data class PerformanceLevel(
    val name: String,
    val description: String,
    val points: Double
)

/**
 * 批改选项
 */
@Serializable
data class GradingOptions(
    val enableDetailedFeedback: Boolean = true,
    val enableResourceRecommendation: Boolean = true,
    val enableCodeExecution: Boolean = true, // 对编程作业
    val strictMode: Boolean = false,
    val customInstructions: String = ""
)

/**
 * 批改结果类型
 */
sealed class AssignmentGradingResult {
    data class Success(val result: GradingResult) : AssignmentGradingResult()
    data class Failure(val error: String) : AssignmentGradingResult()
    data class NeedsReview(val partialResult: GradingResult, val reason: String) : AssignmentGradingResult()
}

/**
 * 质量评估结果
 */
@Serializable
data class QualityAssessment(
    val accuracy: Double, // 准确性 0-1
    val consistency: Double, // 一致性 0-1
    val completeness: Double, // 完整性 0-1
    val fairness: Double, // 公平性 0-1
    val overallQuality: Double, // 总体质量 0-1
    val issues: List<QualityIssue> = emptyList()
)

@Serializable
data class QualityIssue(
    val type: String,
    val description: String,
    val severity: Severity,
    val recommendation: String
)
