package ai.kastrax.edutech.actors

import ai.kastrax.edutech.models.*
import kotlinx.serialization.Serializable

/**
 * 基础消息接口
 */
interface Message

/**
 * 教育科技Actor系统消息定义
 * 
 * 实现ed2.md第2.1节Actor模型的消息传递机制
 */

// ============= 学习会话管理消息 =============

/**
 * 开始学习会话
 */
@Serializable
data class StartLearningSession(
    val courseId: CourseId,
    val objectives: List<String>,
    val initialContext: Map<String, String> = emptyMap()
) : Message

/**
 * 会话开始确认
 */
@Serializable
data class SessionStarted(
    val sessionId: SessionId,
    val initialRecommendations: List<LearningRecommendation>
) : Message

/**
 * 暂停学习会话
 */
@Serializable
data class PauseLearningSession(
    val sessionId: SessionId,
    val reason: String? = null
) : Message

/**
 * 会话暂停确认
 */
@Serializable
data class SessionPaused(
    val sessionId: SessionId
) : Message

/**
 * 恢复学习会话
 */
@Serializable
data class ResumeLearningSession(
    val sessionId: SessionId
) : Message

/**
 * 会话恢复确认
 */
@Serializable
data class SessionResumed(
    val sessionId: SessionId
) : Message

/**
 * 完成学习会话
 */
@Serializable
data class CompleteLearningSession(
    val sessionId: SessionId,
    val summary: String? = null
) : Message

/**
 * 会话完成确认
 */
@Serializable
data class SessionCompleted(
    val sessionId: SessionId,
    val finalMetrics: SessionMetrics
) : Message

// ============= 学习活动处理消息 =============

/**
 * 处理学习活动
 */
@Serializable
data class ProcessLearningActivity(
    val sessionId: SessionId,
    val activity: LearningActivity,
    val context: ActivityContext = ActivityContext.empty()
) : Message

/**
 * 活动处理完成
 */
@Serializable
data class ActivityProcessed(
    val activityId: ActivityId,
    val performance: Double,
    val feedback: LearningFeedback,
    val nextRecommendation: LearningRecommendation?,
    val updatedMetrics: SessionMetrics
) : Message

/**
 * 活动上下文
 */
@Serializable
data class ActivityContext(
    val previousActivities: List<ActivityId> = emptyList(),
    val timeConstraints: Long? = null, // 时间限制（分钟）
    val assistanceLevel: AssistanceLevel = AssistanceLevel.NORMAL,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        fun empty(): ActivityContext = ActivityContext()
    }
}

@Serializable
enum class AssistanceLevel {
    MINIMAL,    // 最少帮助
    NORMAL,     // 正常帮助
    ENHANCED    // 增强帮助
}

// ============= 个性化和推荐消息 =============

/**
 * 更新个性化设置
 */
@Serializable
data class UpdatePersonalization(
    val profileUpdates: Map<String, String>,
    val preferences: LearningPreferences
) : Message

/**
 * 个性化更新确认
 */
@Serializable
data class PersonalizationUpdated(
    val planSummary: String
) : Message

/**
 * 获取学习推荐
 */
@Serializable
data class GetRecommendations(
    val context: RecommendationContext
) : Message

/**
 * 推荐生成完成
 */
@Serializable
data class RecommendationsGenerated(
    val recommendations: List<LearningRecommendation>
) : Message

/**
 * 推荐上下文
 */
@Serializable
data class RecommendationContext(
    val subject: Subject? = null,
    val difficulty: DifficultyLevel? = null,
    val contentTypes: Set<ContentType> = emptySet(),
    val timeAvailable: Long? = null, // 可用时间（分钟）
    val specificTopics: List<String> = emptyList(),
    val learningGoals: List<String> = emptyList()
)

// ============= 进度查询消息 =============

/**
 * 获取学习进度
 */
@Serializable
data class GetLearningProgress(
    val timeRange: ProgressTimeRange? = null,
    val subjects: Set<Subject> = emptySet(),
    val includeDetails: Boolean = true
) : Message

