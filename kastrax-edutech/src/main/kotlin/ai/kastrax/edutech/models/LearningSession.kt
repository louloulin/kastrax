package ai.kastrax.edutech.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * 学习会话实体 - 基于ed2.md第3.2节数据模型设计
 * 
 * 管理学生的学习会话状态和活动记录
 */
@Serializable
data class LearningSession(
    val id: SessionId,
    val studentId: StudentId,
    val courseId: CourseId,
    val objectives: List<String>,
    val activities: List<LearningActivity> = emptyList(),
    val startTime: Instant,
    val endTime: Instant? = null,
    val status: SessionStatus,
    val sessionMetrics: SessionMetrics,
    val contextData: Map<String, String> = emptyMap()
) {
    companion object {
        fun create(
            studentId: StudentId,
            courseId: CourseId,
            objectives: List<String>
        ): LearningSession {
            val now = kotlinx.datetime.Clock.System.now()
            return LearningSession(
                id = SessionId.generate(),
                studentId = studentId,
                courseId = courseId,
                objectives = objectives,
                startTime = now,
                status = SessionStatus.ACTIVE,
                sessionMetrics = SessionMetrics.initial()
            )
        }
    }
    
    fun addActivity(activity: LearningActivity): LearningSession {
        return copy(
            activities = activities + activity,
            sessionMetrics = sessionMetrics.updateWith(activity)
        )
    }
    
    fun complete(): LearningSession {
        val now = kotlinx.datetime.Clock.System.now()
        return copy(
            endTime = now,
            status = SessionStatus.COMPLETED,
            sessionMetrics = sessionMetrics.finalize(now, startTime)
        )
    }
    
    fun getDuration(): Duration? {
        return endTime?.let { end ->
            end - startTime
        }
    }
    
    fun isActive(): Boolean = status == SessionStatus.ACTIVE
}

/**
 * 强类型的会话ID
 */
@Serializable
@JvmInline
value class SessionId(val value: String) {
    companion object {
        fun generate(): SessionId = SessionId("session_${java.util.UUID.randomUUID()}")
        fun fromString(value: String): SessionId = SessionId(value)
    }
    
    override fun toString(): String = value
}

/**
 * 强类型的课程ID
 */
@Serializable
@JvmInline
value class CourseId(val value: String) {
    companion object {
        fun generate(): CourseId = CourseId("course_${java.util.UUID.randomUUID()}")
        fun fromString(value: String): CourseId = CourseId(value)
    }
    
    override fun toString(): String = value
}

/**
 * 会话状态枚举
 */
@Serializable
enum class SessionStatus(val displayName: String) {
    ACTIVE("进行中"),
    PAUSED("暂停"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    ERROR("错误状态");
    
    fun canTransitionTo(newStatus: SessionStatus): Boolean = when (this) {
        ACTIVE -> newStatus in setOf(PAUSED, COMPLETED, CANCELLED, ERROR)
        PAUSED -> newStatus in setOf(ACTIVE, COMPLETED, CANCELLED)
        COMPLETED -> false
        CANCELLED -> false
        ERROR -> newStatus in setOf(ACTIVE, CANCELLED)
    }
}

/**
 * 会话指标 - 实现ed2.md第2.2节学习状态跟踪
 */
@Serializable
data class SessionMetrics(
    val totalTimeSpent: Duration,
    val activitiesCompleted: Int,
    val averagePerformance: Double,
    val engagementScore: Double,
    val difficultyProgression: List<DifficultyLevel>,
    val topicsStudied: Set<Topic>,
    val skillsExercised: Set<Skill>,
    val mistakeCount: Int,
    val hintUsageCount: Int,
    val selfAssessmentScores: List<Int>
) {
    companion object {
        fun initial(): SessionMetrics = SessionMetrics(
            totalTimeSpent = Duration.ZERO,
            activitiesCompleted = 0,
            averagePerformance = 0.0,
            engagementScore = 0.0,
            difficultyProgression = emptyList(),
            topicsStudied = emptySet(),
            skillsExercised = emptySet(),
            mistakeCount = 0,
            hintUsageCount = 0,
            selfAssessmentScores = emptyList()
        )
    }
    
    fun updateWith(activity: LearningActivity): SessionMetrics {
        return copy(
            activitiesCompleted = activitiesCompleted + 1,
            averagePerformance = calculateNewAverage(averagePerformance, activity.performance, activitiesCompleted + 1),
            engagementScore = updateEngagementScore(activity),
            difficultyProgression = difficultyProgression + activity.difficulty,
            topicsStudied = topicsStudied + activity.topic,
            skillsExercised = skillsExercised + activity.skillsInvolved,
            mistakeCount = mistakeCount + activity.mistakesMade,
            hintUsageCount = hintUsageCount + activity.hintsUsed
        )
    }
    
    fun finalize(endTime: Instant, startTime: Instant): SessionMetrics {
        return copy(
            totalTimeSpent = endTime - startTime
        )
    }
    
    private fun calculateNewAverage(currentAvg: Double, newValue: Double, count: Int): Double {
        return if (count <= 1) newValue else (currentAvg * (count - 1) + newValue) / count
    }
    
    private fun updateEngagementScore(activity: LearningActivity): Double {
        // 基于活动参与度计算参与分数
        val timeEngagement = minOf(activity.timeSpent.inWholeMinutes / 10.0, 1.0)
        val performanceEngagement = activity.performance / 100.0
        val interactionEngagement = if (activity.interactionCount > 0) 1.0 else 0.5
        
        return (timeEngagement + performanceEngagement + interactionEngagement) / 3.0
    }
}

/**
 * 学习活动 - 实现ed2.md第3.4节用户体验流程
 */
@Serializable
data class LearningActivity(
    val id: ActivityId,
    val type: ActivityType,
    val topic: Topic,
    val difficulty: DifficultyLevel,
    val skillsInvolved: Set<Skill>,
    val startTime: Instant,
    val endTime: Instant? = null,
    val performance: Double, // 0-100
    val timeSpent: Duration,
    val mistakesMade: Int = 0,
    val hintsUsed: Int = 0,
    val interactionCount: Int = 0,
    val completed: Boolean = false,
    val feedback: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        fun create(
            type: ActivityType,
            topic: Topic,
            difficulty: DifficultyLevel,
            skillsInvolved: Set<Skill>
        ): LearningActivity {
            val now = kotlinx.datetime.Clock.System.now()
            return LearningActivity(
                id = ActivityId.generate(),
                type = type,
                topic = topic,
                difficulty = difficulty,
                skillsInvolved = skillsInvolved,
                startTime = now,
                performance = 0.0,
                timeSpent = Duration.ZERO
            )
        }
    }
    
