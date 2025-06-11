package ai.kastrax.edutech.collaborative

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

// ==================== 基础枚举类型 ====================

@Serializable
enum class SessionStatus {
    PLANNED,        // 计划中
    ACTIVE,         // 进行中
    PAUSED,         // 暂停
    COMPLETED,      // 已完成
    CANCELLED       // 已取消
}

@Serializable
enum class EngagementLevel {
    VERY_HIGH,
    HIGH,
    MEDIUM,
    LOW,
    VERY_LOW
}

@Serializable
enum class SkillLevel {
    BEGINNER,
    NOVICE,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

/**
 * Week 23-24: 智能协作学习平台 - 数据模型
 * 
 * 核心功能：
 * - 实时协作学习环境
 * - 智能小组匹配系统
 * - 协作学习分析引擎
 * - 同伴学习推荐系统
 * - 群体智能评估系统
 */

// ==================== 协作学习会话 ====================

@Serializable
data class CollaborativeSession(
    val id: SessionId,
    val title: String,
    val description: String,
    val subject: Subject,
    val topic: String,
    val facilitatorId: String, // 教师或主持人ID
    val participants: List<SessionParticipant>,
    val sessionType: CollaborativeSessionType,
    val learningObjectives: List<String>,
    val activities: List<CollaborativeActivity>,
    val status: SessionStatus,
    val settings: SessionSettings,
    val createdAt: Instant,
    val startTime: Instant?,
    val endTime: Instant?,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class SessionParticipant(
    val studentId: StudentId,
    val role: ParticipantRole,
    val joinedAt: Instant,
    val status: ParticipantStatus,
    val contributionScore: Double = 0.0,
    val engagementLevel: EngagementLevel = EngagementLevel.MEDIUM,
    val permissions: Set<SessionPermission> = emptySet()
)

@Serializable
enum class CollaborativeSessionType {
    PEER_LEARNING,          // 同伴学习
    GROUP_PROJECT,          // 小组项目
    DISCUSSION_FORUM,       // 讨论论坛
    COLLABORATIVE_WRITING,  // 协作写作
    PROBLEM_SOLVING,        // 问题解决
    STUDY_GROUP,           // 学习小组
    VIRTUAL_CLASSROOM      // 虚拟课堂
}

@Serializable
enum class ParticipantRole {
    FACILITATOR,    // 主持人
    LEADER,         // 组长
    MEMBER,         // 成员
    OBSERVER,       // 观察者
    MENTOR          // 导师
}

@Serializable
enum class ParticipantStatus {
    ACTIVE,         // 活跃
    IDLE,           // 空闲
    AWAY,           // 离开
    DISCONNECTED    // 断开连接
}

@Serializable
enum class SessionPermission {
    EDIT_CONTENT,       // 编辑内容
    MODERATE_DISCUSSION, // 主持讨论
    INVITE_PARTICIPANTS, // 邀请参与者
    MANAGE_ACTIVITIES,   // 管理活动
    VIEW_ANALYTICS      // 查看分析
}

@Serializable
data class SessionSettings(
    val maxParticipants: Int = 30,
    val allowAnonymous: Boolean = false,
    val enableVoiceChat: Boolean = true,
    val enableVideoChat: Boolean = false,
    val enableScreenShare: Boolean = true,
    val enableFileSharing: Boolean = true,
    val moderationLevel: ModerationLevel = ModerationLevel.MEDIUM,
    val recordSession: Boolean = false
)

@Serializable
enum class ModerationLevel {
    NONE,       // 无审核
    LOW,        // 低级审核
    MEDIUM,     // 中级审核
    HIGH        // 高级审核
}

// ==================== 协作活动 ====================

@Serializable
data class CollaborativeActivity(
    val id: String,
    val sessionId: SessionId,
    val type: ActivityType,
    val title: String,
    val description: String,
    val instructions: String,
    val resources: List<ActivityResource>,
    val timeline: ActivityTimeline,
    val groupConfiguration: GroupConfiguration,
    val assessmentCriteria: List<AssessmentCriterion>,
    val status: ActivityStatus,
    val createdAt: Instant,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class ActivityType {
    BRAINSTORMING,          // 头脑风暴
    PEER_REVIEW,            // 同伴评议
    GROUP_DISCUSSION,       // 小组讨论
    COLLABORATIVE_DOCUMENT, // 协作文档
    PROBLEM_SOLVING,        // 问题解决
    CASE_STUDY,            // 案例研究
    ROLE_PLAYING,          // 角色扮演
    KNOWLEDGE_SHARING      // 知识分享
}

@Serializable
data class ActivityResource(
    val id: String,
    val type: ResourceType,
    val title: String,
    val url: String?,
    val content: String?,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class ResourceType {
    DOCUMENT,       // 文档
    VIDEO,          // 视频
    AUDIO,          // 音频
    IMAGE,          // 图片
    LINK,           // 链接
    INTERACTIVE     // 交互式内容
}

@Serializable
data class ActivityTimeline(
    val startTime: Instant,
    val endTime: Instant,
    val phases: List<ActivityPhase>
)

@Serializable
data class ActivityPhase(
    val name: String,
    val description: String,
    val duration: Duration,
    val requirements: List<String>
)

@Serializable
data class GroupConfiguration(
    val groupSizeMin: Int,
    val groupSizeMax: Int,
    val groupingStrategy: GroupingStrategy,
    val allowSelfSelection: Boolean = false,
    val balancingCriteria: List<BalancingCriterion> = emptyList()
) {
    val groupSize: IntRange
        get() = groupSizeMin..groupSizeMax
}

@Serializable
enum class GroupingStrategy {
    RANDOM,             // 随机分组
    ABILITY_BASED,      // 基于能力
    INTEREST_BASED,     // 基于兴趣
    COMPLEMENTARY,      // 互补性分组
    MIXED_ABILITY,      // 混合能力
    SELF_SELECTED      // 自选分组
}

@Serializable
data class BalancingCriterion(
    val type: BalancingType,
    val weight: Double,
    val target: String
)

@Serializable
enum class BalancingType {
    SKILL_LEVEL,        // 技能水平
    LEARNING_STYLE,     // 学习风格
    PERSONALITY,        // 性格特征
    EXPERIENCE,         // 经验水平
    TIMEZONE,           // 时区
    LANGUAGE           // 语言
}

@Serializable
enum class ActivityStatus {
    PLANNED,        // 计划中
    ACTIVE,         // 进行中
    PAUSED,         // 暂停
    COMPLETED,      // 已完成
    CANCELLED       // 已取消
}

// ==================== 协作交互 ====================

@Serializable
data class CollaborativeInteraction(
    val id: String,
    val sessionId: SessionId,
    val activityId: String?,
    val participantId: StudentId,
    val type: InteractionType,
    val content: InteractionContent,
    val timestamp: Instant,
    val responses: List<InteractionResponse> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class InteractionType {
    MESSAGE,            // 消息
    QUESTION,           // 问题
    ANSWER,             // 回答
    COMMENT,            // 评论
    REACTION,           // 反应
    EDIT,               // 编辑
    SHARE,              // 分享
    VOTE,               // 投票
    ANNOTATION         // 注释
}

@Serializable
data class InteractionContent(
    val text: String?,
    val attachments: List<Attachment> = emptyList(),
    val mentions: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val formatting: Map<String, String> = emptyMap()
)

@Serializable
data class Attachment(
    val id: String,
    val type: AttachmentType,
    val name: String,
    val url: String,
    val size: Long,
    val mimeType: String
)

@Serializable
enum class AttachmentType {
    FILE,
    IMAGE,
    VIDEO,
    AUDIO,
    LINK,
    CODE_SNIPPET
}

@Serializable
data class InteractionResponse(
    val participantId: StudentId,
    val type: ResponseType,
    val content: String?,
    val timestamp: Instant
)

@Serializable
enum class ResponseType {
    LIKE,           // 点赞
    DISLIKE,        // 点踩
    HELPFUL,        // 有帮助
    UNCLEAR,        // 不清楚
    AGREE,          // 同意
    DISAGREE,       // 不同意
    QUESTION,       // 提问
    SUGGESTION     // 建议
}

// ==================== 小组匹配 ====================

@Serializable
data class GroupMatchingRequest(
    val sessionId: SessionId,
    val activityId: String,
    val participants: List<StudentId>,
    val groupConfiguration: GroupConfiguration,
    val preferences: MatchingPreferences,
    val constraints: List<MatchingConstraint> = emptyList()
)

@Serializable
data class MatchingPreferences(
    val prioritizeCompatibility: Boolean = true,
    val prioritizeComplementarity: Boolean = false,
    val considerPastCollaborations: Boolean = true,
    val avoidRecentPartners: Boolean = true,
    val balanceSkillLevels: Boolean = true,
    val respectTimeZones: Boolean = true
)

@Serializable
data class MatchingConstraint(
    val type: ConstraintType,
    val participants: List<StudentId>,
    val description: String
)

@Serializable
enum class ConstraintType {
    MUST_BE_TOGETHER,       // 必须在一起
    CANNOT_BE_TOGETHER,     // 不能在一起
    PREFERRED_TOGETHER,     // 偏好在一起
    AVOID_TOGETHER,         // 避免在一起
    SKILL_REQUIREMENT,      // 技能要求
    ROLE_REQUIREMENT       // 角色要求
}

@Serializable
data class GroupMatchingResult(
    val requestId: String,
    val groups: List<CollaborativeGroup>,
    val matchingScore: Double,
    val explanation: String,
    val alternatives: List<GroupAlternative> = emptyList(),
    val generatedAt: Instant
)

@Serializable
data class CollaborativeGroup(
    val id: String,
    val members: List<GroupMember>,
    val compatibilityScore: Double,
    val balanceScore: Double,
    val predictedPerformance: Double,
    val recommendedRoles: Map<StudentId, ParticipantRole>
)

@Serializable
data class GroupMember(
    val studentId: StudentId,
    val profile: CollaborativeProfile,
    val contributionPotential: Double,
    val compatibilityScores: Map<StudentId, Double>
)

@Serializable
data class GroupAlternative(
    val groups: List<CollaborativeGroup>,
    val score: Double,
    val reason: String
)

// ==================== 协作档案 ====================

@Serializable
data class CollaborativeProfile(
    val studentId: StudentId,
    val collaborationStyle: CollaborationStyle,
    val communicationPreferences: CommunicationPreferences,
    val skillContributions: Map<String, SkillLevel>,
    val pastCollaborations: List<CollaborationHistory>,
    val performanceMetrics: CollaborationMetrics,
    val preferences: StudentPreferences,
    val lastUpdated: Instant
)

@Serializable
data class CollaborationStyle(
    val leadership: LeadershipStyle,
    val participation: ParticipationStyle,
    val conflictResolution: ConflictResolutionStyle,
    val workingStyle: WorkingStyle,
    val feedbackStyle: FeedbackStyle
)

@Serializable
enum class LeadershipStyle {
    NATURAL_LEADER,     // 天然领导者
    SUPPORTIVE_LEADER,  // 支持型领导
    COLLABORATIVE,      // 协作型
    FOLLOWER,          // 跟随者
    SITUATIONAL        // 情境型
}

@Serializable
enum class ParticipationStyle {
    HIGHLY_ACTIVE,      // 高度活跃
    MODERATELY_ACTIVE,  // 适度活跃
    SELECTIVE,          // 选择性参与
    OBSERVER,           // 观察者
    RESERVED           // 保守型
}

@Serializable
enum class ConflictResolutionStyle {
    COLLABORATIVE,      // 协作解决
    COMPETITIVE,        // 竞争型
    ACCOMMODATING,      // 迁就型
    AVOIDING,           // 回避型
    COMPROMISING       // 妥协型
}

@Serializable
enum class WorkingStyle {
    STRUCTURED,         // 结构化
    FLEXIBLE,           // 灵活型
    DETAIL_ORIENTED,    // 细节导向
    BIG_PICTURE,        // 大局观
    METHODICAL         // 有条理的
}

@Serializable
enum class FeedbackStyle {
    DIRECT,             // 直接型
    DIPLOMATIC,         // 外交型
    CONSTRUCTIVE,       // 建设性
    ENCOURAGING,        // 鼓励型
    ANALYTICAL         // 分析型
}

@Serializable
data class CommunicationPreferences(
    val preferredChannels: Set<CommunicationChannel>,
    val responseTimeExpectation: Duration,
    val formalityLevel: FormalityLevel,
    val languagePreferences: List<String>,
    val availabilityWindows: List<TimeWindow>
)

@Serializable
enum class CommunicationChannel {
    TEXT_CHAT,          // 文字聊天
    VOICE_CALL,         // 语音通话
    VIDEO_CALL,         // 视频通话
    EMAIL,              // 邮件
    FORUM,              // 论坛
    DOCUMENT_COMMENTS   // 文档评论
}

@Serializable
enum class FormalityLevel {
    VERY_FORMAL,        // 非常正式
    FORMAL,             // 正式
    SEMI_FORMAL,        // 半正式
    INFORMAL,           // 非正式
    VERY_INFORMAL      // 非常非正式
}

@Serializable
data class TimeWindow(
    val dayOfWeek: Int, // 1-7 (Monday-Sunday)
    val startHour: Int, // 0-23
    val endHour: Int,   // 0-23
    val timezone: String
)

@Serializable
data class CollaborationHistory(
    val sessionId: SessionId,
    val partnersIds: List<StudentId>,
    val duration: Duration,
    val performance: CollaborationPerformance,
    val feedback: List<PeerFeedback>,
    val completedAt: Instant
)

@Serializable
data class CollaborationPerformance(
    val overallScore: Double,
    val contributionScore: Double,
    val communicationScore: Double,
    val reliabilityScore: Double,
    val creativityScore: Double,
    val problemSolvingScore: Double
)

@Serializable
data class PeerFeedback(
    val fromStudentId: StudentId,
    val toStudentId: StudentId,
    val ratings: Map<String, Double>,
    val comments: String?,
    val timestamp: Instant
)

@Serializable
data class CollaborationMetrics(
    val totalSessions: Int,
    val averagePerformance: Double,
    val preferredGroupSizeMin: Int,
    val preferredGroupSizeMax: Int,
    val successfulCollaborations: Int,
    val leadershipExperience: Int,
    val mentorshipExperience: Int,
    val conflictResolutionSuccess: Double,
    val peerRating: Double
) {
    val preferredGroupSize: IntRange
        get() = preferredGroupSizeMin..preferredGroupSizeMax
}

@Serializable
data class StudentPreferences(
    val preferredGroupSizeMin: Int,
    val preferredGroupSizeMax: Int,
    val preferredPartnerTypes: Set<PartnerType>,
    val avoidedPartnerTypes: Set<PartnerType>,
    val workingTimePreferences: List<TimeWindow>,
    val subjectInterests: Map<Subject, InterestLevel>,
    val collaborationGoals: List<CollaborationGoal>
) {
    val preferredGroupSize: IntRange
        get() = preferredGroupSizeMin..preferredGroupSizeMax
}

@Serializable
enum class PartnerType {
    SIMILAR_SKILL,      // 相似技能
    COMPLEMENTARY_SKILL, // 互补技能
    HIGHER_SKILL,       // 更高技能
    LOWER_SKILL,        // 较低技能
    SIMILAR_STYLE,      // 相似风格
    DIFFERENT_STYLE,    // 不同风格
    EXPERIENCED,        // 有经验的
    BEGINNER           // 初学者
}

@Serializable
enum class InterestLevel {
    VERY_HIGH,
    HIGH,
    MEDIUM,
    LOW,
    VERY_LOW
}

@Serializable
enum class CollaborationGoal {
    SKILL_DEVELOPMENT,      // 技能发展
    KNOWLEDGE_SHARING,      // 知识分享
    PEER_LEARNING,          // 同伴学习
    LEADERSHIP_PRACTICE,    // 领导力练习
    COMMUNICATION_IMPROVEMENT, // 沟通改进
    CULTURAL_EXCHANGE,      // 文化交流
    NETWORKING,             // 建立网络
    PROJECT_COMPLETION     // 项目完成
}

// ==================== 结果类型 ====================

@Serializable
sealed class CollaborativeSessionResult {
    @Serializable
    data class Success(val session: CollaborativeSession) : CollaborativeSessionResult()
    
    @Serializable
    data class Error(val message: String, val code: String) : CollaborativeSessionResult()
}

@Serializable
sealed class GroupMatchingResultType {
    @Serializable
    data class Success(val result: GroupMatchingResult) : GroupMatchingResultType()
    
    @Serializable
    data class Error(val message: String, val code: String) : GroupMatchingResultType()
}

@Serializable
sealed class CollaborationAnalysisResult {
    @Serializable
    data class Success(val analysis: CollaborationAnalysis) : CollaborationAnalysisResult()
    
    @Serializable
    data class Error(val message: String, val code: String) : CollaborationAnalysisResult()
}

@Serializable
data class CollaborationAnalysis(
    val sessionId: SessionId,
    val participationMetrics: Map<StudentId, ParticipationMetrics>,
    val groupDynamics: GroupDynamicsAnalysis,
    val learningOutcomes: LearningOutcomesAnalysis,
    val recommendations: List<CollaborationRecommendation>,
    val generatedAt: Instant
)

@Serializable
data class ParticipationMetrics(
    val messageCount: Int,
    val activeTime: Duration,
    val contributionQuality: Double,
    val engagementLevel: EngagementLevel,
    val leadershipMoments: Int,
    val helpfulInteractions: Int
)

@Serializable
data class GroupDynamicsAnalysis(
    val cohesionScore: Double,
    val communicationEffectiveness: Double,
    val conflictLevel: ConflictLevel,
    val leadershipDistribution: Map<StudentId, Double>,
    val collaborationPatterns: List<CollaborationPattern>
)

@Serializable
enum class ConflictLevel {
    NONE,
    LOW,
    MODERATE,
    HIGH,
    SEVERE
}

@Serializable
data class CollaborationPattern(
    val type: PatternType,
    val participants: List<StudentId>,
    val frequency: Double,
    val impact: PatternImpact
)

@Serializable
enum class PatternType {
    KNOWLEDGE_SHARING,      // 知识分享
    PEER_SUPPORT,          // 同伴支持
    LEADERSHIP_ROTATION,    // 领导轮换
    CONFLICT_RESOLUTION,    // 冲突解决
    CREATIVE_COLLABORATION, // 创意协作
    TASK_DISTRIBUTION      // 任务分配
}

@Serializable
enum class PatternImpact {
    VERY_POSITIVE,
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
    VERY_NEGATIVE
}

@Serializable
data class LearningOutcomesAnalysis(
    val objectiveAchievement: Map<String, Double>,
    val skillDevelopment: Map<StudentId, Map<String, Double>>,
    val knowledgeGain: Map<StudentId, Double>,
    val collaborationSkillImprovement: Map<StudentId, Double>
)

@Serializable
data class CollaborationRecommendation(
    val type: RecommendationType,
    val targetParticipants: List<StudentId>,
    val description: String,
    val priority: RecommendationPriority,
    val actionItems: List<String>,
    val expectedOutcome: String
)

@Serializable
enum class RecommendationType {
    GROUP_RESTRUCTURING,    // 小组重组
    ROLE_ADJUSTMENT,        // 角色调整
    COMMUNICATION_IMPROVEMENT, // 沟通改进
    CONFLICT_INTERVENTION,  // 冲突干预
    SKILL_DEVELOPMENT,      // 技能发展
    ENGAGEMENT_BOOST,       // 参与度提升
    LEADERSHIP_OPPORTUNITY  // 领导机会
}

@Serializable
enum class RecommendationPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    OPTIONAL
}
