package ai.kastrax.edutech.adaptive

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 自适应学习引擎数据模型
 * Week 19-20 扩展功能
 */

// 基础数据类型
@Serializable
data class PerformanceRecord(
    val activityId: String,
    val accuracy: Double,
    val completionTime: Int,
    val timestamp: Instant,
    val difficulty: DifficultyLevel
)

@Serializable
data class LearningActivity(
    val id: String,
    val title: String,
    val type: String,
    val difficulty: DifficultyLevel,
    val estimatedTime: Int
)

@Serializable
data class LearningSession(
    val id: String,
    val studentId: StudentId,
    val startTime: Instant,
    val activities: List<LearningActivity>,
    val performance: SessionPerformance
)

@Serializable
data class SessionPerformance(
    val overallScore: Double = 0.0,
    val completionRate: Double = 0.0,
    val engagementLevel: Double = 0.0
)

@Serializable
data class LearningPlan(
    val id: String,
    val studentId: StudentId,
    val activities: List<LearningActivity>,
    val estimatedDuration: Int,
    val objectives: List<String>
)

@Serializable
data class HistoricalLearningData(
    val studentId: StudentId,
    val performanceHistory: List<PerformanceRecord>,
    val sessionHistory: List<LearningSession>,
    val learningPatterns: List<String>
)

// 能力评估相关
@Serializable
data class LearningCapabilityAssessment(
    val studentId: StudentId,
    val assessmentTime: Instant,
    val currentPerformance: CurrentPerformance,
    val cognitiveLoad: CognitiveLoad,
    val learningPattern: LearningPattern,
    val capabilityMetrics: CapabilityMetrics,
    val recommendations: List<String>
)

@Serializable
data class CurrentPerformance(
    val accuracy: Double,
    val speed: Int,
    val consistency: Double,
    val improvement: Double
)

@Serializable
data class CognitiveLoad(
    val intrinsicLoad: Double,
    val extraneousLoad: Double,
    val germaneLoad: Double,
    val totalLoad: Double
)

@Serializable
data class LearningPattern(
    val preferredDifficulty: DifficultyLevel,
    val optimalSessionLength: Int,
    val peakPerformanceTime: String,
    val learningStyle: LearningStyle
)

@Serializable
data class CapabilityMetrics(
    val overallCapability: Double,
    val processingSpeed: Double,
    val workingMemory: Double,
    val attentionSpan: Double,
    val adaptability: Double
)

// 难度调整相关
@Serializable
data class RealtimePerformance(
    val currentAccuracy: Double,
    val currentSpeed: Double,
    val frustrationLevel: Double,
    val confidenceLevel: Double
)

@Serializable
data class DifficultyFit(
    val currentFit: Double,
    val optimalRange: ClosedFloatingPointRange<Double>,
    val adjustmentNeeded: Boolean
)

@Serializable
data class DifficultyAdjustment(
    val studentId: StudentId,
    val originalDifficulty: DifficultyLevel,
    val adjustedDifficulty: DifficultyLevel,
    val adjustmentReason: String,
    val adjustmentTime: Instant,
    val expectedImpact: String
)

@Serializable
data class AdjustmentStrategy(
    val type: AdjustmentType,
    val reason: String,
    val expectedImpact: String
)

@Serializable
enum class AdjustmentType {
    INCREASE, DECREASE, MAINTAIN, ADAPTIVE
}

// 学习节奏控制相关
@Serializable
data class PacePreferences(
    val preferredIntensity: PaceIntensity,
    val maxSessionLength: Int,
    val preferredBreakFrequency: Int,
    val adaptiveAdjustment: Boolean
)

@Serializable
data class LearningPace(
    val activitiesPerHour: Double,
    val averageTimePerActivity: Double,
    val breakFrequency: Int,
    val intensity: PaceIntensity
)

@Serializable
enum class PaceIntensity {
    LOW, MODERATE, HIGH, ADAPTIVE
}

@Serializable
data class PaceEffectiveness(
    val effectiveness: Double,
    val sustainability: Double,
    val engagement: Double
)

@Serializable
data class LearningPaceControl(
    val studentId: StudentId,
    val sessionId: String,
    val currentPace: LearningPace,
    val optimalPace: LearningPace,
    val adjustments: List<PaceAdjustment>,
    val controlTime: Instant,
    val effectiveness: PaceEffectiveness
)