    fun complete(performance: Double, feedback: String? = null): LearningActivity {
        val now = kotlinx.datetime.Clock.System.now()
        return copy(
            endTime = now,
            performance = performance,
            timeSpent = endTime?.let { it - startTime } ?: Duration.ZERO,
            completed = true,
            feedback = feedback
        )
    }
    
    fun addMistake(): LearningActivity = copy(mistakesMade = mistakesMade + 1)
    fun useHint(): LearningActivity = copy(hintsUsed = hintsUsed + 1)
    fun addInteraction(): LearningActivity = copy(interactionCount = interactionCount + 1)
}

/**
 * 强类型的活动ID
 */
@Serializable
@JvmInline
value class ActivityId(val value: String) {
    companion object {
        fun generate(): ActivityId = ActivityId("activity_${java.util.UUID.randomUUID()}")
        fun fromString(value: String): ActivityId = ActivityId(value)
    }
    
    override fun toString(): String = value
}

/**
 * 活动类型枚举
 */
@Serializable
enum class ActivityType(val displayName: String, val category: ActivityCategory) {
    READING("阅读", ActivityCategory.CONTENT_CONSUMPTION),
    VIDEO_WATCHING("观看视频", ActivityCategory.CONTENT_CONSUMPTION),
    LISTENING("听讲", ActivityCategory.CONTENT_CONSUMPTION),
    
    QUIZ("测验", ActivityCategory.ASSESSMENT),
    EXAM("考试", ActivityCategory.ASSESSMENT),
    ASSIGNMENT("作业", ActivityCategory.ASSESSMENT),
    
    DISCUSSION("讨论", ActivityCategory.INTERACTION),
    COLLABORATION("协作", ActivityCategory.INTERACTION),
    PEER_REVIEW("同伴评议", ActivityCategory.INTERACTION),
    
    PRACTICE("练习", ActivityCategory.PRACTICE),
    SIMULATION("模拟", ActivityCategory.PRACTICE),
    EXPERIMENT("实验", ActivityCategory.PRACTICE),
    
    REFLECTION("反思", ActivityCategory.METACOGNITION),
    SELF_ASSESSMENT("自我评估", ActivityCategory.METACOGNITION),
    GOAL_SETTING("目标设定", ActivityCategory.METACOGNITION);
}

@Serializable
enum class ActivityCategory(val displayName: String) {
    CONTENT_CONSUMPTION("内容消费"),
    ASSESSMENT("评估测试"),
    INTERACTION("互动交流"),
    PRACTICE("实践练习"),
    METACOGNITION("元认知")
}

/**
 * 技能枚举
 */
@Serializable
@JvmInline
value class Skill(val value: String) {
    companion object {
        // 认知技能
        val CRITICAL_THINKING = Skill("critical_thinking")
        val PROBLEM_SOLVING = Skill("problem_solving")
        val LOGICAL_REASONING = Skill("logical_reasoning")
        val CREATIVE_THINKING = Skill("creative_thinking")
        val ANALYTICAL_THINKING = Skill("analytical_thinking")
        
        // 学习技能
        val READING_COMPREHENSION = Skill("reading_comprehension")
        val INFORMATION_PROCESSING = Skill("information_processing")
        val MEMORY_RETENTION = Skill("memory_retention")
        val PATTERN_RECOGNITION = Skill("pattern_recognition")
        val KNOWLEDGE_APPLICATION = Skill("knowledge_application")
        
        // 交流技能
        val WRITTEN_COMMUNICATION = Skill("written_communication")
        val ORAL_COMMUNICATION = Skill("oral_communication")
        val PRESENTATION = Skill("presentation")
        val COLLABORATION = Skill("collaboration")
        val PEER_INTERACTION = Skill("peer_interaction")
        
        // 技术技能
        val DIGITAL_LITERACY = Skill("digital_literacy")
        val PROGRAMMING = Skill("programming")
        val DATA_ANALYSIS = Skill("data_analysis")
        val RESEARCH = Skill("research")
        val INFORMATION_LITERACY = Skill("information_literacy")
    }
    
    override fun toString(): String = value
}
