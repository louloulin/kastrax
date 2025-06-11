package ai.kastrax.edutech.grading

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * 批改质量保证系统
 * 提供人工审核机制、质量评估指标、持续改进流程和准确性监控
 */
class QualityAssuranceSystem(
    private val humanReviewService: HumanReviewService,
    private val qualityMetricsCollector: QualityMetricsCollector,
    private val continuousImprovementEngine: ContinuousImprovementEngine
) {
    
    /**
     * 质量保证流程
     */
    suspend fun performQualityAssurance(
        gradingResult: GradingResult,
        submission: AssignmentSubmission
    ): QualityAssuranceResult {
        
        // 1. 自动质量检查
        val automaticQualityCheck = performAutomaticQualityCheck(gradingResult, submission)
        
        // 2. 决定是否需要人工审核
        val needsHumanReview = determineHumanReviewNeed(automaticQualityCheck, gradingResult)
        
        // 3. 人工审核（如果需要）
        val humanReviewResult = if (needsHumanReview) {
            humanReviewService.requestReview(gradingResult, submission, automaticQualityCheck)
        } else null
        
        // 4. 最终质量评估
        val finalQualityAssessment = calculateFinalQualityAssessment(
            automaticQualityCheck, humanReviewResult
        )
        
        // 5. 记录质量指标
        qualityMetricsCollector.recordQualityMetrics(
            gradingResult, submission, finalQualityAssessment
        )
        
        // 6. 触发持续改进
        continuousImprovementEngine.analyzeForImprovement(
            gradingResult, submission, finalQualityAssessment
        )
        
        return QualityAssuranceResult(
            originalResult = gradingResult,
            qualityAssessment = finalQualityAssessment,
            humanReviewResult = humanReviewResult,
            approved = finalQualityAssessment.overallQuality >= 0.8,
            recommendations = generateQualityRecommendations(finalQualityAssessment)
        )
    }
    
    /**
     * 自动质量检查
     */
    private fun performAutomaticQualityCheck(
        gradingResult: GradingResult,
        submission: AssignmentSubmission
    ): AutomaticQualityCheck {
        
        val checks = mutableListOf<QualityCheckResult>()
        
        // 1. 分数合理性检查
        checks.add(checkScoreReasonableness(gradingResult))
        
        // 2. 反馈完整性检查
        checks.add(checkFeedbackCompleteness(gradingResult.feedback))
        
        // 3. 一致性检查
        checks.add(checkConsistency(gradingResult))
        
        // 4. 置信度检查
        checks.add(checkConfidence(gradingResult))
        
        // 5. 时间合理性检查
        checks.add(checkGradingTime(gradingResult, submission))
        
        // 6. 内容相关性检查
        checks.add(checkContentRelevance(gradingResult, submission))
        
        val overallScore = checks.map { it.score }.average()
        val criticalIssues = checks.filter { it.severity == Severity.CRITICAL }
        val majorIssues = checks.filter { it.severity == Severity.MAJOR }
        
        return AutomaticQualityCheck(
            checks = checks,
            overallScore = overallScore,
            criticalIssues = criticalIssues.size,
            majorIssues = majorIssues.size,
            passed = overallScore >= 0.7 && criticalIssues.isEmpty()
        )
    }
    
    /**
     * 分数合理性检查
     */
    private fun checkScoreReasonableness(gradingResult: GradingResult): QualityCheckResult {
        val score = gradingResult.overallScore
        val maxScore = gradingResult.maxScore
        
        val issues = mutableListOf<String>()
        var severity = Severity.INFO
        var checkScore = 1.0
        
        // 检查分数范围
        if (score < 0 || score > maxScore) {
            issues.add("分数超出有效范围")
            severity = Severity.CRITICAL
            checkScore = 0.0
        }
        
        // 检查分数分布合理性
        val rubricScores = gradingResult.rubricScores.values
        if (rubricScores.isNotEmpty()) {
            val totalRubricScore = rubricScores.sum()
            val scoreDifference = kotlin.math.abs(score - totalRubricScore)
            
            if (scoreDifference > maxScore * 0.1) { // 差异超过10%
                issues.add("总分与各项分数之和差异过大")
                severity = maxOf(severity, Severity.MAJOR)
                checkScore = kotlin.math.min(checkScore, 0.6)
            }
        }
        
        return QualityCheckResult(
            checkName = "分数合理性",
            passed = issues.isEmpty(),
            score = checkScore,
            severity = severity,
            issues = issues,
            recommendations = if (issues.isNotEmpty()) listOf("重新计算分数") else emptyList()
        )
    }
    
    /**
     * 反馈完整性检查
     */
    private fun checkFeedbackCompleteness(feedback: DetailedFeedback): QualityCheckResult {
        val issues = mutableListOf<String>()
        var score = 1.0
        
        if (feedback.summary.isBlank()) {
            issues.add("缺少总结")
            score -= 0.3
        }
        
        if (feedback.strengths.isEmpty()) {
            issues.add("缺少优势分析")
            score -= 0.2
        }
        
        if (feedback.weaknesses.isEmpty()) {
            issues.add("缺少不足分析")
            score -= 0.2
        }
        
        if (feedback.improvements.isEmpty()) {
            issues.add("缺少改进建议")
            score -= 0.3
        }
        
        val severity = when {
            score < 0.5 -> Severity.MAJOR
            score < 0.8 -> Severity.MINOR
            else -> Severity.INFO
        }
        
        return QualityCheckResult(
            checkName = "反馈完整性",
            passed = issues.isEmpty(),
            score = kotlin.math.max(0.0, score),
            severity = severity,
            issues = issues,
            recommendations = issues.map { "补充$it" }
        )
    }
    
    /**
     * 一致性检查
     */
    private fun checkConsistency(gradingResult: GradingResult): QualityCheckResult {
        val issues = mutableListOf<String>()
        var score = 1.0
        
        // 检查分数与反馈的一致性
        val overallScore = gradingResult.overallScore
        val passed = gradingResult.passed
        
        // 高分但未通过，或低分但通过
        if ((overallScore >= 60 && !passed) || (overallScore < 60 && passed)) {
            issues.add("分数与通过状态不一致")
            score -= 0.4
        }
        
        // 检查反馈语调与分数的一致性
        val hasPositiveFeedback = gradingResult.feedback.strengths.isNotEmpty()
        val hasNegativeFeedback = gradingResult.feedback.weaknesses.isNotEmpty()
        
        if (overallScore >= 80 && !hasPositiveFeedback) {
            issues.add("高分作业缺少正面反馈")
            score -= 0.2
        }
        
        if (overallScore <= 40 && !hasNegativeFeedback) {
            issues.add("低分作业缺少改进建议")
            score -= 0.3
        }
        
        val severity = when {
            score < 0.6 -> Severity.MAJOR
            score < 0.8 -> Severity.MINOR
            else -> Severity.INFO
        }
        
        return QualityCheckResult(
            checkName = "一致性",
            passed = issues.isEmpty(),
            score = kotlin.math.max(0.0, score),
            severity = severity,
            issues = issues,
            recommendations = issues.map { "检查并修正$it" }
        )
    }
    
    /**
     * 置信度检查
     */
    private fun checkConfidence(gradingResult: GradingResult): QualityCheckResult {
        val confidence = gradingResult.confidence
        val issues = mutableListOf<String>()
        
        val severity = when {
            confidence < 0.5 -> Severity.CRITICAL
            confidence < 0.7 -> Severity.MAJOR
            confidence < 0.8 -> Severity.MINOR
            else -> Severity.INFO
        }
        
        if (confidence < 0.7) {
            issues.add("批改置信度较低 (${"%.2f".format(confidence)})")
        }
        
        return QualityCheckResult(
            checkName = "置信度",
            passed = confidence >= 0.7,
            score = confidence,
            severity = severity,
            issues = issues,
            recommendations = if (confidence < 0.7) listOf("建议人工审核") else emptyList()
        )
    }
    
    /**
     * 批改时间检查
     */
    private fun checkGradingTime(
        gradingResult: GradingResult,
        submission: AssignmentSubmission
    ): QualityCheckResult {
        val gradingTime = gradingResult.gradingTime
        val issues = mutableListOf<String>()
        var score = 1.0
        
        // 根据作业类型设定合理的批改时间范围
        val expectedTimeRange = when (submission.type.category) {
            AssignmentCategory.PROGRAMMING -> 30L..300L // 30秒到5分钟
            AssignmentCategory.MATHEMATICS -> 15L..180L // 15秒到3分钟
            AssignmentCategory.WRITING -> 60L..600L // 1分钟到10分钟
            AssignmentCategory.CREATIVE -> 45L..450L // 45秒到7.5分钟
        }
        
        val gradingSeconds = gradingTime.inWholeSeconds
        
        if (gradingSeconds < expectedTimeRange.first) {
            issues.add("批改时间过短，可能不够仔细")
            score -= 0.3
        } else if (gradingSeconds > expectedTimeRange.last) {
            issues.add("批改时间过长，可能存在问题")
            score -= 0.2
        }
        
        val severity = if (issues.isNotEmpty()) Severity.MINOR else Severity.INFO
        
        return QualityCheckResult(
            checkName = "批改时间",
            passed = issues.isEmpty(),
            score = kotlin.math.max(0.0, score),
            severity = severity,
            issues = issues,
            recommendations = if (issues.isNotEmpty()) listOf("检查批改流程") else emptyList()
        )
    }
    
    /**
     * 内容相关性检查
     */
    private fun checkContentRelevance(
        gradingResult: GradingResult,
        submission: AssignmentSubmission
    ): QualityCheckResult {
        // 简化实现，实际应该使用更复杂的相关性分析
        val feedback = gradingResult.feedback
        val issues = mutableListOf<String>()
        var score = 1.0
        
        // 检查反馈是否包含与作业类型相关的关键词
        val relevantKeywords = when (submission.type.category) {
            AssignmentCategory.PROGRAMMING -> listOf("代码", "算法", "函数", "变量", "语法")
            AssignmentCategory.MATHEMATICS -> listOf("计算", "公式", "解题", "步骤", "答案")
            AssignmentCategory.WRITING -> listOf("内容", "结构", "语言", "逻辑", "表达")
            AssignmentCategory.CREATIVE -> listOf("创意", "表现", "技法", "概念", "艺术")
        }
        
        val feedbackText = "${feedback.summary} ${feedback.strengths.joinToString()} ${feedback.weaknesses.joinToString()}"
        val keywordMatches = relevantKeywords.count { keyword ->
            feedbackText.contains(keyword, ignoreCase = true)
        }
        
        if (keywordMatches < relevantKeywords.size / 2) {
            issues.add("反馈内容与作业类型相关性较低")
            score -= 0.4
        }
        
        val severity = if (issues.isNotEmpty()) Severity.MINOR else Severity.INFO
        
        return QualityCheckResult(
            checkName = "内容相关性",
            passed = issues.isEmpty(),
            score = kotlin.math.max(0.0, score),
            severity = severity,
            issues = issues,
            recommendations = if (issues.isNotEmpty()) listOf("增强反馈的针对性") else emptyList()
        )
    }
    
    /**
     * 判断是否需要人工审核
     */
    private fun determineHumanReviewNeed(
        qualityCheck: AutomaticQualityCheck,
        gradingResult: GradingResult
    ): Boolean {
        return when {
            qualityCheck.criticalIssues > 0 -> true
            qualityCheck.majorIssues >= 2 -> true
            qualityCheck.overallScore < 0.6 -> true
            gradingResult.confidence < 0.5 -> true
            gradingResult.needsReview -> true
            else -> false
        }
    }
    
    /**
     * 计算最终质量评估
     */
    private fun calculateFinalQualityAssessment(
        automaticCheck: AutomaticQualityCheck,
        humanReview: HumanReviewResult?
    ): QualityAssessment {
        
        val baseAccuracy = automaticCheck.overallScore
        val baseConsistency = if (automaticCheck.criticalIssues == 0) 0.9 else 0.5
        val baseCompleteness = automaticCheck.checks
            .find { it.checkName == "反馈完整性" }?.score ?: 0.8
        val baseFairness = 0.8 // 默认公平性
        
        // 如果有人工审核，调整评估结果
        val finalAccuracy = humanReview?.let { review ->
            if (review.approved) kotlin.math.max(baseAccuracy, 0.8) else kotlin.math.min(baseAccuracy, 0.6)
        } ?: baseAccuracy
        
        val finalConsistency = humanReview?.let { review ->
            if (review.hasRevisions) 0.7 else baseConsistency
        } ?: baseConsistency
        
        val overallQuality = (finalAccuracy + finalConsistency + baseCompleteness + baseFairness) / 4
        
        val issues = automaticCheck.checks
            .filter { !it.passed }
            .map { check ->
                QualityIssue(
                    type = check.checkName,
                    description = check.issues.joinToString(", "),
                    severity = check.severity,
                    recommendation = check.recommendations.joinToString(", ")
                )
            }
        
        return QualityAssessment(
            accuracy = finalAccuracy,
            consistency = finalConsistency,
            completeness = baseCompleteness,
            fairness = baseFairness,
            overallQuality = overallQuality,
            issues = issues
        )
    }
    
    /**
     * 生成质量改进建议
     */
    private fun generateQualityRecommendations(
        qualityAssessment: QualityAssessment
    ): List<QualityRecommendation> {
        val recommendations = mutableListOf<QualityRecommendation>()
        
        if (qualityAssessment.accuracy < 0.8) {
            recommendations.add(
                QualityRecommendation(
                    category = "准确性改进",
                    description = "提高批改准确性",
                    actions = listOf("优化评分算法", "增加训练数据", "改进提示词"),
                    priority = Priority.HIGH
                )
            )
        }
        
        if (qualityAssessment.consistency < 0.8) {
            recommendations.add(
                QualityRecommendation(
                    category = "一致性改进",
                    description = "提高评分一致性",
                    actions = listOf("标准化评分流程", "增加质量检查点"),
                    priority = Priority.MEDIUM
                )
            )
        }
        
        if (qualityAssessment.completeness < 0.8) {
            recommendations.add(
                QualityRecommendation(
                    category = "完整性改进",
                    description = "完善反馈内容",
                    actions = listOf("增加反馈模板", "强化内容检查"),
                    priority = Priority.MEDIUM
                )
            )
        }
        
        return recommendations
    }
}

