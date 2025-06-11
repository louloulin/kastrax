package ai.kastrax.edutech.analytics

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * 学习分析请求
 */
@Serializable
data class LearningAnalysisRequest(
    val timeRange: TimeRange,
    val analysisTypes: List<AnalysisType>,
    val predictionHorizon: Duration,
    val includeRealTime: Boolean = false,
    val detailLevel: DetailLevel = DetailLevel.STANDARD
)

/**
 * 学习分析结果
 */
@Serializable
sealed class LearningAnalysisResult {
    @Serializable
    data class Success(
        val studentId: StudentId,
        val analysisTimestamp: Instant,
        val learningPatterns: LearningPatterns,
        val predictions: LearningPredictions,
        val riskAssessment: RiskAssessmentResult,
        val interventions: List<InterventionRecommendation>,
        val analyticsReport: AnalyticsReport
    ) : LearningAnalysisResult()
    
    @Serializable
    data class Failure(
        val studentId: StudentId,
        val error: String,
        val timestamp: Instant
    ) : LearningAnalysisResult()
}

/**
 * 学习模式
 */
@Serializable
data class LearningPatterns(
    val studentId: StudentId,
    val identifiedPatterns: List<LearningPattern>,
    val patternStrength: Double,
    val patternConsistency: Double,
    val temporalPatterns: List<TemporalPattern>,
    val behavioralPatterns: List<BehavioralPattern>,
    val performancePatterns: List<PerformancePattern>
)

/**
 * 学习模式类型
 */
@Serializable
data class LearningPattern(
    val id: String,
    val type: PatternType,
    val description: String,
    val frequency: Double,
    val strength: Double,
    val confidence: Double,
    val firstObserved: Instant,
    val lastObserved: Instant,
    val associatedOutcomes: List<String>
)

/**
 * 时间模式
 */
@Serializable
data class TemporalPattern(
    val patternId: String,
    val timeOfDay: List<Int>, // 小时
    val daysOfWeek: List<Int>, // 1-7
    val sessionDuration: Duration,
    val frequency: Double,
    val effectiveness: Double
)

/**
 * 行为模式
 */
@Serializable
data class BehavioralPattern(
    val patternId: String,
    val behaviorType: BehaviorType,
    val description: String,
    val frequency: Double,
    val impact: BehaviorImpact,
    val triggers: List<String>,
    val outcomes: List<String>
)

/**
 * 表现模式
 */
@Serializable
data class PerformancePattern(
    val patternId: String,
    val performanceMetric: String,
    val trend: PerformanceTrend,
    val cyclicity: Double,
    val volatility: Double,
    val predictability: Double
)

/**
 * 学习预测
 */
@Serializable
data class LearningPredictions(
    val studentId: StudentId,
    val predictionHorizon: Duration,
    val expectedGrade: Double,
    val completionProbability: Double,
    val masteryPredictions: Map<String, Double>,
    val riskPredictions: List<RiskPrediction>,
    val recommendedActions: List<String>,
    val confidence: Double,
    val generatedAt: Instant
)

/**
 * 风险预测
 */
@Serializable
data class RiskPrediction(
    val riskType: RiskType,
    val probability: Double,
    val severity: RiskSeverity,
    val timeframe: Duration,
    val mitigationStrategies: List<String>
)

/**
 * 风险评估结果
 */
@Serializable
data class RiskAssessmentResult(
    val studentId: StudentId,
    val assessmentTimestamp: Instant,
    val overallRiskLevel: RiskLevel,
    val identifiedRisks: List<IdentifiedRisk>,
    val riskFactors: List<RiskFactor>,
    val mitigationRecommendations: List<MitigationRecommendation>
)

/**
 * 识别的风险
 */
@Serializable
data class IdentifiedRisk(
    val riskId: String,
    val type: RiskType,
    val severity: RiskSeverity,
    val probability: Double,
    val description: String,
    val potentialImpact: String,
    val timeframe: Duration,
    val confidence: Double
)

/**
 * 风险因素
 */
@Serializable
data class RiskFactor(
    val factorId: String,
    val name: String,
    val category: RiskCategory,
    val weight: Double,
    val currentValue: Double,
    val thresholdValue: Double,
    val trend: Trend
)

/**
 * 缓解建议
 */
@Serializable
data class MitigationRecommendation(
    val recommendationId: String,
    val targetRisk: String,
    val strategy: String,
    val actions: List<String>,
    val priority: Priority,
    val estimatedEffectiveness: Double,
    val implementationTime: Duration
)

/**
 * 干预建议
 */
@Serializable
data class InterventionRecommendation(
    val interventionId: String,
    val type: InterventionType,
    val description: String,
    val targetArea: String,
    val urgency: Urgency,
    val expectedOutcome: String,
    val implementationSteps: List<String>,
    val successMetrics: List<String>,
    val timeframe: Duration
)

/**
 * 实时分析
 */
@Serializable
data class RealTimeAnalysis(
    val studentId: StudentId,
    val sessionId: String,
    val timestamp: Instant,
    val currentPatterns: List<RealTimePattern>,
    val immediateRisks: List<ImmediateRisk>,
    val realTimeInterventions: List<RealTimeIntervention>,
    val alertLevel: AlertLevel
)

/**
 * 实时模式
 */
@Serializable
data class RealTimePattern(
    val patternType: String,
    val strength: Double,
    val deviation: Double,
    val significance: Double
)

/**
 * 即时风险
 */
