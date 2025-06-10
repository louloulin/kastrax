package ai.kastrax.edutech.recommendation

import ai.kastrax.edutech.models.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * 学习档案服务
 * 
 * 实现ed2.md第二阶段Week 5-6学习档案系统
 * 管理学生的学习偏好、能力评估和学习历史
 */
class LearningProfileService {
    private val profiles = mutableMapOf<StudentId, LearningProfile>()
    private val mutex = Mutex()
    
    /**
     * 获取学习档案
     *
     * @param studentId 学生ID
     * @return 学习档案
     */
    suspend fun getProfile(studentId: StudentId): LearningProfile? {
        return mutex.withLock {
            profiles[studentId]
        }
    }
    
    /**
     * 创建学习档案
     *
     * @param studentId 学生ID
     * @param initialData 初始数据
     * @return 创建结果
     */
    suspend fun createProfile(
        studentId: StudentId,
        initialData: ProfileInitialData? = null
    ): ProfileResult {
        return try {
            val profile = if (initialData != null) {
                LearningProfile.fromInitialData(studentId, initialData)
            } else {
                LearningProfile.createDefault(studentId)
            }
            
            mutex.withLock {
                profiles[studentId] = profile
            }
            
            ProfileResult.Success(profile)
        } catch (e: Exception) {
            ProfileResult.Failure("创建学习档案失败: ${e.message}")
        }
    }
    
    /**
     * 更新学习档案
     *
     * @param studentId 学生ID
     * @param updates 更新数据
     * @return 更新结果
     */
    suspend fun updateProfile(
        studentId: StudentId,
        updates: ProfileUpdates
    ): ProfileResult {
        return try {
            mutex.withLock {
                val currentProfile = profiles[studentId]
                    ?: return ProfileResult.Failure("学习档案不存在")
                
                val updatedProfile = currentProfile.applyUpdates(updates)
                profiles[studentId] = updatedProfile
                
                ProfileResult.Success(updatedProfile)
            }
        } catch (e: Exception) {
            ProfileResult.Failure("更新学习档案失败: ${e.message}")
        }
    }
    
    /**
     * 记录学习活动
     *
     * @param studentId 学生ID
     * @param activity 学习活动
     * @param performance 学习表现
     * @return 记录结果
     */
    suspend fun recordLearningActivity(
        studentId: StudentId,
        activity: LearningActivity,
        performance: ActivityPerformance
    ): ProfileUpdateResult {
        return try {
            mutex.withLock {
                val profile = profiles[studentId]
                    ?: return ProfileUpdateResult.Failure("学习档案不存在")
                
                val updatedProfile = profile.addActivityRecord(activity, performance)
                profiles[studentId] = updatedProfile
                
                ProfileUpdateResult.Success("学习活动记录成功")
            }
        } catch (e: Exception) {
            ProfileUpdateResult.Failure("记录学习活动失败: ${e.message}")
        }
    }
    
    /**
     * 评估学习风格
     *
     * @param studentId 学生ID
     * @param responses 问卷回答
     * @return 评估结果
     */
    suspend fun assessLearningStyle(
        studentId: StudentId,
        responses: List<StyleAssessmentResponse>
    ): StyleAssessmentResult {
        return try {
            val learningStyle = analyzeLearningStyle(responses)
            val confidence = calculateAssessmentConfidence(responses)
            
            // 更新档案中的学习风格
            updateProfile(studentId, ProfileUpdates(learningStyle = learningStyle))
            
            StyleAssessmentResult.Success(
                learningStyle = learningStyle,
                confidence = confidence,
                recommendations = generateStyleRecommendations(learningStyle)
            )
        } catch (e: Exception) {
            StyleAssessmentResult.Failure("学习风格评估失败: ${e.message}")
        }
    }
    
    /**
     * 评估技能水平
     *
     * @param studentId 学生ID
     * @param skill 技能类型
     * @param assessmentData 评估数据
     * @return 评估结果
     */
    suspend fun assessSkillLevel(
        studentId: StudentId,
        skill: Skill,
        assessmentData: SkillAssessmentData
    ): SkillAssessmentResult {
        return try {
            val skillLevel = calculateSkillLevel(assessmentData)
            val confidence = calculateSkillConfidence(assessmentData)
            
            // 更新档案中的技能水平
            val skillUpdates = mapOf(skill to skillLevel)
            updateProfile(studentId, ProfileUpdates(skillLevels = skillUpdates))
            
            SkillAssessmentResult.Success(
                skill = skill,
                level = skillLevel,
                confidence = confidence,
                recommendations = generateSkillRecommendations(skill, skillLevel)
            )
        } catch (e: Exception) {
            SkillAssessmentResult.Failure("技能评估失败: ${e.message}")
        }
    }
    
