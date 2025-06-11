package ai.kastrax.edutech.grading

import ai.kastrax.edutech.models.DifficultyLevel
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * 增强反馈
 */
@Serializable
data class EnhancedFeedback(
    val originalFeedback: DetailedFeedback,
    val errorAnalysis: ErrorAnalysis,
    val improvementPlan: ImprovementPlan,
    val resourceRecommendations: List<LearningResource>,
    val personalizedGuidance: PersonalizedGuidance,
    val actionPlan: ActionPlan,
    val generatedAt: Instant
)

/**
 * 错误分析
 */
@Serializable
data class ErrorAnalysis(
    val majorErrors: List<ErrorDetail>,
    val minorErrors: List<ErrorDetail>,
    val errorPatterns: List<String>,
    val rootCauses: List<String>
)

/**
 * 错误详情
 */
@Serializable
data class ErrorDetail(
    val type: String,
    val description: String,
    val severity: Severity,
    val suggestion: String,
    val lineNumber: Int? = null,
    val context: String? = null
)

/**
 * 改进计划
 */
@Serializable
data class ImprovementPlan(
    val shortTermGoals: List<LearningGoal>, // 1-2周
    val mediumTermGoals: List<LearningGoal>, // 1-2个月
    val longTermGoals: List<LearningGoal> // 3-6个月
)

/**
 * 学习目标
 */
@Serializable
data class LearningGoal(
    val title: String,
    val description: String,
    val estimatedTime: Duration,
    val priority: Priority,
    val prerequisites: List<String> = emptyList(),
    val successCriteria: List<String> = emptyList()
)

/**
 * 个性化指导
 */
@Serializable
data class PersonalizedGuidance(
    val learningAdvice: String,
    val studyMethods: List<String>,
    val motivation: String,
    val habitSuggestions: List<String>,
    val psychologicalSupport: String
)

/**
 * 行动计划
 */
@Serializable
data class ActionPlan(
    val steps: List<ActionStep>,
    val estimatedTotalTime: Int, // 分钟
    val difficulty: DifficultyLevel
)

/**
 * 行动步骤
 */
@Serializable
data class ActionStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val estimatedTime: Duration,
    val resources: List<LearningResource>,
    val priority: Priority,
    val completed: Boolean = false
)

/**
 * 学生档案（简化版本，用于个性化）
 */
@Serializable
data class StudentProfile(
    val studentId: String,
    val learningStyle: LearningStyle,
    val skillLevels: Map<String, SkillLevel>,
    val preferences: LearningPreferences,
    val weakAreas: List<String> = emptyList(),
    val strongAreas: List<String> = emptyList()
)

/**
 * 学习风格
 */
@Serializable
enum class LearningStyle(val displayName: String) {
    VISUAL("视觉型"),
    AUDITORY("听觉型"),
    KINESTHETIC("动觉型"),
    READING_WRITING("读写型"),
    MIXED("混合型")
}

/**
 * 技能水平
 */
@Serializable
enum class SkillLevel(val displayName: String, val value: Int) {
    NOVICE("新手", 1),
    BEGINNER("初学者", 2),
    INTERMEDIATE("中级", 3),
    ADVANCED("高级", 4),
    EXPERT("专家", 5)
}

/**
 * 学习偏好
 */
@Serializable
data class LearningPreferences(
    val preferredContentTypes: List<ContentType>,
    val preferredDifficulty: DifficultyLevel,
    val learningPace: LearningPace,
    val feedbackFrequency: FeedbackFrequency
)

/**
 * 内容类型偏好
 */
@Serializable
enum class ContentType(val displayName: String) {
    TEXT("文本"),
    VIDEO("视频"),
    INTERACTIVE("交互式"),
    AUDIO("音频"),
    VISUAL("图像"),
    HANDS_ON("实践操作")
}

/**
 * 学习节奏
 */
@Serializable
enum class LearningPace(val displayName: String) {
    SLOW("慢节奏"),
    MODERATE("中等节奏"),
    FAST("快节奏"),
    ADAPTIVE("自适应")
}

/**
 * 反馈频率
 */
@Serializable
enum class FeedbackFrequency(val displayName: String) {
    IMMEDIATE("即时反馈"),
    FREQUENT("频繁反馈"),
    MODERATE("适度反馈"),
    MINIMAL("最少反馈")
}

/**
 * 反馈效果评估
 */
@Serializable
data class FeedbackEffectiveness(
    val feedbackId: String,
    val studentId: String,
    val helpfulness: Double, // 0-1, 学生评价的有用性
    val clarity: Double, // 0-1, 清晰度
    val actionability: Double, // 0-1, 可操作性
    val motivation: Double, // 0-1, 激励效果
    val overallSatisfaction: Double, // 0-1, 总体满意度
    val studentComments: String = "",
    val evaluatedAt: Instant
)

