package ai.kastrax.edutech.recommendation

import ai.kastrax.edutech.models.*
import ai.kastrax.rag.RAG
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * 个性化推荐引擎
 * 
 * 实现ed2.md第二阶段Week 5-6个性化推荐引擎
 * 基于RAG系统和学习档案提供个性化内容推荐
 */
class PersonalizationEngine(
    private val ragSystem: RAG,
    private val learningProfileService: LearningProfileService
) {
    private val mutex = Mutex()
    private val recommendationCache = mutableMapOf<String, List<ContentRecommendation>>()
    
    /**
     * 生成个性化学习计划
     *
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @param objectives 学习目标
     * @return 学习计划
     */
    suspend fun generateLearningPlan(
        studentId: StudentId,
        courseId: CourseId,
        objectives: List<String>
    ): LearningPlanResult {
        return try {
            val profile = learningProfileService.getProfile(studentId)
                ?: return LearningPlanResult.Failure("学习档案不存在")
            
            // 分析学习目标
            val analyzedObjectives = analyzeObjectives(objectives, profile)
            
            // 生成学习路径
            val learningPath = generateLearningPath(analyzedObjectives, profile)
            
            // 创建学习计划
            val plan = LearningPlan(
                id = LearningPlanId.generate(),
                studentId = studentId,
                courseId = courseId,
                objectives = analyzedObjectives,
                learningPath = learningPath,
                estimatedDuration = calculateEstimatedDuration(learningPath),
                difficulty = calculatePlanDifficulty(learningPath, profile),
                createdAt = Clock.System.now()
            )
            
            LearningPlanResult.Success(plan)
        } catch (e: Exception) {
            LearningPlanResult.Failure("生成学习计划失败: ${e.message}")
        }
    }
    
    /**
     * 生成内容推荐
     *
     * @param studentId 学生ID
     * @param context 推荐上下文
     * @param limit 推荐数量限制
     * @return 推荐结果
     */
    suspend fun generateRecommendations(
        studentId: StudentId,
        context: RecommendationContext,
        limit: Int = 10
    ): RecommendationResult {
        return try {
            val profile = learningProfileService.getProfile(studentId)
                ?: return RecommendationResult.Failure("学习档案不存在")
            
            // 构建推荐查询
            val query = buildRecommendationQuery(context, profile)
            
            // 使用RAG系统搜索相关内容
            val searchResults = ragSystem.search(query, limit * 2) // 获取更多候选
            
            // 基于学习档案过滤和排序
            val recommendations = searchResults
                .mapNotNull { result -> convertToRecommendation(result, profile) }
                .sortedByDescending { it.score }
                .take(limit)
            
            // 缓存推荐结果
            mutex.withLock {
                recommendationCache[studentId.value] = recommendations
            }
            
            RecommendationResult.Success(recommendations)
        } catch (e: Exception) {
            RecommendationResult.Failure("生成推荐失败: ${e.message}")
        }
    }
    
    /**
     * 适应性调整学习计划
     *
     * @param planId 学习计划ID
     * @param feedback 学习反馈
     * @param performance 学习表现
     * @return 调整结果
     */
    suspend fun adaptPlan(
        planId: LearningPlanId,
        feedback: LearningFeedback,
        performance: LearningPerformance
    ): PlanAdaptationResult {
        return try {
            // 分析学习表现
            val analysis = analyzePerformance(performance, feedback)
            
            // 生成调整建议
            val adaptations = generateAdaptations(analysis)
            
            // 应用调整
            val adaptedPlan = applyAdaptations(planId, adaptations)
            
            PlanAdaptationResult.Success(
                adaptedPlan = adaptedPlan,
                adaptations = adaptations,
                reason = analysis.summary
            )
        } catch (e: Exception) {
            PlanAdaptationResult.Failure("计划调整失败: ${e.message}")
        }
    }
    
    /**
     * 获取学习建议
     *
     * @param studentId 学生ID
     * @param currentActivity 当前活动
     * @return 学习建议
     */
    suspend fun getLearningAdvice(
        studentId: StudentId,
        currentActivity: LearningActivity? = null
    ): LearningAdviceResult {
        return try {
            val profile = learningProfileService.getProfile(studentId)
                ?: return LearningAdviceResult.Failure("学习档案不存在")
            
            val advice = generateLearningAdvice(profile, currentActivity)
            
            LearningAdviceResult.Success(advice)
        } catch (e: Exception) {
            LearningAdviceResult.Failure("获取学习建议失败: ${e.message}")
        }
    }
    
    // 私有辅助方法
    
    private fun analyzeObjectives(
        objectives: List<String>,
        profile: LearningProfile
    ): List<AnalyzedObjective> {
        return objectives.map { objective ->
            AnalyzedObjective(
                text = objective,
                difficulty = estimateObjectiveDifficulty(objective, profile),
                estimatedTime = estimateObjectiveTime(objective, profile),
                prerequisites = identifyPrerequisites(objective, profile),
                skills = identifyRequiredSkills(objective)
            )
        }
    }
    
    private fun generateLearningPath(
        objectives: List<AnalyzedObjective>,
        profile: LearningProfile
    ): LearningPath {
        // 根据学习风格和能力生成最优学习路径
        val steps = objectives.flatMap { objective ->
            generateLearningSteps(objective, profile)
        }
        
        return LearningPath(
            steps = steps,
            totalEstimatedTime = steps.sumOf { it.estimatedTime },
            difficulty = calculatePathDifficulty(steps)
        )
    }
    
    private fun generateLearningSteps(
        objective: AnalyzedObjective,
        profile: LearningProfile
    ): List<LearningStep> {
        val steps = mutableListOf<LearningStep>()
        
        // 根据学习风格选择合适的学习活动类型
        val preferredActivityTypes = getPreferredActivityTypes(profile.learningStyle)
        
        preferredActivityTypes.forEach { activityType ->
            steps.add(
                LearningStep(
                    id = LearningStepId.generate(),
                    objective = objective.text,
                    activityType = activityType,
                    estimatedTime = objective.estimatedTime / preferredActivityTypes.size,
                    difficulty = objective.difficulty,
                    order = steps.size
                )
            )
        }
        
        return steps
    }
    
    private fun getPreferredActivityTypes(learningStyle: LearningStyle): List<ActivityType> {
        return when (learningStyle) {
            LearningStyle.VISUAL -> listOf(
                ActivityType.VIDEO_WATCHING,
                ActivityType.READING,
                ActivityType.PRACTICE
            )
            LearningStyle.AUDITORY -> listOf(
                ActivityType.LISTENING,
                ActivityType.DISCUSSION,
                ActivityType.PRACTICE
            )
            LearningStyle.KINESTHETIC -> listOf(
                ActivityType.PRACTICE,
                ActivityType.SIMULATION,
                ActivityType.EXPERIMENT
            )
            LearningStyle.READING_WRITING -> listOf(
                ActivityType.READING,
                ActivityType.ASSIGNMENT,
                ActivityType.PRACTICE
            )
            LearningStyle.BALANCED -> listOf(
                ActivityType.READING,
                ActivityType.VIDEO_WATCHING,
                ActivityType.PRACTICE,
                ActivityType.DISCUSSION
            )
        }
    }
    
    private fun buildRecommendationQuery(
        context: RecommendationContext,
        profile: LearningProfile
    ): String {
        val queryParts = mutableListOf<String>()
        
        // 添加当前主题
        context.currentTopic?.let { queryParts.add(it.value) }
        
        // 添加学习目标
        context.learningObjectives.forEach { queryParts.add(it) }
        
        // 添加学科信息
        context.subject?.let { queryParts.add(it.displayName) }
        
        // 添加难度级别
        queryParts.add("难度:${context.difficulty.displayName}")
        
        // 添加学习风格偏好
        queryParts.add("学习风格:${profile.learningStyle.displayName}")
        
        return queryParts.joinToString(" ")
    }
    
    private fun convertToRecommendation(
        searchResult: ai.kastrax.store.document.DocumentSearchResult,
        profile: LearningProfile
    ): ContentRecommendation? {
        return try {
            val contentId = searchResult.document.metadata["contentId"] as? String
                ?: return null

            // 计算个性化评分
            val personalizedScore = calculatePersonalizedScore(searchResult, profile)

            ContentRecommendation(
                contentId = ContentId(contentId),
                score = personalizedScore,
                reason = generateRecommendationReason(searchResult, profile),
                timestamp = Clock.System.now()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculatePersonalizedScore(
        searchResult: ai.kastrax.store.document.DocumentSearchResult,
        profile: LearningProfile
    ): Double {
        var score = searchResult.score
        
        // 根据学习风格调整评分
        val contentType = searchResult.document.metadata["type"] as? String
        if (contentType != null) {
            val styleBonus = calculateLearningStyleBonus(contentType, profile.learningStyle)
            score *= (1.0 + styleBonus)
        }

        // 根据难度级别调整评分
        val difficulty = searchResult.document.metadata["difficulty"] as? String
        if (difficulty != null) {
            val difficultyBonus = calculateDifficultyBonus(difficulty, profile.currentLevel)
            score *= (1.0 + difficultyBonus)
        }
        
        return min(score, 1.0)
    }
    
    private fun calculateLearningStyleBonus(contentType: String, learningStyle: LearningStyle): Double {
        return when (learningStyle) {
            LearningStyle.VISUAL -> if (contentType in listOf("VIDEO", "IMAGE")) 0.2 else 0.0
            LearningStyle.AUDITORY -> if (contentType in listOf("AUDIO", "VIDEO")) 0.2 else 0.0
            LearningStyle.KINESTHETIC -> if (contentType in listOf("INTERACTIVE", "SIMULATION")) 0.2 else 0.0
            LearningStyle.READING_WRITING -> if (contentType == "TEXT") 0.2 else 0.0
            LearningStyle.BALANCED -> 0.1
        }
    }
    
    private fun calculateDifficultyBonus(difficulty: String, currentLevel: DifficultyLevel): Double {
        // 简化实现：如果难度匹配当前水平，给予奖励
        return if (difficulty == currentLevel.name) 0.15 else 0.0
    }
    
    private fun generateRecommendationReason(
        searchResult: ai.kastrax.store.document.DocumentSearchResult,
        profile: LearningProfile
    ): String {
        val reasons = mutableListOf<String>()
        
        reasons.add("与您的学习目标高度匹配")
        
        val contentType = searchResult.document.metadata["type"] as? String
        if (contentType != null) {
            when (profile.learningStyle) {
                LearningStyle.VISUAL -> if (contentType in listOf("VIDEO", "IMAGE")) {
                    reasons.add("适合您的视觉学习风格")
                }
                LearningStyle.AUDITORY -> if (contentType in listOf("AUDIO", "VIDEO")) {
                    reasons.add("适合您的听觉学习风格")
                }
                LearningStyle.KINESTHETIC -> if (contentType in listOf("INTERACTIVE", "SIMULATION")) {
                    reasons.add("适合您的动手学习风格")
                }
                LearningStyle.READING_WRITING -> if (contentType == "TEXT") {
                    reasons.add("适合您的阅读写作学习风格")
                }
                LearningStyle.BALANCED -> reasons.add("适合您的综合学习风格")
            }
        }
        
        return reasons.joinToString("，")
    }
    
    // 其他辅助方法的简化实现
    private fun calculateEstimatedDuration(learningPath: LearningPath): Int = learningPath.totalEstimatedTime
    private fun calculatePlanDifficulty(learningPath: LearningPath, profile: LearningProfile): DifficultyLevel = learningPath.difficulty
    private fun calculatePathDifficulty(steps: List<LearningStep>): DifficultyLevel = DifficultyLevel.INTERMEDIATE
    private fun estimateObjectiveDifficulty(objective: String, profile: LearningProfile): DifficultyLevel = DifficultyLevel.INTERMEDIATE
    private fun estimateObjectiveTime(objective: String, profile: LearningProfile): Int = 30
    private fun identifyPrerequisites(objective: String, profile: LearningProfile): List<String> = emptyList()
    private fun identifyRequiredSkills(objective: String): List<Skill> = listOf(Skill.LOGICAL_REASONING)
    private fun analyzePerformance(performance: LearningPerformance, feedback: LearningFeedback): PerformanceAnalysis = PerformanceAnalysis("表现良好")
    private fun generateAdaptations(analysis: PerformanceAnalysis): List<PlanAdaptation> = emptyList()
    private fun applyAdaptations(planId: LearningPlanId, adaptations: List<PlanAdaptation>): LearningPlan = LearningPlan.createDefault()
    private fun generateLearningAdvice(profile: LearningProfile, currentActivity: LearningActivity?): LearningAdvice = LearningAdvice.createDefault()
}

// 数据类定义
@Serializable
data class LearningPlanId(val value: String) {
    companion object {
        fun generate(): LearningPlanId = LearningPlanId("plan_${java.util.UUID.randomUUID()}")
    }
}

@Serializable
data class LearningStepId(val value: String) {
    companion object {
        fun generate(): LearningStepId = LearningStepId("step_${java.util.UUID.randomUUID()}")
    }
}

@Serializable
data class AnalyzedObjective(
    val text: String,
    val difficulty: DifficultyLevel,
    val estimatedTime: Int,
    val prerequisites: List<String>,
    val skills: List<Skill>
)

@Serializable
data class LearningStep(
    val id: LearningStepId,
    val objective: String,
    val activityType: ActivityType,
    val estimatedTime: Int,
    val difficulty: DifficultyLevel,
    val order: Int
)

@Serializable
data class LearningPath(
    val steps: List<LearningStep>,
    val totalEstimatedTime: Int,
    val difficulty: DifficultyLevel
)

@Serializable
data class LearningPlan(
    val id: LearningPlanId,
    val studentId: StudentId,
    val courseId: CourseId,
    val objectives: List<AnalyzedObjective>,
    val learningPath: LearningPath,
    val estimatedDuration: Int,
    val difficulty: DifficultyLevel,
    val createdAt: kotlinx.datetime.Instant
) {
    companion object {
        fun createDefault(): LearningPlan = LearningPlan(
            id = LearningPlanId.generate(),
            studentId = StudentId.generate(),
            courseId = CourseId.generate(),
            objectives = emptyList(),
            learningPath = LearningPath(emptyList(), 0, DifficultyLevel.BEGINNER),
            estimatedDuration = 0,
            difficulty = DifficultyLevel.BEGINNER,
            createdAt = Clock.System.now()
        )
    }
}

@Serializable
data class RecommendationContext(
    val currentTopic: Topic? = null,
    val learningObjectives: List<String> = emptyList(),
    val subject: Subject? = null,
    val difficulty: DifficultyLevel = DifficultyLevel.INTERMEDIATE,
    val timeConstraint: Int? = null,
    val excludeContentIds: List<ContentId> = emptyList()
)

@Serializable
data class LearningPerformance(
    val accuracy: Double,
    val completionTime: Int,
    val engagementLevel: Double,
    val difficultyRating: Int
)

@Serializable
data class LearningFeedback(
    val rating: Int,
    val comments: String,
    val suggestions: List<String> = emptyList()
)

@Serializable
data class PerformanceAnalysis(
    val summary: String
)

@Serializable
data class PlanAdaptation(
    val type: String,
    val description: String
)

@Serializable
data class LearningAdvice(
    val recommendations: List<String>,
    val tips: List<String>,
    val warnings: List<String> = emptyList()
) {
    companion object {
        fun createDefault(): LearningAdvice = LearningAdvice(
            recommendations = listOf("继续保持良好的学习习惯"),
            tips = listOf("定期复习已学内容")
        )
    }
}

// 结果类型定义
sealed class LearningPlanResult {
    data class Success(val plan: LearningPlan) : LearningPlanResult()
    data class Failure(val error: String) : LearningPlanResult()
}

sealed class RecommendationResult {
    data class Success(val recommendations: List<ContentRecommendation>) : RecommendationResult()
    data class Failure(val error: String) : RecommendationResult()
}

sealed class PlanAdaptationResult {
    data class Success(
        val adaptedPlan: LearningPlan,
        val adaptations: List<PlanAdaptation>,
        val reason: String
    ) : PlanAdaptationResult()
    data class Failure(val error: String) : PlanAdaptationResult()
}

sealed class LearningAdviceResult {
    data class Success(val advice: LearningAdvice) : LearningAdviceResult()
    data class Failure(val error: String) : LearningAdviceResult()
}
