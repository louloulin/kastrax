package ai.kastrax.edutech.assessment

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.*

/**
 * 评估ID
 */
@Serializable
@JvmInline
value class AssessmentId(val value: String) {
    companion object {
        fun generate(): AssessmentId = AssessmentId("assessment_${UUID.randomUUID()}")
    }
}

/**
 * 问题ID
 */
@Serializable
@JvmInline
value class QuestionId(val value: String) {
    companion object {
        fun generate(): QuestionId = QuestionId("question_${UUID.randomUUID()}")
    }
}

/**
 * 提交ID
 */
@Serializable
@JvmInline
value class SubmissionId(val value: String) {
    companion object {
        fun generate(): SubmissionId = SubmissionId("submission_${UUID.randomUUID()}")
    }
}

/**
 * 批改结果ID
 */
@Serializable
@JvmInline
value class GradingResultId(val value: String) {
    companion object {
        fun generate(): GradingResultId = GradingResultId("grading_${UUID.randomUUID()}")
    }
}

/**
 * 问题类型
 */
enum class QuestionType {
    MULTIPLE_CHOICE,    // 选择题
    TRUE_FALSE,         // 判断题
    SHORT_ANSWER,       // 简答题
    ESSAY,              // 论文题
    FILL_IN_BLANK       // 填空题
}

/**
 * 问题
 */
@Serializable
data class Question(
    val id: QuestionId = QuestionId.generate(),
    val type: QuestionType,
    val content: String,
    val options: List<String> = emptyList(), // 选择题选项
    val correctAnswer: String? = null,
    val points: Double = 1.0,
    val explanation: String? = null,
    val tags: List<String> = emptyList()
)

/**
 * 答案
 */
@Serializable
data class Answer(
    val questionId: QuestionId,
    val content: String,
    val answeredAt: Instant = kotlinx.datetime.Clock.System.now()
)

/**
 * 评估
 */
@Serializable
data class Assessment(
    val id: AssessmentId = AssessmentId.generate(),
    val title: String,
    val description: String,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val questions: List<Question>,
    val timeLimit: Int? = null, // 分钟
    val passingScore: Double = 60.0, // 及格分数百分比
    val maxAttempts: Int = 1,
    val isPublished: Boolean = false,
    val createdBy: String,
    val createdAt: Instant = kotlinx.datetime.Clock.System.now(),
    val updatedAt: Instant = kotlinx.datetime.Clock.System.now()
)

/**
 * 评估提交
 */
@Serializable
data class AssessmentSubmission(
    val id: SubmissionId = SubmissionId.generate(),
    val assessmentId: AssessmentId,
    val studentId: StudentId,
    val answers: List<Answer>,
    val startedAt: Instant,
    val submittedAt: Instant = kotlinx.datetime.Clock.System.now(),
    val timeSpent: Int, // 秒
    val attemptNumber: Int = 1
)

/**
 * 问题评分
 */
@Serializable
data class QuestionGrade(
    val questionId: QuestionId,
    val studentAnswer: String,
    val correctAnswer: String,
    val score: Double,
    val maxScore: Double,
    val isCorrect: Boolean,
    val feedback: String
)

/**
 * 批改结果
 */
@Serializable
data class GradingResult(
    val id: GradingResultId = GradingResultId.generate(),
    val submissionId: SubmissionId,
    val assessmentId: AssessmentId,
    val studentId: StudentId,
    val questionGrades: List<QuestionGrade>,
    val totalScore: Double,
    val maxScore: Double,
    val percentage: Double,
    val passed: Boolean,
    val feedback: String,
    val gradedAt: Instant = kotlinx.datetime.Clock.System.now(),
    val gradedBy: String? = null // null表示自动批改
)

/**
 * 评估创建请求
 */
@Serializable
data class AssessmentCreationRequest(
    val title: String,
    val description: String,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val questions: List<Question>,
    val timeLimit: Int? = null,
    val passingScore: Double = 60.0,
    val maxAttempts: Int = 1,
    val createdBy: String
)

/**
 * 评估统计
 */
@Serializable
data class AssessmentStatistics(
    val assessmentId: AssessmentId,
    val totalSubmissions: Int,
    val averageScore: Double,
    val passRate: Double, // 百分比
    val scoreDistribution: Map<String, Int>, // 分数段分布
    val questionAnalysis: List<QuestionAnalysis>
)

/**
 * 问题分析
 */
@Serializable
data class QuestionAnalysis(
    val questionId: QuestionId,
    val correctRate: Double,
    val averageScore: Double,
    val commonWrongAnswers: List<String>,
    val difficulty: String // "简单", "中等", "困难"
)

/**
 * 评估报告
 */
