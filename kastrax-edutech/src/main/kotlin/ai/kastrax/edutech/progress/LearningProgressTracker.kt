package ai.kastrax.edutech.progress

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.assessment.GradingResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import java.util.*

// 进度跟踪数据模型

// 使用models包中的LearningActivity

/**
 * 活动表现
 */
@Serializable
data class ActivityPerformance(
    val accuracy: Double,
    val completionTime: Int, // 秒
    val engagementLevel: Double,
    val completed: Boolean,
    val attempts: Int = 1,
    val hintsUsed: Int = 0,
    val mistakesMade: Int = 0
)

/**
 * 学生进度记录
 */
@Serializable
data class StudentProgressRecord(
    val studentId: StudentId,
    var overallProgress: OverallProgress,
    val subjectProgress: MutableMap<Subject, SubjectProgressRecord>,
    val skillProgress: MutableMap<Skill, SkillProgressRecord>,
    val milestones: MutableList<Milestone>,
    var learningGoals: MutableList<LearningGoal> = mutableListOf(),
    var lastUpdated: Instant
)

/**
 * 整体进度
 */
@Serializable
data class OverallProgress(
    var totalActivities: Int = 0,
    var completedActivities: Int = 0,
    var averagePerformance: Double = 0.0,
    var completionRate: Double = 0.0,
    var totalTimeSpent: Int = 0, // 秒
    var currentStreak: Int = 0, // 连续学习天数
    var level: Int = 1,
    var experiencePoints: Int = 0
)

/**
 * 学科进度记录
 */
@Serializable
data class SubjectProgressRecord(
    val subject: Subject,
    var totalActivities: Int = 0,
    var completedActivities: Int = 0,
    var averagePerformance: Double = 0.0,
    var timeSpent: Int = 0,
    var currentLevel: DifficultyLevel = DifficultyLevel.BEGINNER,
    val topicsCompleted: MutableSet<Topic> = mutableSetOf(),
    var lastActivity: Instant
)

/**
 * 技能进度记录
 */
@Serializable
data class SkillProgressRecord(
    val skill: Skill,
    var currentLevel: Int = 1,
    var experiencePoints: Int = 0,
    var practiceCount: Int = 0,
    var averagePerformance: Double = 0.0,
    var recentImprovement: Double = 0.0,
    var lastPractice: Instant
)

/**
 * 里程碑
 */
@Serializable
data class Milestone(
    val id: String,
    val title: String,
    val description: String,
    val type: MilestoneType,
    val achievedAt: Instant
)

/**
 * 里程碑类型
 */
enum class MilestoneType {
    ACTIVITY_COUNT,         // 活动数量
    STREAK,                 // 连续学习
    HIGH_PERFORMANCE,       // 高分表现
    SKILL_MASTERY,          // 技能掌握
    SUBJECT_COMPLETION,     // 学科完成
    ASSESSMENT_EXCELLENCE   // 评估优秀
}

/**
 * 学习目标
 */
@Serializable
data class LearningGoal(
    val id: String = "goal_${UUID.randomUUID()}",
    val title: String,
    val description: String,
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val unit: String, // 如 "个活动", "分钟", "%"
    val deadline: Instant? = null,
    val priority: GoalPriority = GoalPriority.MEDIUM,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdAt: Instant = Clock.System.now()
)

/**
 * 目标优先级
 */
enum class GoalPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * 目标状态
 */
enum class GoalStatus {
    ACTIVE,     // 活跃
    COMPLETED,  // 已完成
    PAUSED,     // 暂停
    CANCELLED   // 取消
}

/**
 * 学习轨迹点
 */
@Serializable
data class LearningTrajectoryPoint(
    val timestamp: Instant,
    val activityType: ActivityType,
    val subject: Subject,
    val topic: Topic,
    val difficulty: DifficultyLevel,
    val performance: Double,
    val timeSpent: Int,
    val completed: Boolean,
    val engagementLevel: Double
)

/**
 * 学习轨迹
 */