@Serializable
data class ImmediateRisk(
    val riskType: String,
    val severity: RiskSeverity,
    val probability: Double,
    val description: String,
    val recommendedAction: String
)

/**
 * 实时干预
 */
@Serializable
data class RealTimeIntervention(
    val interventionType: String,
    val message: String,
    val action: String,
    val priority: Priority
)

/**
 * 学习洞察
 */
@Serializable
data class LearningInsights(
    val studentId: StudentId,
    val timeRange: TimeRange,
    val keyFindings: List<String>,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val behaviorPatterns: List<String>,
    val riskFactors: List<String>,
    val recommendations: List<String>,
    val confidence: Double,
    val generatedAt: Instant
)

/**
 * 学习成果预测
 */
@Serializable
data class LearningOutcomePrediction(
    val studentId: StudentId,
    val courseId: CourseId,
    val predictionHorizon: Duration,
    val predictedGrade: Double,
    val completionProbability: Double,
    val riskFactors: List<String>,
    val recommendedActions: List<String>,
    val confidence: Double,
    val generatedAt: Instant
)

/**
 * 分析报告
 */
@Serializable
data class AnalyticsReport(
    val executiveSummary: String,
    val keyFindings: List<String>,
    val riskAnalysis: String,
    val actionPlan: List<String>,
    val dataQuality: DataQuality,
    val recommendations: List<String>,
    val nextSteps: List<String>
)

/**
 * 批量分析结果
 */
@Serializable
data class BatchAnalysisResult(
    val totalStudents: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: List<LearningAnalysisResult>,
    val batchSummary: BatchSummary
)

/**
 * 批量摘要
 */
@Serializable
data class BatchSummary(
    val totalAnalyzed: Int,
    val successfulAnalyses: Int,
    val averageRiskLevel: Double,
    val commonPatterns: List<String>,
    val aggregateInsights: List<String>,
    val recommendedSystemActions: List<String>
)

/**
 * 历史学习数据
 */
@Serializable
data class HistoricalLearningData(
    val studentId: StudentId,
    val courseId: CourseId,
    val pastGrades: List<Double>,
    val engagementMetrics: EngagementMetrics,
    val learningVelocity: Double,
    val difficultyProgression: List<Double>
)

/**
 * 参与度指标
 */
@Serializable
data class EngagementMetrics(
    val averageSessionDuration: Duration,
    val completionRate: Double,
    val interactionFrequency: Double
)

/**
 * 当前表现
 */
@Serializable
data class CurrentPerformance(
    val studentId: StudentId,
    val courseId: CourseId,
    val currentGrade: Double,
    val recentTrend: PerformanceTrend,
    val engagementLevel: EngagementLevel,
    val masteryLevel: Double,
    val lastActivity: Instant
)

/**
 * 数据质量
 */
@Serializable
data class DataQuality(
    val completeness: Double,
    val accuracy: Double,
    val timeliness: Double
)

/**
 * 时间范围
 */
@Serializable
data class TimeRange(
    val start: Instant,
    val end: Instant
)

// 枚举类型

@Serializable
enum class AnalysisType {
    PATTERN_RECOGNITION,
    PREDICTIVE_ANALYSIS,
    RISK_ASSESSMENT,
    INTERVENTION_PLANNING,
    REAL_TIME_MONITORING
}

@Serializable
enum class DetailLevel {
    BASIC,
    STANDARD,
    DETAILED,
    COMPREHENSIVE
}

@Serializable
enum class PatternType {
    TEMPORAL,
    BEHAVIORAL,
    PERFORMANCE,
    ENGAGEMENT,
    LEARNING_STYLE,
    CONTENT_PREFERENCE
}

@Serializable
enum class BehaviorType {
    STUDY_HABITS,
    INTERACTION_PATTERNS,
    CONTENT_CONSUMPTION,
    ASSESSMENT_APPROACH,
    HELP_SEEKING,
    COLLABORATION
}

@Serializable
enum class BehaviorImpact {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
    MIXED
}

@Serializable
enum class PerformanceTrend {
    IMPROVING,
    DECLINING,
    STABLE,
    VOLATILE,
    CYCLICAL
}

@Serializable
enum class RiskType {
    ACADEMIC_FAILURE,
    DISENGAGEMENT,
    KNOWLEDGE_GAP,
    MOTIVATION_LOSS,
    SKILL_DEFICIENCY,
    TIME_MANAGEMENT,
    OVERLOAD
}

@Serializable
enum class RiskSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
enum class RiskLevel {
    MINIMAL,
    LOW,
    MODERATE,
    HIGH,
    CRITICAL
}

@Serializable
enum class RiskCategory {
    ACADEMIC,
    BEHAVIORAL,
    MOTIVATIONAL,
    TECHNICAL,
    ENVIRONMENTAL
}

@Serializable
enum class Trend {
    INCREASING,
    DECREASING,
    STABLE,
    FLUCTUATING
}

@Serializable
enum class InterventionType {
    CONTENT_ADJUSTMENT,
    PACING_MODIFICATION,
    SUPPORT_PROVISION,
    MOTIVATION_BOOST,
    SKILL_BUILDING,
    FEEDBACK_ENHANCEMENT
}

@Serializable
enum class Urgency {
    LOW,
    MEDIUM,
    HIGH,
    IMMEDIATE
}

@Serializable
enum class AlertLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
enum class EngagementLevel {
    VERY_LOW,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

@Serializable
enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