/**
 * 进度时间范围
 */
@Serializable
data class ProgressTimeRange(
    val startTime: kotlinx.datetime.Instant,
    val endTime: kotlinx.datetime.Instant
)

// ============= 学习目标管理消息 =============

/**
 * 更新学习目标
 */
@Serializable
data class UpdateLearningGoals(
    val newGoals: List<LearningGoal>
) : Message

/**
 * 目标更新确认
 */
@Serializable
data class GoalsUpdated(
    val goals: List<LearningGoal>
) : Message

/**
 * 学习目标
 */
@Serializable
data class LearningGoal(
    val id: String,
    val title: String,
    val description: String,
    val subject: Subject,
    val targetDate: kotlinx.datetime.Instant?,
    val priority: GoalPriority,
    val measurableOutcomes: List<String>,
    val currentProgress: Double = 0.0 // 0-100
)

@Serializable
enum class GoalPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

// ============= 元认知记录消息 =============

/**
 * 记录元认知反思
 */
@Serializable
data class RecordMetacognition(
    val reflection: MetacognitiveReflection
) : Message

/**
 * 元认知记录确认
 */
@Serializable
data class MetacognitionRecorded(
    val reflectionId: String
) : Message

/**
 * 元认知反思
 */
@Serializable
data class MetacognitiveReflection(
    val id: String,
    val timestamp: kotlinx.datetime.Instant,
    val activityId: ActivityId?,
    val reflectionType: ReflectionType,
    val content: String,
    val selfAssessment: SelfAssessment,
    val strategiesUsed: List<String>,
    val strategiesEffectiveness: Map<String, Int>, // 1-5 评分
    val futureStrategies: List<String>
)

@Serializable
enum class ReflectionType {
    BEFORE_LEARNING,    // 学习前反思
    DURING_LEARNING,    // 学习中反思
    AFTER_LEARNING,     // 学习后反思
    STRATEGY_EVALUATION // 策略评估
}

@Serializable
data class SelfAssessment(
    val understanding: Int,     // 理解程度 1-5
    val confidence: Int,        // 信心程度 1-5
    val effort: Int,           // 努力程度 1-5
    val satisfaction: Int,      // 满意度 1-5
    val difficulty: Int        // 感知难度 1-5
)

// ============= 数据模型支持类 =============

/**
 * 学习推荐
 */
@Serializable
data class LearningRecommendation(
    val id: String,
    val type: RecommendationType,
    val title: String,
    val description: String,
    val contentId: String?,
    val estimatedDuration: Long, // 分钟
    val difficulty: DifficultyLevel,
    val relevanceScore: Double, // 0-1
    val personalizedReason: String,
    val prerequisites: List<String> = emptyList(),
    val learningOutcomes: List<String> = emptyList()
)

@Serializable
enum class RecommendationType {
    CONTENT,        // 内容推荐
    ACTIVITY,       // 活动推荐
    PRACTICE,       // 练习推荐
    REVIEW,         // 复习推荐
    CHALLENGE,      // 挑战推荐
    REMEDIATION     // 补救推荐
}

/**
 * 学习反馈
 */
@Serializable
data class LearningFeedback(
    val activityId: ActivityId,
    val overallScore: Double,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val suggestions: List<String>,
    val encouragement: String,
    val nextSteps: List<String>
)

/**
 * 学习偏好
 */
@Serializable
data class LearningPreferences(
    val preferredDifficulty: DifficultyLevel,
    val preferredContentTypes: Set<ContentType>,
    val preferredSessionDuration: Long, // 分钟
    val preferredTimeOfDay: TimeOfDay?,
    val feedbackFrequency: FeedbackFrequency,
    val challengeLevel: ChallengeLevel
)

@Serializable
enum class TimeOfDay {
    MORNING, AFTERNOON, EVENING, NIGHT
}