/**
 * 质量保证结果
 */
@Serializable
data class QualityAssuranceResult(
    val originalResult: GradingResult,
    val qualityAssessment: QualityAssessment,
    val humanReviewResult: HumanReviewResult?,
    val approved: Boolean,
    val recommendations: List<QualityRecommendation>
)

/**
 * 自动质量检查结果
 */
@Serializable
data class AutomaticQualityCheck(
    val checks: List<QualityCheckResult>,
    val overallScore: Double,
    val criticalIssues: Int,
    val majorIssues: Int,
    val passed: Boolean
)

/**
 * 质量检查结果
 */
@Serializable
data class QualityCheckResult(
    val checkName: String,
    val passed: Boolean,
    val score: Double,
    val severity: Severity,
    val issues: List<String>,
    val recommendations: List<String>
)

/**
 * 人工审核结果
 */
@Serializable
data class HumanReviewResult(
    val reviewId: String,
    val reviewerId: String,
    val reviewedAt: Instant,
    val approved: Boolean,
    val hasRevisions: Boolean,
    val revisedScore: Double?,
    val reviewComments: String,
    val timeSpent: Duration
)

/**
 * 质量改进建议
 */
@Serializable
data class QualityRecommendation(
    val category: String,
    val description: String,
    val actions: List<String>,
    val priority: Priority
)