/**
 * 反馈改进建议
 */
@Serializable
data class FeedbackImprovementSuggestion(
    val category: String,
    val currentIssue: String,
    val suggestedImprovement: String,
    val priority: Priority,
    val implementationDifficulty: DifficultyLevel
)

/**
 * 反馈模板
 */
@Serializable
data class FeedbackTemplate(
    val id: String,
    val name: String,
    val assignmentType: AssignmentType,
    val template: String,
    val variables: List<TemplateVariable>,
    val isActive: Boolean = true
)

/**
 * 模板变量
 */
@Serializable
data class TemplateVariable(
    val name: String,
    val type: VariableType,
    val description: String,
    val required: Boolean = true,
    val defaultValue: String? = null
)

/**
 * 变量类型
 */
@Serializable
enum class VariableType(val displayName: String) {
    STRING("字符串"),
    NUMBER("数字"),
    BOOLEAN("布尔值"),
    LIST("列表"),
    OBJECT("对象")
}

/**
 * 反馈生成配置
 */
@Serializable
data class FeedbackGenerationConfig(
    val enableDetailedAnalysis: Boolean = true,
    val enableResourceRecommendation: Boolean = true,
    val enablePersonalizedGuidance: Boolean = true,
    val enableActionPlan: Boolean = true,
    val maxResourceCount: Int = 10,
    val maxActionSteps: Int = 8,
    val feedbackLanguage: String = "zh-CN",
    val toneOfVoice: ToneOfVoice = ToneOfVoice.ENCOURAGING
)

/**
 * 反馈语调
 */
@Serializable
enum class ToneOfVoice(val displayName: String) {
    FORMAL("正式"),
    FRIENDLY("友好"),
    ENCOURAGING("鼓励"),
    CONSTRUCTIVE("建设性"),
    DIRECT("直接")
}

/**
 * 反馈质量指标
 */
@Serializable
data class FeedbackQualityMetrics(
    val relevance: Double, // 相关性 0-1
    val specificity: Double, // 具体性 0-1
    val constructiveness: Double, // 建设性 0-1
    val clarity: Double, // 清晰度 0-1
    val completeness: Double, // 完整性 0-1
    val timeliness: Double, // 及时性 0-1
    val overallQuality: Double // 总体质量 0-1
)

/**
 * 反馈分析报告
 */
@Serializable
data class FeedbackAnalysisReport(
    val reportId: String,
    val generatedAt: Instant,
    val timeRange: TimeRange,
    val totalFeedbacks: Int,
    val averageQuality: FeedbackQualityMetrics,
    val studentSatisfaction: Double,
    val improvementAreas: List<String>,
    val successMetrics: Map<String, Double>,
    val recommendations: List<FeedbackImprovementSuggestion>
)

/**
 * 时间范围
 */
@Serializable
data class TimeRange(
    val startTime: Instant,
    val endTime: Instant
)

/**
 * 反馈个性化设置
 */
@Serializable
data class FeedbackPersonalizationSettings(
    val studentId: String,
    val preferredTone: ToneOfVoice,
    val detailLevel: DetailLevel,
    val focusAreas: List<String>,
    val avoidAreas: List<String> = emptyList(),
    val motivationalPreferences: List<String> = emptyList()
)

/**
 * 详细程度
 */
@Serializable
enum class DetailLevel(val displayName: String) {
    BRIEF("简要"),
    MODERATE("适中"),
    DETAILED("详细"),
    COMPREHENSIVE("全面")
}

/**
 * 反馈交付方式
 */
@Serializable
data class FeedbackDelivery(
    val method: DeliveryMethod,
    val timing: DeliveryTiming,
    val format: FeedbackFormat,
    val channels: List<DeliveryChannel>
)

/**
 * 交付方式
 */
@Serializable
enum class DeliveryMethod(val displayName: String) {
    IMMEDIATE("即时交付"),
    SCHEDULED("定时交付"),
    ON_DEMAND("按需交付"),
    BATCH("批量交付")
}

/**
 * 交付时机
 */
@Serializable
enum class DeliveryTiming(val displayName: String) {
    AFTER_SUBMISSION("提交后"),
    AFTER_GRADING("批改后"),
    DAILY_SUMMARY("每日总结"),
    WEEKLY_SUMMARY("每周总结")
}

/**
 * 反馈格式
 */
@Serializable
enum class FeedbackFormat(val displayName: String) {
    TEXT("纯文本"),
    HTML("HTML格式"),
    PDF("PDF文档"),
    INTERACTIVE("交互式"),
    MULTIMEDIA("多媒体")
}

/**
 * 交付渠道
 */
@Serializable
enum class DeliveryChannel(val displayName: String) {
    EMAIL("邮件"),
    SMS("短信"),
    PUSH_NOTIFICATION("推送通知"),
    IN_APP("应用内"),
    DASHBOARD("仪表板")
}