@Serializable
enum class FeedbackFrequency {
    IMMEDIATE,  // 立即反馈
    PERIODIC,   // 定期反馈
    ON_DEMAND   // 按需反馈
}

@Serializable
enum class ChallengeLevel {
    COMFORT_ZONE,   // 舒适区
    STRETCH,        // 拉伸区
    CHALLENGE       // 挑战区
}

/**
 * 学习进度报告
 */
@Serializable
data class LearningProgressReport(
    val studentId: StudentId,
    val currentSession: LearningSession?,
    val overallProgress: OverallProgress,
    val subjectProgress: Map<Subject, SubjectProgress>,
    val skillDevelopment: Map<Skill, SkillProgress>,
    val recentActivities: List<LearningActivity>,
    val achievements: List<Achievement>,
    val areasForImprovement: List<ImprovementArea>,
    val nextMilestones: List<Milestone>
) : Message

@Serializable
data class OverallProgress(
    val completionPercentage: Double,
    val averagePerformance: Double,
    val totalTimeSpent: kotlin.time.Duration,
    val activitiesCompleted: Int,
    val currentStreak: Int, // 连续学习天数
    val level: Int,
    val experiencePoints: Int
)

@Serializable
data class SubjectProgress(
    val subject: Subject,
    val completionPercentage: Double,
    val averagePerformance: Double,
    val timeSpent: kotlin.time.Duration,
    val topicsCompleted: Int,
    val totalTopics: Int,
    val currentDifficulty: DifficultyLevel
)

@Serializable
data class SkillProgress(
    val skill: Skill,
    val currentLevel: Int,
    val progressToNextLevel: Double,
    val practiceCount: Int,
    val averagePerformance: Double,
    val recentImprovement: Double
)

@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val earnedAt: kotlinx.datetime.Instant,
    val points: Int
)

@Serializable
enum class AchievementCategory {
    PERFORMANCE,    // 表现成就
    CONSISTENCY,    // 坚持成就
    IMPROVEMENT,    // 进步成就
    EXPLORATION,    // 探索成就
    COLLABORATION   // 协作成就
}

@Serializable
data class ImprovementArea(
    val area: String,
    val description: String,
    val priority: Priority,
    val suggestedActions: List<String>,
    val estimatedTimeToImprove: Long // 天数
)

@Serializable
enum class Priority {
    LOW, MEDIUM, HIGH, URGENT
}

@Serializable
data class Milestone(
    val id: String,
    val title: String,
    val description: String,
    val targetDate: kotlinx.datetime.Instant,
    val progress: Double, // 0-100
    val requirements: List<String>
)

// ============= 教师Actor消息 =============

/**
 * 班级管理消息
 */
@Serializable
data class ManageClass(
    val classId: String,
    val action: ClassAction,
    val parameters: Map<String, String> = emptyMap()
) : Message

@Serializable
enum class ClassAction {
    ADD_STUDENT,
    REMOVE_STUDENT,
    UPDATE_CURRICULUM,
    GENERATE_REPORT,
    SCHEDULE_ASSESSMENT,
    BROADCAST_MESSAGE
}

/**
 * 内容生成请求
 */
@Serializable
data class GenerateContent(
    val contentType: ContentType,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val learningObjectives: List<String>,
    val targetAudience: String,
    val constraints: ContentConstraints = ContentConstraints()
) : Message

@Serializable
data class ContentConstraints(
    val maxDuration: Long? = null, // 分钟
    val requiredFormat: String? = null,
    val includedTopics: List<String> = emptyList(),
    val excludedTopics: List<String> = emptyList(),
    val accessibilityRequirements: List<String> = emptyList()
)

/**
 * 班级进度分析请求
 */
@Serializable
data class AnalyzeClassProgress(
    val classId: String,
    val analysisType: AnalysisType,
    val timeRange: ProgressTimeRange? = null
) : Message