@Serializable
data class AssessmentReport(
    val id: String,
    val assessmentId: AssessmentId,
    val studentId: StudentId? = null, // null表示整体报告
    val reportType: ReportType,
    val content: String,
    val generatedAt: Instant = kotlinx.datetime.Clock.System.now()
)

/**
 * 报告类型
 */
enum class ReportType {
    STUDENT,    // 学生个人报告
    OVERALL,    // 整体报告
    DETAILED    // 详细分析报告
}

// 结果类型定义

/**
 * 评估创建结果
 */
sealed class AssessmentCreationResult {
    data class Success(val assessment: Assessment) : AssessmentCreationResult()
    data class Failure(val error: String) : AssessmentCreationResult()
}

/**
 * 评估提交结果
 */
sealed class AssessmentSubmissionResult {
    data class Success(
        val submissionId: SubmissionId,
        val gradingResult: GradingResult,
        val submittedAt: Instant
    ) : AssessmentSubmissionResult()
    
    data class Failure(val error: String) : AssessmentSubmissionResult()
}

/**
 * 评估统计结果
 */
sealed class AssessmentStatisticsResult {
    data class Success(val statistics: AssessmentStatistics) : AssessmentStatisticsResult()
    data class Failure(val error: String) : AssessmentStatisticsResult()
}

/**
 * 评估报告结果
 */
sealed class AssessmentReportResult {
    data class Success(val report: AssessmentReport) : AssessmentReportResult()
    data class Failure(val error: String) : AssessmentReportResult()
}

/**
 * 评估仓库接口
 */
interface AssessmentRepository {
    suspend fun saveAssessment(assessment: Assessment)
    suspend fun getAssessment(assessmentId: AssessmentId): Assessment?
    suspend fun getAllAssessments(): List<Assessment>
    suspend fun getAssessmentsBySubject(subject: Subject): List<Assessment>
    suspend fun deleteAssessment(assessmentId: AssessmentId)
    
    suspend fun saveSubmission(submission: AssessmentSubmission)
    suspend fun getSubmission(submissionId: SubmissionId): AssessmentSubmission?
    suspend fun getSubmissionsByAssessment(assessmentId: AssessmentId): List<AssessmentSubmission>
    suspend fun getSubmissionsByStudent(studentId: StudentId): List<AssessmentSubmission>
    
    suspend fun saveGradingResult(result: GradingResult)
    suspend fun getGradingResult(submissionId: SubmissionId): GradingResult?
    suspend fun getGradingResultsByAssessment(assessmentId: AssessmentId): List<GradingResult>
    suspend fun getGradingResultsByStudent(studentId: StudentId): List<GradingResult>
}

/**
 * 内存评估仓库实现
 */
class InMemoryAssessmentRepository : AssessmentRepository {
    private val assessments = mutableMapOf<AssessmentId, Assessment>()
    private val submissions = mutableMapOf<SubmissionId, AssessmentSubmission>()
    private val gradingResults = mutableMapOf<SubmissionId, GradingResult>()
    
    override suspend fun saveAssessment(assessment: Assessment) {
        assessments[assessment.id] = assessment
    }
    
    override suspend fun getAssessment(assessmentId: AssessmentId): Assessment? {
        return assessments[assessmentId]
    }
    
    override suspend fun getAllAssessments(): List<Assessment> {
        return assessments.values.toList()
    }
    
    override suspend fun getAssessmentsBySubject(subject: Subject): List<Assessment> {
        return assessments.values.filter { it.subject == subject }
    }
    
    override suspend fun deleteAssessment(assessmentId: AssessmentId) {
        assessments.remove(assessmentId)
    }
    
    override suspend fun saveSubmission(submission: AssessmentSubmission) {
        submissions[submission.id] = submission
    }
    
    override suspend fun getSubmission(submissionId: SubmissionId): AssessmentSubmission? {
        return submissions[submissionId]
    }
    
    override suspend fun getSubmissionsByAssessment(assessmentId: AssessmentId): List<AssessmentSubmission> {
        return submissions.values.filter { it.assessmentId == assessmentId }
    }
    
    override suspend fun getSubmissionsByStudent(studentId: StudentId): List<AssessmentSubmission> {
        return submissions.values.filter { it.studentId == studentId }
    }
    
    override suspend fun saveGradingResult(result: GradingResult) {
        gradingResults[result.submissionId] = result
    }
    
    override suspend fun getGradingResult(submissionId: SubmissionId): GradingResult? {
        return gradingResults[submissionId]
    }
    
    override suspend fun getGradingResultsByAssessment(assessmentId: AssessmentId): List<GradingResult> {
        return gradingResults.values.filter { it.assessmentId == assessmentId }
    }
    
    override suspend fun getGradingResultsByStudent(studentId: StudentId): List<GradingResult> {
        return gradingResults.values.filter { it.studentId == studentId }
    }
}