    /**
     * 获取学习统计
     *
     * @param studentId 学生ID
     * @param timeRange 时间范围
     * @return 学习统计
     */
    suspend fun getLearningStatistics(
        studentId: StudentId,
        timeRange: TimeRange? = null
    ): StatisticsResult {
        return try {
            val profile = getProfile(studentId)
                ?: return StatisticsResult.Failure("学习档案不存在")
            
            val statistics = calculateLearningStatistics(profile, timeRange)
            
            StatisticsResult.Success(statistics)
        } catch (e: Exception) {
            StatisticsResult.Failure("获取学习统计失败: ${e.message}")
        }
    }
    
    /**
     * 生成学习报告
     *
     * @param studentId 学生ID
     * @param reportType 报告类型
     * @return 学习报告
     */
    suspend fun generateLearningReport(
        studentId: StudentId,
        reportType: ReportType = ReportType.COMPREHENSIVE
    ): ReportResult {
        return try {
            val profile = getProfile(studentId)
                ?: return ReportResult.Failure("学习档案不存在")
            
            val report = when (reportType) {
                ReportType.COMPREHENSIVE -> generateComprehensiveReport(profile)
                ReportType.PROGRESS -> generateProgressReport(profile)
                ReportType.SKILLS -> generateSkillsReport(profile)
                ReportType.RECOMMENDATIONS -> generateRecommendationsReport(profile)
            }
            
            ReportResult.Success(report)
        } catch (e: Exception) {
            ReportResult.Failure("生成学习报告失败: ${e.message}")
        }
    }
    
    // 私有辅助方法
    
    private fun analyzeLearningStyle(responses: List<StyleAssessmentResponse>): LearningStyle {
        val scores = mutableMapOf<LearningStyle, Int>()
        
        responses.forEach { response ->
            response.styleWeights.forEach { (style, weight) ->
                scores[style] = scores.getOrDefault(style, 0) + weight
            }
        }
        
        return scores.maxByOrNull { it.value }?.key ?: LearningStyle.BALANCED
    }
    
    private fun calculateAssessmentConfidence(responses: List<StyleAssessmentResponse>): Double {
        // 简化实现：基于回答的一致性计算置信度
        return if (responses.size >= 10) 0.8 else responses.size * 0.08
    }
    
    private fun generateStyleRecommendations(learningStyle: LearningStyle): List<String> {
        return when (learningStyle) {
            LearningStyle.VISUAL -> listOf(
                "使用图表、图像和视频来学习",
                "制作思维导图和流程图",
                "使用颜色编码来组织信息"
            )
            LearningStyle.AUDITORY -> listOf(
                "参与讨论和小组学习",
                "使用录音来复习内容",
                "大声朗读学习材料"
            )
            LearningStyle.KINESTHETIC -> listOf(
                "通过实践和实验来学习",
                "使用模拟和互动练习",
                "定期休息和活动"
            )
            LearningStyle.READING_WRITING -> listOf(
                "做详细的笔记",
                "写总结和反思",
                "阅读多种资源"
            )
            LearningStyle.BALANCED -> listOf(
                "结合多种学习方法",
                "根据内容选择最适合的学习方式",
                "保持学习方法的多样性"
            )
        }
    }
    
    private fun calculateSkillLevel(assessmentData: SkillAssessmentData): SkillLevel {
        val averageScore = assessmentData.scores.average()
        return when {
            averageScore >= 0.9 -> SkillLevel.EXPERT
            averageScore >= 0.8 -> SkillLevel.ADVANCED
            averageScore >= 0.6 -> SkillLevel.INTERMEDIATE
            averageScore >= 0.4 -> SkillLevel.BEGINNER
            else -> SkillLevel.NOVICE
        }
    }
    
    private fun calculateSkillConfidence(assessmentData: SkillAssessmentData): Double {
        val variance = assessmentData.scores.map { score ->
            val mean = assessmentData.scores.average()
            (score - mean) * (score - mean)
        }.average()
        
        return 1.0 - variance // 方差越小，置信度越高
    }
    
