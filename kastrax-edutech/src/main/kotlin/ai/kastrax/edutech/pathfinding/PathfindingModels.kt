package ai.kastrax.edutech.pathfinding

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 智能学习路径相关数据模型
 * Week 17-18 高级扩展功能
 */

/**
 * 学习路径
 */
@Serializable
data class LearningPath(
    val id: String,
    val studentId: StudentId,
    val goals: List<LearningGoal>,
    val steps: List<LearningStep>,
    val estimatedDuration: Int, // 分钟
    val difficulty: DifficultyLevel,
    val createdAt: Instant,
    val lastUpdated: Instant,
    val status: LearningPathStatus,
    val milestones: List<LearningMilestone>,
    val adaptiveSettings: AdaptiveSettings
)

/**
 * 学习目标
 */
@Serializable
data class LearningGoal(
    val id: String,
    val title: String,
    val description: String,
    val subject: Subject,
    val requiredSkills: List<String>,
    val estimatedHours: Int,
    val priority: GoalPriority,
    val deadline: Instant? = null
)

/**
 * 目标优先级
 */
@Serializable
enum class GoalPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * 学习步骤
 */
@Serializable
data class LearningStep(
    val id: String,
    val title: String,
    val description: String,
    val type: StepType,
    val resourceId: String,
    val estimatedTime: Int, // 分钟
    val difficulty: DifficultyLevel,
    val prerequisites: List<String>,
    val learningObjectives: List<String>,
    val assessmentCriteria: List<String>
)

/**
 * 步骤类型
 */
@Serializable
enum class StepType {
    READING,        // 阅读
    VIDEO,          // 视频
    EXERCISE,       // 练习
    QUIZ,           // 测验
    PROJECT,        // 项目
    DISCUSSION,     // 讨论
    REFLECTION,     // 反思
    PRACTICE       // 实践
}

/**
 * 学习路径状态
 */
@Serializable
enum class LearningPathStatus {
    DRAFT,          // 草稿
    ACTIVE,         // 活跃
    PAUSED,         // 暂停
    COMPLETED,      // 完成
    ABANDONED       // 放弃
}

/**
 * 学习里程碑
 */
@Serializable
data class LearningMilestone(
    val id: String,
    val title: String,
    val description: String,
    val targetDate: Instant,
    val completionCriteria: List<String>,
    val rewardPoints: Int,
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null
)

/**
 * 自适应设置
 */
@Serializable
data class AdaptiveSettings(
    val difficultyAdjustment: Boolean = true,
    val paceAdjustment: Boolean = true,
    val contentRecommendation: Boolean = true,
    val automaticReview: Boolean = true,
    val personalizedFeedback: Boolean = true
)

/**
 * 学习约束条件
 */
@Serializable
data class LearningConstraints(
    val maxDailyStudyTime: Int, // 分钟
    val preferredStudyTimes: List<TimeSlot>,
    val availableDays: List<DayOfWeek>,
    val deadline: Instant? = null,
    val budgetConstraints: BudgetConstraints? = null,
    val accessibilityRequirements: List<String> = emptyList()
)

/**
 * 时间段
 */
@Serializable
data class TimeSlot(
    val startHour: Int,
    val endHour: Int,
    val dayOfWeek: DayOfWeek? = null
)

/**
 * 星期枚举
 */
@Serializable
enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

/**
 * 预算约束
 */
@Serializable
data class BudgetConstraints(
    val maxCost: Double,
    val currency: String = "CNY",
    val includesFreeResourcesOnly: Boolean = false
)

/**
 * 分析后的目标
 */
@Serializable
data class AnalyzedGoal(
    val original: LearningGoal,
    val complexity: ComplexityLevel,
    val prerequisites: List<String>,
    val estimatedEffort: Int,
    val subGoals: List<LearningSubGoal>
)

/**
 * 复杂度级别
 */
@Serializable
enum class ComplexityLevel {
    LOW, MEDIUM, HIGH, VERY_HIGH
}

/**
 * 学习子目标
 */
@Serializable
data class LearningSubGoal(
    val id: String,
    val description: String,
    val estimatedHours: Int,
    val priority: SubGoalPriority
)

/**
 * 子目标优先级
 */
@Serializable
enum class SubGoalPriority {
    LOW, MEDIUM, HIGH
}

/**
 * 知识评估
 */
@Serializable
data class KnowledgeAssessment(
    val knowledgeMap: Map<String, Double>,
    val skillLevels: Map<String, Double>,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val confidenceScores: Map<String, Double>
)

/**
 * 学习资源
 */
@Serializable
data class LearningResource(
    val id: String,
    val title: String,
    val type: String,
    val difficulty: DifficultyLevel,
    val estimatedTime: Int,
    val relevanceScore: Double
)

/**
 * 学习进度
 */