/**
 * 人工审核服务接口
 */
interface HumanReviewService {
    suspend fun requestReview(
        gradingResult: GradingResult,
        submission: AssignmentSubmission,
        qualityCheck: AutomaticQualityCheck
    ): HumanReviewResult
}

/**
 * 质量指标收集器接口
 */
interface QualityMetricsCollector {
    suspend fun recordQualityMetrics(
        gradingResult: GradingResult,
        submission: AssignmentSubmission,
        qualityAssessment: QualityAssessment
    )
}

/**
 * 持续改进引擎接口
 */
interface ContinuousImprovementEngine {
    suspend fun analyzeForImprovement(
        gradingResult: GradingResult,
        submission: AssignmentSubmission,
        qualityAssessment: QualityAssessment
    )
}

/**
 * 简单的人工审核服务实现
 */
class SimpleHumanReviewService : HumanReviewService {
    override suspend fun requestReview(
        gradingResult: GradingResult,
        submission: AssignmentSubmission,
        qualityCheck: AutomaticQualityCheck
    ): HumanReviewResult {
        // 模拟人工审核过程
        val approved = qualityCheck.overallScore > 0.6
        val hasRevisions = !approved
        val revisedScore = if (hasRevisions) gradingResult.overallScore * 1.1 else null

        return HumanReviewResult(
            reviewId = "review_${java.util.UUID.randomUUID()}",
            reviewerId = "reviewer_001",
            reviewedAt = Clock.System.now(),
            approved = approved,
            hasRevisions = hasRevisions,
            revisedScore = revisedScore,
            reviewComments = if (approved) "审核通过" else "需要改进批改质量",
            timeSpent = kotlin.time.Duration.parse("PT10M")
        )
    }
}