@Serializable
enum class AnalysisType {
    OVERALL_PERFORMANCE,
    INDIVIDUAL_PROGRESS,
    SUBJECT_ANALYSIS,
    SKILL_DEVELOPMENT,
    ENGAGEMENT_METRICS,
    PREDICTIVE_ANALYSIS
}

// ============= 响应消息 =============

/**
 * 班级操作完成
 */
@Serializable
data class ClassActionCompleted(
    val action: ClassAction,
    val result: String
) : Message

/**
 * 内容生成完成
 */
@Serializable
data class ContentGenerated(
    val content: GeneratedContent
) : Message

/**
 * 进度分析完成
 */
@Serializable
data class ProgressAnalysisCompleted(
    val analysisType: AnalysisType,
    val results: AnalysisResult,
    val improvements: List<ClassImprovement>,
    val timestamp: kotlinx.datetime.Instant
) : Message

/**
 * 学生注册
 */
@Serializable
data class RegisterStudent(
    val studentId: StudentId
) : Message

/**
 * 学生注册完成
 */
@Serializable
data class StudentRegistered(
    val studentId: StudentId
) : Message

/**
 * 学生注销
 */
@Serializable
data class UnregisterStudent(
    val studentId: StudentId
) : Message

/**
 * 学生注销完成
 */
@Serializable
data class StudentUnregistered(
    val studentId: StudentId
) : Message

/**
 * 班级广播
 */
@Serializable
data class BroadcastToClass(
    val message: String
) : Message

/**
 * 消息广播完成
 */
@Serializable
data class MessageBroadcasted(
    val message: String
) : Message

/**
 * 更新课程
 */
@Serializable
data class UpdateCurriculum(
    val curriculum: String
) : Message

/**
 * 课程更新完成
 */
@Serializable
data class CurriculumUpdated(
    val curriculum: String
) : Message

/**
 * 安排评估
 */
@Serializable
data class ScheduleAssessment(
    val assessment: String
) : Message

/**
 * 评估安排完成
 */
@Serializable
data class AssessmentScheduled(
    val assessment: String
) : Message

// ============= 支持类型定义 =============

/**
 * 生成的内容
 */
@Serializable
data class GeneratedContent(
    val id: String,
    val title: String,
    val content: String,
    val type: ContentType,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val estimatedDuration: Long, // 分钟
    val objectives: List<String>,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 分析结果
 */
@Serializable
sealed class AnalysisResult {
    @Serializable
    data class OverallPerformance(
        val averagePerformance: Double,
        val completionRate: Double,
        val totalActivities: Int,
        val studentCount: Int,
        val performanceDistribution: Map<String, Int>
    ) : AnalysisResult()

    @Serializable
    data class IndividualProgress(
        val studentProgress: List<StudentProgressData>
    ) : AnalysisResult()

    @Serializable
    data class SubjectPerformance(
        val subjectData: Map<String, Double>
    ) : AnalysisResult()

    @Serializable
    data class SkillDevelopment(
        val skillData: Map<String, Double>
    ) : AnalysisResult()

    @Serializable
    data class EngagementMetrics(
        val engagementData: Map<String, Double>
    ) : AnalysisResult()

    @Serializable
    data class PredictiveAnalysis(
        val predictions: List<String>
    ) : AnalysisResult()
}

/**
 * 班级改进建议
 */
@Serializable
data class ClassImprovement(
    val area: String,
    val description: String,
    val priority: Priority,
    val suggestedActions: List<String>,
    val estimatedImpact: ImpactLevel
)

/**
 * 影响级别
 */
@Serializable
enum class ImpactLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * 学生进度数据
 */
@Serializable
data class StudentProgressData(
    val studentId: StudentId,
    val performance: Double,
    val completionRate: Double,
    val timeSpent: Long, // 分钟
    val activitiesCompleted: Int
)

/**
 * 内容生成请求
 */
@Serializable
data class ContentGenerationRequest(
    val type: ContentType,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val objectives: List<String>,
    val targetAudience: String,
    val constraints: ContentConstraints,
    val context: List<String> = emptyList() // 简化为字符串列表
)