@Serializable
data class LearningProgress(
    val pathId: String,
    val studentId: StudentId,
    val completedSteps: List<String>,
    val currentStepId: String?,
    val overallProgress: Double, // 0.0 - 1.0
    val timeSpent: Int, // 分钟
    val lastActivity: Instant
)

/**
 * 表现数据
 */
@Serializable
data class PerformanceData(
    val studentId: StudentId,
    val pathId: String,
    val scores: List<Double>,
    val completionTimes: List<Int>,
    val difficultyRatings: List<DifficultyLevel>,
    val engagementMetrics: EngagementMetrics
)

/**
 * 参与度指标
 */
@Serializable
data class EngagementMetrics(
    val sessionDuration: Int,
    val interactionCount: Int,
    val pauseFrequency: Int,
    val helpRequestCount: Int,
    val satisfactionRating: Double
)

/**
 * 活动表现
 */
@Serializable
data class ActivityPerformance(
    val activityId: String,
    val score: Double,
    val timeSpent: Int,
    val attempts: Int,
    val completedAt: Instant,
    val difficultyRating: DifficultyLevel
)

/**
 * 路径位置
 */
@Serializable
data class PathPosition(
    val stepIndex: Int,
    val progressPercentage: Double
)

/**
 * 准备度评估
 */
@Serializable
data class ReadinessAssessment(
    val isReady: Boolean,
    val confidenceLevel: Double,
    val recommendedPreparation: List<String> = emptyList()
)

/**
 * 学习活动
 */
@Serializable
data class LearningActivity(
    val id: String,
    val title: String,
    val type: ActivityType,
    val difficulty: DifficultyLevel,
    val estimatedTime: Int
)

/**
 * 活动类型
 */
@Serializable
enum class ActivityType {
    READING, VIDEO, EXERCISE, QUIZ, PROJECT, DISCUSSION, PRACTICE, REVIEW
}

/**
 * 排序后的活动
 */
@Serializable
data class RankedActivity(
    val activity: LearningActivity,
    val relevanceScore: Double,
    val difficultyMatch: Double,
    val timeMatch: Double
)

/**
 * 活动推荐
 */
@Serializable
data class ActivityRecommendation(
    val activityId: String,
    val title: String,
    val reason: String,
    val confidence: Double
)

/**
 * 成功预测
 */
@Serializable
data class SuccessPrediction(
    val probability: Double,
    val confidenceLevel: Double,
    val riskFactors: List<String>,
    val recommendations: List<String>
)

// 结果类型定义

/**
 * 学习路径结果
 */
sealed class LearningPathResult {
    data class Success(val path: LearningPath, val message: String) : LearningPathResult()
    data class Failure(val error: String) : LearningPathResult()
}

/**
 * 路径调整结果
 */
sealed class PathAdjustmentResult {
    data class Success(
        val adjustedPath: LearningPath,
        val adjustments: List<PathAdjustment>,
        val message: String
    ) : PathAdjustmentResult()
    data class Failure(val error: String) : PathAdjustmentResult()
}

/**
 * 活动推荐结果
 */
sealed class ActivityRecommendationResult {
    data class Success(val recommendation: ActivityRecommendation, val message: String) : ActivityRecommendationResult()
    data class Failure(val error: String) : ActivityRecommendationResult()
}

/**
 * 成功预测结果
 */
sealed class SuccessPredictionResult {
    data class Success(val prediction: SuccessPrediction, val message: String) : SuccessPredictionResult()
    data class Failure(val error: String) : SuccessPredictionResult()
}

// 辅助数据类

/**
 * 表现分析
 */
@Serializable
data class PerformanceAnalysis(
    val averageScore: Double = 0.0,
    val improvementTrend: Double = 0.0,
    val strugglingAreas: List<String> = emptyList(),
    val strongAreas: List<String> = emptyList()
)

/**
 * 调整需求
 */
@Serializable
data class AdjustmentNeed(
    val type: AdjustmentType,
    val severity: AdjustmentSeverity,
    val description: String
)

/**
 * 调整类型
 */
@Serializable
enum class AdjustmentType {
    DIFFICULTY, PACE, CONTENT, SEQUENCE, SUPPORT
}

/**
 * 调整严重程度
 */
@Serializable
enum class AdjustmentSeverity {
    MINOR, MODERATE, MAJOR, CRITICAL
}

/**
 * 路径调整
 */
@Serializable
data class PathAdjustment(
    val type: AdjustmentType,
    val description: String,
    val impact: String,
    val appliedAt: Instant
)

/**
 * 历史数据
 */
@Serializable
data class HistoricalData(
    val completedPaths: Int = 0,
    val averageSuccessRate: Double = 0.0,
    val preferredLearningStyles: List<LearningStyle> = emptyList(),
    val commonStruggles: List<String> = emptyList()
)