@Serializable
data class PaceAdjustment(
    val type: String,
    val adjustment: String,
    val reason: String
)

// 学习干预相关
@Serializable
data class InterventionTrigger(
    val type: TriggerType,
    val threshold: Double,
    val currentValue: Double,
    val triggerTime: Instant
)

@Serializable
enum class TriggerType {
    PERFORMANCE_DROP, FRUSTRATION_HIGH, ENGAGEMENT_LOW, TIME_EXCEEDED
}

@Serializable
data class InterventionContext(
    val currentActivity: LearningActivity,
    val sessionProgress: Double,
    val studentState: StudentState,
    val environmentFactors: Map<String, String>
)

@Serializable
data class StudentState(
    val energy: Double,
    val motivation: Double,
    val focus: Double,
    val stress: Double
)

@Serializable
data class InterventionNeed(
    val urgency: InterventionUrgency,
    val type: InterventionType,
    val scope: InterventionScope
)

@Serializable
enum class InterventionUrgency {
    LOW, MEDIUM, HIGH, CRITICAL
}

@Serializable
enum class InterventionType {
    MOTIVATIONAL, INSTRUCTIONAL, TECHNICAL, EMOTIONAL
}

@Serializable
enum class InterventionScope {
    INDIVIDUAL, GROUP, SYSTEM
}

@Serializable
data class InterventionStrategy(
    val approach: InterventionApproach,
    val timing: InterventionTiming,
    val duration: InterventionDuration
)

@Serializable
enum class InterventionApproach {
    SUPPORTIVE, CORRECTIVE, PREVENTIVE, ADAPTIVE
}

@Serializable
enum class InterventionTiming {
    IMMEDIATE, DELAYED, SCHEDULED, ADAPTIVE
}

@Serializable
enum class InterventionDuration {
    SHORT, MEDIUM, LONG, ONGOING
}

@Serializable
data class InterventionContent(
    val message: String,
    val actionItems: List<String>,
    val resources: List<String>
)

@Serializable
data class LearningIntervention(
    val studentId: StudentId,
    val strategy: InterventionStrategy,
    val content: InterventionContent,
    val executionTime: Instant,
    val status: InterventionStatus
)

@Serializable
enum class InterventionStatus {
    PLANNED, EXECUTED, COMPLETED, CANCELLED
}

// 学习效果优化相关
@Serializable
data class OutcomePrediction(
    val successProbability: Double,
    val expectedCompletionTime: Int,
    val predictedPerformance: Double,
    val confidenceLevel: Double
)

@Serializable
data class OptimizationOpportunity(
    val area: String,
    val description: String,
    val potentialGain: Double
)

@Serializable
data class OptimizationStrategy(
    val name: String,
    val description: String,
    val expectedBenefit: Double
)

@Serializable
data class LearningOptimization(
    val studentId: StudentId,
    val originalPlan: LearningPlan,
    val optimizedPlan: LearningPlan,
    val prediction: OutcomePrediction,
    val optimizationStrategies: List<OptimizationStrategy>,
    val optimizationTime: Instant,
    val expectedImprovement: Double
)

// 结果类型
sealed class CapabilityAssessmentResult {
    data class Success(val assessment: LearningCapabilityAssessment, val message: String) : CapabilityAssessmentResult()
    data class Failure(val error: String) : CapabilityAssessmentResult()
}

sealed class DifficultyAdjustmentResult {
    data class Success(val adjustment: DifficultyAdjustment, val adjustedActivity: LearningActivity, val message: String) : DifficultyAdjustmentResult()
    data class Failure(val error: String) : DifficultyAdjustmentResult()
}

sealed class PaceControlResult {
    data class Success(val control: LearningPaceControl, val message: String) : PaceControlResult()
    data class Failure(val error: String) : PaceControlResult()
}

sealed class InterventionResult {
    data class Success(val intervention: LearningIntervention, val message: String) : InterventionResult()
    data class Failure(val error: String) : InterventionResult()
}

sealed class OptimizationResult {
    data class Success(val optimization: LearningOptimization, val message: String) : OptimizationResult()
    data class Failure(val error: String) : OptimizationResult()
}