/**
 * 简单的质量指标收集器实现
 */
class SimpleQualityMetricsCollector : QualityMetricsCollector {
    private val metrics = mutableListOf<QualityMetricRecord>()

    override suspend fun recordQualityMetrics(
        gradingResult: GradingResult,
        submission: AssignmentSubmission,
        qualityAssessment: QualityAssessment
    ) {
        val record = QualityMetricRecord(
            timestamp = Clock.System.now(),
            submissionType = submission.type,
            gradingScore = gradingResult.overallScore,
            qualityScore = qualityAssessment.overallQuality,
            confidence = gradingResult.confidence,
            gradingTime = gradingResult.gradingTime
        )
        metrics.add(record)
    }

    fun getMetrics(): List<QualityMetricRecord> = metrics.toList()
}

/**
 * 质量指标记录
 */
@Serializable
data class QualityMetricRecord(
    val timestamp: Instant,
    val submissionType: AssignmentType,
    val gradingScore: Double,
    val qualityScore: Double,
    val confidence: Double,
    val gradingTime: Duration
)

/**
 * 简单的持续改进引擎实现
 */
class SimpleContinuousImprovementEngine : ContinuousImprovementEngine {
    private val improvementSuggestions = mutableListOf<ImprovementSuggestion>()

    override suspend fun analyzeForImprovement(
        gradingResult: GradingResult,
        submission: AssignmentSubmission,
        qualityAssessment: QualityAssessment
    ) {
        // 分析并生成改进建议
        if (qualityAssessment.accuracy < 0.7) {
            improvementSuggestions.add(
                ImprovementSuggestion(
                    category = "准确性",
                    description = "批改准确性需要提升",
                    priority = Priority.HIGH,
                    actionItems = listOf("优化算法", "增加训练数据")
                )
            )
        }

        if (gradingResult.confidence < 0.6) {
            improvementSuggestions.add(
                ImprovementSuggestion(
                    category = "置信度",
                    description = "批改置信度较低",
                    priority = Priority.MEDIUM,
                    actionItems = listOf("改进模型", "增加验证机制")
                )
            )
        }
    }

    fun getImprovementSuggestions(): List<ImprovementSuggestion> = improvementSuggestions.toList()
}
