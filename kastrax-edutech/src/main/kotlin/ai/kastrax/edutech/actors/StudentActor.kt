package ai.kastrax.edutech.actors

import ai.kastrax.memory.api.Memory
import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.services.LearningAnalytics
import ai.kastrax.edutech.services.PersonalizationEngine
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.time.Duration

/**
 * 学生Actor - 实现ed2.md第2.1节Actor模型在教育场景的应用
 *
 * 管理单个学生的学习状态，实现真正的分布式学习管理
 */
class StudentActor(
    private val studentId: StudentId,
    private val memorySystem: Memory,
    private val ragSystem: RAGSystem,
    private val learningAnalytics: LearningAnalytics,
    private val personalizationEngine: PersonalizationEngine
) : Actor {
    
    private val logger = KotlinLogging.logger {}
    
    // 学生当前状态
    private var currentSession: LearningSession? = null
    private var learningState = LearningState.initial(studentId)
    private var personalizedPlan = PersonalizedLearningPlan.empty(studentId)
    
    override suspend fun Context.receive(msg: Any) {
        logger.debug { "StudentActor[$studentId] received message: ${msg::class.simpleName}" }

        try {
            when (msg) {
                is StartLearningSession -> handleStartSession(msg)
                is ProcessLearningActivity -> handleLearningActivity(msg)
                is UpdatePersonalization -> handlePersonalizationUpdate(msg)
                is GetLearningProgress -> handleProgressQuery(msg)
                is GetRecommendations -> handleRecommendationRequest(msg)
                is PauseLearningSession -> handlePauseSession(msg)
                is ResumeLearningSession -> handleResumeSession(msg)
                is CompleteLearningSession -> handleCompleteSession(msg)
                is UpdateLearningGoals -> handleGoalUpdate(msg)
                is RecordMetacognition -> handleMetacognitionRecord(msg)
                else -> logger.warn { "Unknown message type: ${message::class.simpleName}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error processing message in StudentActor[$studentId]" }
            throw LearningDataException("Failed to process learning activity", e)
        }
    }
    
    private suspend fun Context.handleStartSession(message: StartLearningSession) {
        logger.info { "Starting learning session for student $studentId" }
        
        // 创建新的学习会话
        currentSession = LearningSession.create(
            studentId = studentId,
            courseId = message.courseId,
            objectives = message.objectives
        )
        
        // 更新学习状态
        learningState = learningState.startNewSession(currentSession!!)
        
        // 保存到记忆系统
        memorySystem.saveMessage(
            message = ai.kastrax.memory.api.Message(
                content = "Started learning session: ${currentSession!!.id}",
                role = "system"
            ),
            threadId = studentId.toString()
        )
        
        // 生成个性化学习计划
        personalizedPlan = personalizationEngine.generateLearningPlan(
            studentId = studentId,
            objectives = message.objectives,
            currentState = learningState
        )
        
        // 回复确认
        respond(SessionStarted(currentSession!!.id, personalizedPlan.nextRecommendations))
    }
    
    private suspend fun Context.handleLearningActivity(message: ProcessLearningActivity) {
        val session = currentSession ?: throw IllegalStateException("No active session")
        
        logger.debug { "Processing learning activity: ${message.activity.type}" }
        
        // 处理学习活动
        val processedActivity = processActivity(message.activity)
        
        // 更新会话
        currentSession = session.addActivity(processedActivity)
        
        // 更新学习状态
        learningState = learningState.updateFromActivity(processedActivity)
        
        // 分析学习表现
        val performanceAnalysis = learningAnalytics.analyzePerformance(
            studentId = studentId,
            activity = processedActivity,
            historicalData = learningState.getHistoricalPerformance()
        )
        
        // 更新个性化计划
        personalizedPlan = personalizationEngine.adaptPlan(
            currentPlan = personalizedPlan,
            performanceAnalysis = performanceAnalysis,
            learningState = learningState
        )
        
        // 保存学习记录到记忆系统
        memorySystem.saveMessage(
            message = ai.kastrax.memory.api.Message(
                content = "Completed activity: ${processedActivity.type} - Performance: ${processedActivity.performance}%",
                role = "system"
            ),
            threadId = studentId.toString()
        )
        
        // 生成反馈和下一步建议
        val feedback = generateLearningFeedback(processedActivity, performanceAnalysis)
        val nextRecommendation = personalizedPlan.getNextRecommendation()
        
        // 回复处理结果
        respond(
            ActivityProcessed(
                activityId = processedActivity.id,
                performance = processedActivity.performance,
                feedback = feedback,
                nextRecommendation = nextRecommendation,
                updatedMetrics = currentSession!!.sessionMetrics
            )
        )
    }
    
    private suspend fun Context.handlePersonalizationUpdate(message: UpdatePersonalization) {
        logger.debug { "Updating personalization for student $studentId" }
        
        // 更新学习档案
        learningState = learningState.updateProfile(message.profileUpdates)
        
        // 重新生成个性化计划
        personalizedPlan = personalizationEngine.regeneratePlan(
            studentId = studentId,
            updatedState = learningState,
            newPreferences = message.preferences
        )
        
        // 保存更新到记忆系统
        memorySystem.saveMessage(
            message = ai.kastrax.memory.api.Message(
                content = "Updated personalization: ${msg.profileUpdates}",
                role = "system"
            ),
            threadId = studentId.toString()
        )

        respond(PersonalizationUpdated(personalizedPlan.summary))
    }
    
    private suspend fun Context.handleProgressQuery(message: GetLearningProgress) {
        logger.debug { "Generating progress report for student $studentId" }
        
        val progressReport = LearningProgressReport(
            studentId = studentId,
            currentSession = currentSession,
            overallProgress = learningState.calculateOverallProgress(),
            subjectProgress = learningState.getSubjectProgress(),
            skillDevelopment = learningState.getSkillDevelopment(),
            recentActivities = learningState.getRecentActivities(limit = 10),
            achievements = learningState.getAchievements(),
            areasForImprovement = learningAnalytics.identifyImprovementAreas(learningState),
            nextMilestones = personalizedPlan.getUpcomingMilestones()
        )
        
        respond(progressReport)
    }
    
    private suspend fun Context.handleRecommendationRequest(message: GetRecommendations) {
        logger.debug { "Generating recommendations for student $studentId" }
        
        // 使用RAG系统检索相关学习资源
        val relevantResources = ragSystem.search(
            query = buildRecommendationQuery(message.context, learningState),
            filters = mapOf(
                "difficulty" to learningState.currentDifficultyLevel.name,
                "subject" to message.context.subject?.name,
                "learningStyle" to learningState.learningProfile.learningStyle.name
            ),
            limit = 10
        )
        
        // 生成个性化推荐
        val recommendations = personalizationEngine.generateRecommendations(
            studentId = studentId,
            context = message.context,
            learningState = learningState,
            availableResources = relevantResources
        )
        
        respond(RecommendationsGenerated(recommendations))
    }
    
    private suspend fun processActivity(activity: LearningActivity): LearningActivity {
        // 记录活动开始
        val startTime = kotlinx.datetime.Clock.System.now()
        
        // 模拟活动处理逻辑
        // 在实际实现中，这里会调用具体的学习活动处理器
        
        // 计算表现分数（这里是简化版本）
        val performance = calculateActivityPerformance(activity, learningState)
        
        // 生成反馈
        val feedback = generateActivityFeedback(activity, performance)
        
        return activity.complete(performance, feedback)
    }
    
    private fun calculateActivityPerformance(
        activity: LearningActivity, 
        state: LearningState
    ): Double {
        // 基于学生能力和活动难度计算表现
        val basePerformance = when (activity.difficulty) {
            DifficultyLevel.BEGINNER -> 85.0
            DifficultyLevel.ELEMENTARY -> 80.0
            DifficultyLevel.INTERMEDIATE -> 75.0
            DifficultyLevel.ADVANCED -> 70.0
            DifficultyLevel.EXPERT -> 65.0
        }
        
        // 根据学习风格调整
        val styleMultiplier = state.learningProfile.learningStyle.getMultiplierFor(
            ContentType.TEXT // 简化处理，实际应根据活动类型确定
        )
        
        // 添加随机变化模拟真实表现
        val randomVariation = (-10..10).random()
        
        return (basePerformance * styleMultiplier + randomVariation).coerceIn(0.0, 100.0)
    }
    
    private fun generateActivityFeedback(activity: LearningActivity, performance: Double): String {
        return when {
            performance >= 90 -> "优秀！你在${activity.topic}方面表现出色。"
            performance >= 80 -> "很好！继续保持这种学习状态。"
            performance >= 70 -> "不错，但还有提升空间。建议多练习相关技能。"
            performance >= 60 -> "需要加强练习。建议复习基础概念。"
            else -> "建议寻求帮助，重新学习基础知识。"
        }
    }
    
    private fun generateLearningFeedback(
        activity: LearningActivity,
        analysis: PerformanceAnalysis
    ): LearningFeedback {
        return LearningFeedback(
            activityId = activity.id,
            overallScore = activity.performance,
            strengths = analysis.identifiedStrengths,
            weaknesses = analysis.identifiedWeaknesses,
            suggestions = analysis.improvementSuggestions,
            encouragement = generateEncouragement(activity.performance),
            nextSteps = analysis.recommendedNextSteps
        )
    }
    
    private fun generateEncouragement(performance: Double): String {
        return when {
            performance >= 90 -> "你的表现非常出色！继续保持这种学习热情。"
            performance >= 80 -> "很棒的进步！你正在稳步提升。"
            performance >= 70 -> "你正在进步中，坚持下去！"
            performance >= 60 -> "每一次尝试都是进步，不要放弃！"
            else -> "学习是一个过程，相信自己能够做得更好！"
        }
    }
    
    private fun buildRecommendationQuery(
        context: RecommendationContext,
        state: LearningState
    ): String {
        return buildString {
            append("学习推荐查询: ")
            context.subject?.let { append("学科=${it.displayName} ") }
            append("难度=${state.currentDifficultyLevel.displayName} ")
            append("学习风格=${state.learningProfile.learningStyle.description} ")
            context.specificTopics.takeIf { it.isNotEmpty() }?.let {
                append("主题=${it.joinToString(",")} ")
            }
        }
    }
    
    // 其他处理方法的简化实现
    private suspend fun Context.handlePauseSession(message: PauseLearningSession) {
        currentSession = currentSession?.copy(status = SessionStatus.PAUSED)
        respond(SessionPaused(currentSession!!.id))
    }

    private suspend fun Context.handleResumeSession(message: ResumeLearningSession) {
        currentSession = currentSession?.copy(status = SessionStatus.ACTIVE)
        respond(SessionResumed(currentSession!!.id))
    }

    private suspend fun Context.handleCompleteSession(message: CompleteLearningSession) {
        currentSession = currentSession?.complete()
        learningState = learningState.completeSession(currentSession!!)
        respond(SessionCompleted(currentSession!!.id, currentSession!!.sessionMetrics))
    }

    private suspend fun Context.handleGoalUpdate(message: UpdateLearningGoals) {
        personalizedPlan = personalizedPlan.updateGoals(message.newGoals)
        respond(GoalsUpdated(message.newGoals))
    }

    private suspend fun Context.handleMetacognitionRecord(message: RecordMetacognition) {
        learningState = learningState.recordMetacognition(message.reflection)
        respond(MetacognitionRecorded(message.reflection.id))
    }
}

// 异常类定义
class LearningDataException(message: String, cause: Throwable? = null) : Exception(message, cause)
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
class CriticalLearningException(message: String, cause: Throwable? = null) : Exception(message, cause)