@Serializable
data class LearningTrajectory(
    val studentId: StudentId,
    val points: List<LearningTrajectoryPoint>,
    val totalPoints: Int,
    val timeSpan: Duration
)

/**
 * 时间范围
 */
@Serializable
data class TimeRange(
    val start: Instant,
    val end: Instant
)

/**
 * 进度预警
 */
@Serializable
data class ProgressAlert(
    val id: String,
    val studentId: StudentId,
    val type: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val suggestions: List<String>,
    val createdAt: Instant,
    var resolved: Boolean = false,
    var resolvedAt: Instant? = null
)

/**
 * 预警类型
 */
enum class AlertType {
    LOW_PERFORMANCE,        // 表现不佳
    INSUFFICIENT_TIME,      // 学习时间不足
    LEARNING_STAGNATION,    // 学习停滞
    GOAL_BEHIND_SCHEDULE,   // 目标落后
    SKILL_REGRESSION,       // 技能退步
    ENGAGEMENT_DROP         // 参与度下降
}

/**
 * 预警严重程度
 */
enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * 进度报告
 */
@Serializable
data class ProgressReport(
    val id: String,
    val studentId: StudentId,
    val reportType: ProgressReportType,
    val title: String,
    val content: String,
    val generatedAt: Instant,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 报告类型
 */
enum class ProgressReportType {
    SUMMARY,    // 总结报告
    DETAILED,   // 详细报告
    ANALYTICS   // 分析报告
}

// 结果类型定义

/**
 * 进度更新结果
 */
sealed class ProgressUpdateResult {
    data class Success(val message: String) : ProgressUpdateResult()
    data class Failure(val error: String) : ProgressUpdateResult()
}

/**
 * 学生进度结果
 */
sealed class StudentProgressResult {
    data class Success(val progress: StudentProgressRecord) : StudentProgressResult()
    data class Failure(val error: String) : StudentProgressResult()
}

/**
 * 学习轨迹结果
 */
sealed class LearningTrajectoryResult {
    data class Success(val trajectory: LearningTrajectory) : LearningTrajectoryResult()
    data class Failure(val error: String) : LearningTrajectoryResult()
}

/**
 * 进度预警结果
 */
sealed class ProgressAlertsResult {
    data class Success(val alerts: List<ProgressAlert>) : ProgressAlertsResult()
    data class Failure(val error: String) : ProgressAlertsResult()
}

/**
 * 进度报告结果
 */
sealed class ProgressReportResult {
    data class Success(val report: ProgressReport) : ProgressReportResult()
    data class Failure(val error: String) : ProgressReportResult()
}

/**
 * 目标设置结果
 */
sealed class GoalSettingResult {
    data class Success(val message: String) : GoalSettingResult()
    data class Failure(val error: String) : GoalSettingResult()
}

/**
 * 学习进度跟踪器
 * 
 * 实现ed2.md第二阶段Week 7-8学习进度跟踪功能
 * 支持实时进度监控、学习轨迹记录、进度可视化和预警机制
 */
class LearningProgressTracker {
    private val progressRecords = mutableMapOf<StudentId, StudentProgressRecord>()
    private val learningTrajectories = mutableMapOf<StudentId, MutableList<LearningTrajectoryPoint>>()
    private val progressAlerts = mutableMapOf<StudentId, MutableList<ProgressAlert>>()
    private val mutex = Mutex()
    
    /**
     * 记录学习活动进度
     *
     * @param studentId 学生ID
     * @param activity 学习活动
     * @param performance 表现数据
     * @return 更新结果
     */
    suspend fun recordLearningProgress(
        studentId: StudentId,
        activity: LearningActivity,
        performance: ActivityPerformance
    ): ProgressUpdateResult {
        return try {
            mutex.withLock {
                // 获取或创建学生进度记录
                val progressRecord = progressRecords.getOrPut(studentId) {
                    StudentProgressRecord(
                        studentId = studentId,
                        overallProgress = OverallProgress(),
                        subjectProgress = mutableMapOf(),
                        skillProgress = mutableMapOf(),
                        milestones = mutableListOf(),
                        lastUpdated = Clock.System.now()
                    )
                }
                
                // 更新整体进度
                updateOverallProgress(progressRecord, activity, performance)
                
                // 更新学科进度
                updateSubjectProgress(progressRecord, activity, performance)
                
                // 更新技能进度
                updateSkillProgress(progressRecord, activity, performance)
                
                // 记录学习轨迹
                recordLearningTrajectory(studentId, activity, performance)
                
                // 检查里程碑
                checkMilestones(progressRecord, activity, performance)
                
                // 检查预警条件
                checkProgressAlerts(studentId, progressRecord)
                
                progressRecord.lastUpdated = Clock.System.now()
                progressRecords[studentId] = progressRecord
                
                ProgressUpdateResult.Success("进度更新成功")
            }
        } catch (e: Exception) {
            ProgressUpdateResult.Failure("进度更新失败: ${e.message}")
        }
    }
    
    /**
     * 记录评估结果进度
     *
     * @param studentId 学生ID
     * @param gradingResult 评估结果
     * @return 更新结果
     */
    suspend fun recordAssessmentProgress(
        studentId: StudentId,
        gradingResult: GradingResult
    ): ProgressUpdateResult {
        return try {
            mutex.withLock {
                val progressRecord = progressRecords.getOrPut(studentId) {
                    StudentProgressRecord(
                        studentId = studentId,
                        overallProgress = OverallProgress(),
                        subjectProgress = mutableMapOf(),
                        skillProgress = mutableMapOf(),
                        milestones = mutableListOf(),
                        lastUpdated = Clock.System.now()
                    )
                }
                
                // 更新评估相关进度
                updateAssessmentProgress(progressRecord, gradingResult)
                
                // 记录评估轨迹点
                recordAssessmentTrajectory(studentId, gradingResult)
                
                // 检查评估里程碑
                checkAssessmentMilestones(progressRecord, gradingResult)
                
                progressRecord.lastUpdated = Clock.System.now()
                
                ProgressUpdateResult.Success("评估进度更新成功")
            }
        } catch (e: Exception) {
            ProgressUpdateResult.Failure("评估进度更新失败: ${e.message}")
        }
    }
    
    /**
     * 获取学生进度
     *
     * @param studentId 学生ID
     * @return 进度结果
     */
    suspend fun getStudentProgress(studentId: StudentId): StudentProgressResult {
        return mutex.withLock {
            val progressRecord = progressRecords[studentId]
                ?: return StudentProgressResult.Failure("未找到学生进度记录")
            
            StudentProgressResult.Success(progressRecord)
        }
    }
    
    /**
     * 获取学习轨迹
     *
     * @param studentId 学生ID
     * @param timeRange 时间范围
     * @return 轨迹结果
     */
    suspend fun getLearningTrajectory(
        studentId: StudentId,
        timeRange: TimeRange? = null
    ): LearningTrajectoryResult {
        return mutex.withLock {
            val trajectory = learningTrajectories[studentId] ?: emptyList()
            
            val filteredTrajectory = if (timeRange != null) {
                trajectory.filter { point ->
                    point.timestamp >= timeRange.start && point.timestamp <= timeRange.end
                }
            } else {
                trajectory
            }
            
            LearningTrajectoryResult.Success(
                LearningTrajectory(
                    studentId = studentId,
                    points = filteredTrajectory,
                    totalPoints = filteredTrajectory.size,
                    timeSpan = if (filteredTrajectory.isNotEmpty()) {
                        filteredTrajectory.last().timestamp.minus(filteredTrajectory.first().timestamp)
                    } else {
                        kotlin.time.Duration.ZERO
                    }
                )
            )
        }
    }
    
    /**
     * 获取进度预警
     *
     * @param studentId 学生ID
     * @return 预警结果
     */
    suspend fun getProgressAlerts(studentId: StudentId): ProgressAlertsResult {
        return mutex.withLock {
            val alerts = progressAlerts[studentId] ?: emptyList()
            ProgressAlertsResult.Success(alerts.filter { !it.resolved })
        }
    }
    
    /**
     * 生成进度报告
     *
     * @param studentId 学生ID
     * @param reportType 报告类型
     * @return 报告结果
     */
    suspend fun generateProgressReport(
        studentId: StudentId,
        reportType: ProgressReportType
    ): ProgressReportResult {
        return try {
            val progressRecord = progressRecords[studentId]
                ?: return ProgressReportResult.Failure("未找到学生进度记录")
            
            val trajectory = learningTrajectories[studentId] ?: emptyList()
            val alerts = progressAlerts[studentId] ?: emptyList()
            
            val report = when (reportType) {
                ProgressReportType.SUMMARY -> generateSummaryReport(progressRecord, trajectory)
                ProgressReportType.DETAILED -> generateDetailedReport(progressRecord, trajectory, alerts)
                ProgressReportType.ANALYTICS -> generateAnalyticsReport(progressRecord, trajectory)
            }
            
            ProgressReportResult.Success(report)
        } catch (e: Exception) {
            ProgressReportResult.Failure("生成报告失败: ${e.message}")
        }
    }
    
    /**
     * 设置学习目标
     *
     * @param studentId 学生ID
     * @param goals 学习目标
     * @return 设置结果
     */
    suspend fun setLearningGoals(
        studentId: StudentId,
        goals: List<LearningGoal>
    ): GoalSettingResult {
        return try {
            mutex.withLock {
                val progressRecord = progressRecords.getOrPut(studentId) {
                    StudentProgressRecord(
                        studentId = studentId,
                        overallProgress = OverallProgress(),
                        subjectProgress = mutableMapOf(),
                        skillProgress = mutableMapOf(),
                        milestones = mutableListOf(),
                        lastUpdated = Clock.System.now()
                    )
                }
                
                progressRecord.learningGoals = goals.toMutableList()
                progressRecord.lastUpdated = Clock.System.now()
                
                GoalSettingResult.Success("学习目标设置成功")
            }
        } catch (e: Exception) {
            GoalSettingResult.Failure("设置学习目标失败: ${e.message}")
        }
    }
    
    // 私有辅助方法
    
    private fun updateOverallProgress(
        record: StudentProgressRecord,
        activity: LearningActivity,
        performance: ActivityPerformance
    ) {
        val overall = record.overallProgress
        overall.totalActivities++
        overall.totalTimeSpent += performance.completionTime
        
        if (performance.completed) {
            overall.completedActivities++
        }
        
        // 更新平均表现
        val totalPerformance = overall.averagePerformance * (overall.totalActivities - 1) + performance.accuracy
        overall.averagePerformance = totalPerformance / overall.totalActivities
        
        // 更新完成率
        overall.completionRate = overall.completedActivities.toDouble() / overall.totalActivities * 100
        
        // 更新连续学习天数（简化实现）
        overall.currentStreak++
        
        // 更新经验值
        val experienceGain = calculateExperienceGain(activity, performance)
        overall.experiencePoints += experienceGain
        
        // 检查等级提升
        val newLevel = calculateLevel(overall.experiencePoints)
        if (newLevel > overall.level) {
            overall.level = newLevel
        }
    }
    
    private fun updateSubjectProgress(
        record: StudentProgressRecord,
        activity: LearningActivity,
        performance: ActivityPerformance
    ) {
        val subject = activity.subject
        val subjectProgress = record.subjectProgress.getOrPut(subject) {
            SubjectProgressRecord(
                subject = subject,
                totalActivities = 0,
                completedActivities = 0,
                averagePerformance = 0.0,
                timeSpent = 0,
                currentLevel = DifficultyLevel.BEGINNER,
                topicsCompleted = mutableSetOf(),
                lastActivity = Clock.System.now()
            )
        }
        
        subjectProgress.totalActivities++
        subjectProgress.timeSpent += performance.completionTime
        
        if (performance.completed) {
            subjectProgress.completedActivities++
            subjectProgress.topicsCompleted.add(activity.topic)
        }
        
        // 更新平均表现
        val totalPerformance = subjectProgress.averagePerformance * (subjectProgress.totalActivities - 1) + performance.accuracy
        subjectProgress.averagePerformance = totalPerformance / subjectProgress.totalActivities
        
        // 更新难度级别
        if (subjectProgress.averagePerformance > 0.8 && subjectProgress.completedActivities >= 5) {
            subjectProgress.currentLevel = when (subjectProgress.currentLevel) {
                DifficultyLevel.BEGINNER -> DifficultyLevel.ELEMENTARY
                DifficultyLevel.ELEMENTARY -> DifficultyLevel.INTERMEDIATE
                DifficultyLevel.INTERMEDIATE -> DifficultyLevel.ADVANCED
                DifficultyLevel.ADVANCED -> DifficultyLevel.EXPERT
                DifficultyLevel.EXPERT -> DifficultyLevel.EXPERT
            }
        }
        
        subjectProgress.lastActivity = Clock.System.now()
    }
    
    private fun updateSkillProgress(
        record: StudentProgressRecord,
        activity: LearningActivity,
        performance: ActivityPerformance
    ) {
        activity.skillsInvolved.forEach { skill ->
            val skillProgress = record.skillProgress.getOrPut(skill) {
                SkillProgressRecord(
                    skill = skill,
                    currentLevel = 1,
                    experiencePoints = 0,
                    practiceCount = 0,
                    averagePerformance = 0.0,
                    recentImprovement = 0.0,
                    lastPractice = Clock.System.now()
                )
            }
            
            skillProgress.practiceCount++
            
            // 更新平均表现
            val totalPerformance = skillProgress.averagePerformance * (skillProgress.practiceCount - 1) + performance.accuracy
            skillProgress.averagePerformance = totalPerformance / skillProgress.practiceCount
            
            // 计算技能经验值增长
            val skillExperience = (performance.accuracy * 100).toInt()
            skillProgress.experiencePoints += skillExperience
            
            // 更新技能等级
            val newLevel = skillProgress.experiencePoints / 1000 + 1
            if (newLevel > skillProgress.currentLevel) {
                skillProgress.currentLevel = newLevel
            }
            
            skillProgress.lastPractice = Clock.System.now()
        }
    }
    
    private fun recordLearningTrajectory(
        studentId: StudentId,
        activity: LearningActivity,
        performance: ActivityPerformance
    ) {
        val trajectory = learningTrajectories.getOrPut(studentId) { mutableListOf() }
        
        val trajectoryPoint = LearningTrajectoryPoint(
            timestamp = Clock.System.now(),
            activityType = activity.type,
            subject = activity.subject,
            topic = activity.topic,
            difficulty = activity.difficulty,
            performance = performance.accuracy,
            timeSpent = performance.completionTime,
            completed = performance.completed,
            engagementLevel = performance.engagementLevel
        )
        
        trajectory.add(trajectoryPoint)
        
        // 保持最近1000个点
        if (trajectory.size > 1000) {
            trajectory.removeAt(0)
        }
    }
    
    private fun recordAssessmentTrajectory(
        studentId: StudentId,
        gradingResult: GradingResult
    ) {
        val trajectory = learningTrajectories.getOrPut(studentId) { mutableListOf() }
        
        val trajectoryPoint = LearningTrajectoryPoint(
            timestamp = gradingResult.gradedAt,
            activityType = ActivityType.ASSESSMENT,
            subject = Subject.GENERAL, // 从评估中获取
            topic = Topic("评估"),
            difficulty = DifficultyLevel.INTERMEDIATE,
            performance = gradingResult.percentage / 100.0,
            timeSpent = 0, // 评估时间需要从提交记录中获取
            completed = gradingResult.passed,
            engagementLevel = if (gradingResult.passed) 0.8 else 0.6
        )
        
        trajectory.add(trajectoryPoint)
    }
    
    private fun checkMilestones(
        record: StudentProgressRecord,
        activity: LearningActivity,
        performance: ActivityPerformance
    ) {
        // 检查各种里程碑
        val milestones = mutableListOf<Milestone>()
        
        // 完成活动数里程碑
        if (record.overallProgress.completedActivities in listOf(10, 50, 100, 500)) {
            milestones.add(
                Milestone(
                    id = "activities_${record.overallProgress.completedActivities}",
                    title = "完成${record.overallProgress.completedActivities}个学习活动",
                    description = "恭喜你完成了${record.overallProgress.completedActivities}个学习活动！",
                    type = MilestoneType.ACTIVITY_COUNT,
                    achievedAt = Clock.System.now()
                )
            )
        }
        
        // 连续学习里程碑
        if (record.overallProgress.currentStreak in listOf(7, 30, 100)) {
            milestones.add(
                Milestone(
                    id = "streak_${record.overallProgress.currentStreak}",
                    title = "连续学习${record.overallProgress.currentStreak}天",
                    description = "坚持就是胜利！你已经连续学习${record.overallProgress.currentStreak}天了！",
                    type = MilestoneType.STREAK,
                    achievedAt = Clock.System.now()
                )
            )
        }
        
        // 高分表现里程碑
        if (performance.accuracy >= 0.95) {
            milestones.add(
                Milestone(
                    id = "perfect_score_${UUID.randomUUID()}",
                    title = "完美表现",
                    description = "在${activity.topic.value}中取得了${(performance.accuracy * 100).toInt()}%的优秀成绩！",
                    type = MilestoneType.HIGH_PERFORMANCE,
                    achievedAt = Clock.System.now()
                )
            )
        }
        
        record.milestones.addAll(milestones)
    }
    
    private fun checkAssessmentMilestones(
        record: StudentProgressRecord,
        gradingResult: GradingResult
    ) {
        if (gradingResult.passed && gradingResult.percentage >= 90) {
            val milestone = Milestone(
                id = "assessment_excellence_${gradingResult.id.value}",
                title = "评估优秀",
                description = "在评估中取得了${gradingResult.percentage.toInt()}%的优秀成绩！",
                type = MilestoneType.ASSESSMENT_EXCELLENCE,
                achievedAt = gradingResult.gradedAt
            )
            record.milestones.add(milestone)
        }
    }
    
    private fun checkProgressAlerts(studentId: StudentId, record: StudentProgressRecord) {
        val alerts = progressAlerts.getOrPut(studentId) { mutableListOf() }
        
        // 检查学习停滞
        if (record.overallProgress.averagePerformance < 0.6) {
            alerts.add(
                ProgressAlert(
                    id = "low_performance_${UUID.randomUUID()}",
                    studentId = studentId,
                    type = AlertType.LOW_PERFORMANCE,
                    severity = AlertSeverity.MEDIUM,
                    message = "学习表现需要改进，当前平均表现为${(record.overallProgress.averagePerformance * 100).toInt()}%",
                    suggestions = listOf(
                        "复习基础知识",
                        "寻求帮助",
                        "调整学习方法"
                    ),
                    createdAt = Clock.System.now()
                )
            )
        }
        
        // 检查学习时间不足
        if (record.overallProgress.totalTimeSpent < 3600) { // 少于1小时
            alerts.add(
                ProgressAlert(
                    id = "insufficient_time_${UUID.randomUUID()}",
                    studentId = studentId,
                    type = AlertType.INSUFFICIENT_TIME,
                    severity = AlertSeverity.LOW,
                    message = "学习时间较少，建议增加学习投入",
                    suggestions = listOf(
                        "制定学习计划",
                        "设置学习提醒",
                        "增加每日学习时间"
                    ),
                    createdAt = Clock.System.now()
                )
            )
        }
    }
    
    private fun calculateExperienceGain(activity: LearningActivity, performance: ActivityPerformance): Int {
        val baseExperience = when (activity.difficulty) {
            DifficultyLevel.BEGINNER -> 10
            DifficultyLevel.ELEMENTARY -> 15
            DifficultyLevel.INTERMEDIATE -> 20
            DifficultyLevel.ADVANCED -> 30
            DifficultyLevel.EXPERT -> 50
        }
        
        val performanceMultiplier = performance.accuracy
        val completionBonus = if (performance.completed) 1.2 else 1.0
        
        return (baseExperience * performanceMultiplier * completionBonus).toInt()
    }
    
    private fun calculateLevel(experiencePoints: Int): Int {
        return (experiencePoints / 1000) + 1
    }
    
    private fun generateSummaryReport(
        record: StudentProgressRecord,
        trajectory: List<LearningTrajectoryPoint>
    ): ProgressReport {
        val content = """
            # 学习进度总结报告
            
            ## 整体表现
            - 完成活动：${record.overallProgress.completedActivities}/${record.overallProgress.totalActivities}
            - 平均表现：${(record.overallProgress.averagePerformance * 100).toInt()}%
            - 完成率：${record.overallProgress.completionRate.toInt()}%
            - 当前等级：${record.overallProgress.level}
            
            ## 学科进度
            ${record.subjectProgress.entries.joinToString("\n") { (subject, progress) ->
                "- ${subject.displayName}：${progress.completedActivities}个活动完成，平均表现${(progress.averagePerformance * 100).toInt()}%"
            }}
            
            ## 最近成就
            ${record.milestones.takeLast(3).joinToString("\n") { milestone ->
                "- ${milestone.title}：${milestone.description}"
            }}
        """.trimIndent()
        
        return ProgressReport(
            id = "summary_${UUID.randomUUID()}",
            studentId = record.studentId,
            reportType = ProgressReportType.SUMMARY,
            title = "学习进度总结",
            content = content,
            generatedAt = Clock.System.now()
        )
    }
    
    private fun generateDetailedReport(
        record: StudentProgressRecord,
        trajectory: List<LearningTrajectoryPoint>,
        alerts: List<ProgressAlert>
    ): ProgressReport {
        // 详细报告实现
        return ProgressReport(
            id = "detailed_${UUID.randomUUID()}",
            studentId = record.studentId,
            reportType = ProgressReportType.DETAILED,
            title = "详细学习分析报告",
            content = "详细分析内容...",
            generatedAt = Clock.System.now()
        )
    }
    
    private fun generateAnalyticsReport(
        record: StudentProgressRecord,
        trajectory: List<LearningTrajectoryPoint>
    ): ProgressReport {
        // 分析报告实现
        return ProgressReport(
            id = "analytics_${UUID.randomUUID()}",
            studentId = record.studentId,
            reportType = ProgressReportType.ANALYTICS,
            title = "学习数据分析报告",
            content = "数据分析内容...",
            generatedAt = Clock.System.now()
        )
    }

    private fun updateAssessmentProgress(
        record: StudentProgressRecord,
        gradingResult: GradingResult
    ) {
        val overall = record.overallProgress
        overall.totalActivities++

        if (gradingResult.passed) {
            overall.completedActivities++
        }

        // 更新平均表现
        val totalPerformance = overall.averagePerformance * (overall.totalActivities - 1) + (gradingResult.percentage / 100.0)
        overall.averagePerformance = totalPerformance / overall.totalActivities

        // 更新完成率
        overall.completionRate = overall.completedActivities.toDouble() / overall.totalActivities * 100

        // 更新经验值
        val experienceGain = (gradingResult.percentage * 2).toInt() // 评估给更多经验
        overall.experiencePoints += experienceGain

        // 检查等级提升
        val newLevel = calculateLevel(overall.experiencePoints)
        if (newLevel > overall.level) {
            overall.level = newLevel
        }
    }
}