    private fun generateSkillRecommendations(skill: Skill, level: SkillLevel): List<String> {
        return when (level) {
            SkillLevel.NOVICE, SkillLevel.BEGINNER -> listOf(
                "从基础概念开始学习",
                "多做练习题巩固基础",
                "寻求导师或同伴的帮助"
            )
            SkillLevel.INTERMEDIATE -> listOf(
                "挑战更复杂的问题",
                "学习相关的高级概念",
                "参与项目实践"
            )
            SkillLevel.ADVANCED, SkillLevel.EXPERT -> listOf(
                "探索前沿知识",
                "指导其他学习者",
                "参与研究或创新项目"
            )
        }
    }
    
    private fun calculateLearningStatistics(
        profile: LearningProfile,
        timeRange: TimeRange?
    ): LearningStatistics {
        val activities = if (timeRange != null) {
            profile.activityHistory.filter { 
                it.timestamp >= timeRange.start && it.timestamp <= timeRange.end 
            }
        } else {
            profile.activityHistory
        }
        
        return LearningStatistics(
            totalActivities = activities.size,
            totalLearningTime = activities.sumOf { it.duration },
            averagePerformance = activities.map { it.performance.accuracy }.average().takeIf { !it.isNaN() } ?: 0.0,
            completionRate = activities.count { it.completed }.toDouble() / activities.size.coerceAtLeast(1),
            preferredActivityTypes = activities.groupBy { it.activityType }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }
        )
    }
    
    private fun generateComprehensiveReport(profile: LearningProfile): LearningReport {
        return LearningReport(
            type = ReportType.COMPREHENSIVE,
            studentId = profile.studentId,
            generatedAt = Clock.System.now(),
            summary = "综合学习报告",
            sections = listOf(
                ReportSection("学习风格", "您的学习风格是${profile.learningStyle.displayName}"),
                ReportSection("技能水平", generateSkillsSummary(profile.skillLevels)),
                ReportSection("学习进度", generateProgressSummary(profile)),
                ReportSection("推荐建议", generateRecommendationsSummary(profile))
            )
        )
    }
    
    private fun generateProgressReport(profile: LearningProfile): LearningReport {
        return LearningReport(
            type = ReportType.PROGRESS,
            studentId = profile.studentId,
            generatedAt = Clock.System.now(),
            summary = "学习进度报告",
            sections = listOf(
                ReportSection("整体进度", generateProgressSummary(profile)),
                ReportSection("近期表现", generateRecentPerformanceSummary(profile))
            )
        )
    }
    
    private fun generateSkillsReport(profile: LearningProfile): LearningReport {
        return LearningReport(
            type = ReportType.SKILLS,
            studentId = profile.studentId,
            generatedAt = Clock.System.now(),
            summary = "技能评估报告",
            sections = listOf(
                ReportSection("技能水平", generateSkillsSummary(profile.skillLevels)),
                ReportSection("技能发展建议", generateSkillDevelopmentAdvice(profile.skillLevels))
            )
        )
    }
    
    private fun generateRecommendationsReport(profile: LearningProfile): LearningReport {
        return LearningReport(
            type = ReportType.RECOMMENDATIONS,
            studentId = profile.studentId,
            generatedAt = Clock.System.now(),
            summary = "个性化推荐报告",
            sections = listOf(
                ReportSection("学习建议", generateRecommendationsSummary(profile)),
                ReportSection("资源推荐", generateResourceRecommendations(profile))
            )
        )
    }
    
    // 简化的辅助方法实现
    private fun generateSkillsSummary(skillLevels: Map<Skill, SkillLevel>): String {
        return skillLevels.entries.joinToString(", ") { "${getSkillDisplayName(it.key)}: ${it.value.displayName}" }
    }

    private fun getSkillDisplayName(skill: Skill): String {
        return when (skill.value) {
            "critical_thinking" -> "批判性思维"
            "problem_solving" -> "问题解决"
            "logical_reasoning" -> "逻辑推理"
            "creative_thinking" -> "创造性思维"
            "analytical_thinking" -> "分析思维"
            "reading_comprehension" -> "阅读理解"
            "information_processing" -> "信息处理"
            "memory_retention" -> "记忆保持"
            "pattern_recognition" -> "模式识别"
            "knowledge_application" -> "知识应用"
            "written_communication" -> "书面沟通"
            "oral_communication" -> "口头沟通"
            "presentation" -> "演示"
            "collaboration" -> "协作"
            "peer_interaction" -> "同伴互动"
            "digital_literacy" -> "数字素养"
            "programming" -> "编程"
            "data_analysis" -> "数据分析"
            "research" -> "研究"
            "information_literacy" -> "信息素养"
            else -> skill.value
        }
    }
    
    private fun generateProgressSummary(profile: LearningProfile): String {
        return "学习进度良好，继续保持"
    }
    
    private fun generateRecommendationsSummary(profile: LearningProfile): String {
        return "根据您的学习风格，建议多使用${profile.learningStyle.displayName}相关的学习方法"
    }
    
    private fun generateRecentPerformanceSummary(profile: LearningProfile): String {
        return "近期学习表现稳定"
    }
    
    private fun generateSkillDevelopmentAdvice(skillLevels: Map<Skill, SkillLevel>): String {
        return "继续提升各项技能水平"
    }
    
    private fun generateResourceRecommendations(profile: LearningProfile): String {
        return "推荐适合您学习风格的资源"
    }
}