// 自适应学习服务相关数据类型
@Serializable
data class AdaptivePreferences(
    val difficultyAdaptation: Boolean = true,
    val paceAdaptation: Boolean = true,
    val interventionEnabled: Boolean = true,
    val personalizedFeedback: Boolean = true
)

@Serializable
data class AdaptiveSession(
    val sessionId: String,
    val studentId: StudentId,
    val learningPlan: LearningPlan,
    val adaptiveConfig: AdaptiveConfiguration,
    val initialAssessment: LearningCapabilityAssessment,
    val startTime: Instant,
    val status: SessionStatus
)

@Serializable
enum class SessionStatus {
    ACTIVE, PAUSED, COMPLETED, CANCELLED
}

@Serializable
data class AdaptiveConfiguration(
    val difficultyAdaptation: Boolean,
    val paceAdaptation: Boolean,
    val interventionEnabled: Boolean,
    val optimizationEnabled: Boolean
)

@Serializable
data class RealtimeAdaptationData(
    val studentId: StudentId,
    val currentSession: LearningSession,
    val performanceData: RealtimePerformance,
    val paceData: PaceData,
    val pacePreferences: PacePreferences,
    val interventionContext: InterventionContext
)

@Serializable
data class PaceData(
    val currentPace: Double = 0.0,
    val optimalPace: Double = 0.0,
    val paceEfficiency: Double = 0.0
)

@Serializable
data class Adaptation(
    val type: AdaptationType,
    val description: String,
    val impact: String
)

@Serializable
enum class AdaptationType {
    DIFFICULTY, PACE, INTERVENTION, CONTENT, SEQUENCE
}

@Serializable
data class RealtimeAdaptation(
    val sessionId: String,
    val adaptationTime: Instant,
    val adaptations: List<Adaptation>,
    val triggerData: RealtimeAdaptationData,
    val effectiveness: Double
)

@Serializable
enum class AdaptiveReportType {
    SESSION_SUMMARY, ADAPTATION_ANALYSIS, LEARNING_PROGRESS, COMPREHENSIVE
}

@Serializable
data class AdaptiveReportData(
    val studentId: StudentId,
    val sessionId: String,
    val adaptations: List<RealtimeAdaptation>,
    val performance: List<PerformanceRecord>
)

@Serializable
data class AdaptiveLearningReport(
    val type: AdaptiveReportType,
    val studentId: StudentId,
    val sessionId: String,
    val generatedAt: Instant,
    val summary: String,
    val sections: List<ReportSection>
)

@Serializable
data class ReportSection(
    val title: String,
    val content: String,
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class AdaptationInsights(
    val totalAdaptations: Int,
    val mostFrequentType: AdaptationType,
    val averageEffectiveness: Double,
    val trends: List<String>
)

@Serializable
data class PathOptimizationOpportunity(
    val area: String,
    val description: String,
    val priority: String,
    val expectedBenefit: Double
)

@Serializable
data class PathOptimizationRecommendation(
    val type: String,
    val description: String,
    val implementation: String,
    val expectedImpact: Double
)

@Serializable
data class PathOptimization(
    val studentId: StudentId,
    val originalPlan: LearningPlan,
    val optimizedPlan: LearningPlan,
    val optimizationRecommendations: List<PathOptimizationRecommendation>,
    val adaptationInsights: AdaptationInsights,
    val optimizationTime: Instant,
    val expectedImprovement: Double
)

// 结果类型扩展
sealed class AdaptiveSessionResult {
    data class Success(val session: AdaptiveSession, val message: String) : AdaptiveSessionResult()
    data class Failure(val error: String) : AdaptiveSessionResult()
}

sealed class AdaptationResult {
    data class Success(val adaptation: RealtimeAdaptation, val message: String) : AdaptationResult()
    data class Failure(val error: String) : AdaptationResult()
}

sealed class AdaptiveReportResult {
    data class Success(val report: AdaptiveLearningReport, val message: String) : AdaptiveReportResult()
    data class Failure(val error: String) : AdaptiveReportResult()
}

sealed class PathOptimizationResult {
    data class Success(val optimization: PathOptimization, val message: String) : PathOptimizationResult()
    data class Failure(val error: String) : PathOptimizationResult()
}