// 数据类定义
@Serializable
data class ProfileInitialData(
    val learningStyle: LearningStyle? = null,
    val interests: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val previousExperience: Map<Subject, SkillLevel> = emptyMap()
)

@Serializable
data class ProfileUpdates(
    val learningStyle: LearningStyle? = null,
    val skillLevels: Map<Skill, SkillLevel> = emptyMap(),
    val preferences: Map<String, String> = emptyMap(),
    val goals: List<String>? = null
)

@Serializable
data class ActivityPerformance(
    val accuracy: Double,
    val completionTime: Int,
    val engagementLevel: Double,
    val completed: Boolean
)

@Serializable
data class StyleAssessmentResponse(
    val questionId: String,
    val answer: String,
    val styleWeights: Map<LearningStyle, Int>
)

@Serializable
data class SkillAssessmentData(
    val scores: List<Double>,
    val timeSpent: Int,
    val attempts: Int
)

@Serializable
data class TimeRange(
    val start: kotlinx.datetime.Instant,
    val end: kotlinx.datetime.Instant
)

@Serializable
data class LearningStatistics(
    val totalActivities: Int,
    val totalLearningTime: Int,
    val averagePerformance: Double,
    val completionRate: Double,
    val preferredActivityTypes: List<ActivityType>
)

@Serializable
data class LearningReport(
    val type: ReportType,
    val studentId: StudentId,
    val generatedAt: kotlinx.datetime.Instant,
    val summary: String,
    val sections: List<ReportSection>
)

@Serializable
data class ReportSection(
    val title: String,
    val content: String
)

enum class ReportType {
    COMPREHENSIVE,
    PROGRESS,
    SKILLS,
    RECOMMENDATIONS
}

enum class SkillLevel {
    NOVICE,
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT;
    
    val displayName: String
        get() = when (this) {
            NOVICE -> "新手"
            BEGINNER -> "初学者"
            INTERMEDIATE -> "中级"
            ADVANCED -> "高级"
            EXPERT -> "专家"
        }
}

// 结果类型定义
sealed class ProfileResult {
    data class Success(val profile: LearningProfile) : ProfileResult()
    data class Failure(val error: String) : ProfileResult()
}

sealed class ProfileUpdateResult {
    data class Success(val message: String) : ProfileUpdateResult()
    data class Failure(val error: String) : ProfileUpdateResult()
}

sealed class StyleAssessmentResult {
    data class Success(
        val learningStyle: LearningStyle,
        val confidence: Double,
        val recommendations: List<String>
    ) : StyleAssessmentResult()
    data class Failure(val error: String) : StyleAssessmentResult()
}

sealed class SkillAssessmentResult {
    data class Success(
        val skill: Skill,
        val level: SkillLevel,
        val confidence: Double,
        val recommendations: List<String>
    ) : SkillAssessmentResult()
    data class Failure(val error: String) : SkillAssessmentResult()
}

sealed class StatisticsResult {
    data class Success(val statistics: LearningStatistics) : StatisticsResult()
    data class Failure(val error: String) : StatisticsResult()
}

sealed class ReportResult {
    data class Success(val report: LearningReport) : ReportResult()
    data class Failure(val error: String) : ReportResult()
}
