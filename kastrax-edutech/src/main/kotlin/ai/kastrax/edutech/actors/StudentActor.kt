package ai.kastrax.edutech.actors

import ai.kastrax.memory.api.Memory
import ai.kastrax.rag.RAG
import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.services.*
import kotlinx.datetime.Instant
import mu.KotlinLogging

/**
 * 简化的学生Actor - 实现ed2.md第2.1节Actor模型在教育场景的应用
 * 
 * 管理单个学生的学习状态，实现分布式学习管理
 */
class StudentActor(
    private val studentId: StudentId,
    private val memorySystem: Memory,
    private val ragSystem: RAG,
    private val learningAnalytics: LearningAnalytics,
    private val personalizationEngine: PersonalizationEngine
) {
    
    private val logger = KotlinLogging.logger {}
    
    // 学生当前状态
    private var currentSession: LearningSession? = null
    private var learningState = LearningState.initial(studentId)
    private var personalizedPlan = PersonalizedLearningPlan.empty(studentId)
    
    suspend fun receive(msg: Any): Message? {
        logger.debug { "StudentActor[$studentId] received message: ${msg::class.simpleName}" }
        
        return try {
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
                else -> {
                    logger.warn { "Unknown message type: ${msg::class.simpleName}" }
                    null
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error processing message in StudentActor[$studentId]" }
            throw LearningDataException("Failed to process learning activity", e)
        }
    }
    
    private suspend fun handleStartSession(message: StartLearningSession): Message {
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
            message = SimpleMessage(
                content = "Started learning session: ${currentSession!!.id}",
                role = ai.kastrax.memory.api.MessageRole.SYSTEM
            ),
            threadId = studentId.toString()
        )
        
        // 生成个性化学习计划
        personalizedPlan = personalizationEngine.generateLearningPlan(
            studentId = studentId,
            objectives = message.objectives,
            currentState = learningState
        )
        
        // 返回确认消息
        return SessionStarted(currentSession!!.id, personalizedPlan.nextRecommendations)
    }
    
    private suspend fun handleLearningActivity(message: ProcessLearningActivity): Message {
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
            message = SimpleMessage(
                content = "Completed activity: ${processedActivity.type} - Performance: ${processedActivity.performance}%",
                role = ai.kastrax.memory.api.MessageRole.SYSTEM
            ),
            threadId = studentId.toString()
        )
        
        // 生成反馈和下一步建议
        val feedback = generateLearningFeedback(processedActivity, performanceAnalysis)
        val nextRecommendation = personalizedPlan.getNextRecommendation()
        
        // 返回处理结果
        return ActivityProcessed(
            activityId = processedActivity.id,
            performance = processedActivity.performance,
            feedback = feedback,
            nextRecommendation = nextRecommendation,
            updatedMetrics = currentSession!!.sessionMetrics
        )
    }
    
    private suspend fun handlePersonalizationUpdate(message: UpdatePersonalization): Message {
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
            message = SimpleMessage(
                content = "Updated personalization: ${message.profileUpdates}",
                role = ai.kastrax.memory.api.MessageRole.SYSTEM
            ),
            threadId = studentId.toString()
        )
        
        return PersonalizationUpdated(personalizedPlan.summary)
    }
    
    private suspend fun handleProgressQuery(message: GetLearningProgress): Message {
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
            nextMilestones = personalizedPlan.upcomingMilestones
        )
        
        return progressReport
    }
    
    private suspend fun handleRecommendationRequest(message: GetRecommendations): Message {
        logger.debug { "Generating recommendations for student $studentId" }
        
        // 使用RAG系统检索相关学习资源
        val relevantResources = ragSystem.search(
            query = buildRecommendationQuery(message.context, learningState),
            limit = 10
        )
        
        // 生成个性化推荐
        val recommendations = personalizationEngine.generateRecommendations(
            studentId = studentId,
            context = message.context,
            learningState = learningState,
            availableResources = relevantResources
        )
        
        return RecommendationsGenerated(recommendations)
    }
    
    private suspend fun handlePauseSession(message: PauseLearningSession): Message {
        currentSession = currentSession?.copy(status = SessionStatus.PAUSED)
        return SessionPaused(currentSession!!.id)
    }
    
    private suspend fun handleResumeSession(message: ResumeLearningSession): Message {
        currentSession = currentSession?.copy(status = SessionStatus.ACTIVE)
        return SessionResumed(currentSession!!.id)
    }
    
    private suspend fun handleCompleteSession(message: CompleteLearningSession): Message {
        currentSession = currentSession?.complete()
        learningState = learningState.completeSession(currentSession!!)
        return SessionCompleted(currentSession!!.id, currentSession!!.sessionMetrics)
    }
    
    private suspend fun handleGoalUpdate(message: UpdateLearningGoals): Message {
        personalizedPlan = personalizedPlan.updateGoals(message.newGoals)
        return GoalsUpdated(message.newGoals)
    }
    
    private suspend fun handleMetacognitionRecord(message: RecordMetacognition): Message {
        learningState = learningState.recordMetacognition(message.reflection)
        return MetacognitionRecorded(message.reflection.id)
    }
    
    // 辅助方法
    private suspend fun processActivity(activity: LearningActivity): LearningActivity {
        // 简化的活动处理逻辑
        val performance = calculateActivityPerformance(activity, learningState)
        val feedback = generateActivityFeedback(activity, performance)
        return activity.complete(performance, feedback)
    }
    
    private fun calculateActivityPerformance(activity: LearningActivity, state: LearningState): Double {
        // 基于学生能力和活动难度计算表现
        val basePerformance = when (activity.difficulty) {
            DifficultyLevel.BEGINNER -> 85.0
            DifficultyLevel.ELEMENTARY -> 80.0
            DifficultyLevel.INTERMEDIATE -> 75.0
            DifficultyLevel.ADVANCED -> 70.0
            DifficultyLevel.EXPERT -> 65.0
        }
        
        // 根据学习风格调整
        val styleMultiplier = state.learningProfile.learningStyle.getMultiplierFor(ContentType.TEXT)
        
        // 添加随机变化模拟真实表现
        val randomVariation = Random.nextInt(1, 100)Random.nextInt()
        
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
    
    private fun generateLearningFeedback(activity: LearningActivity, analysis: PerformanceAnalysis): LearningFeedback {
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
    
    private fun buildRecommendationQuery(context: RecommendationContext, state: LearningState): String {
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
}

// 简单的Message实现
data class SimpleMessage(
    override val content: String,
    override val role: ai.kastrax.memory.api.MessageRole,
    override val name: String? = null,
    override val toolCalls: List<ai.kastrax.memory.api.ToolCall> = emptyList(),
    override val toolCallId: String? = null
) : ai.kastrax.memory.api.Message

// 异常类定义
class LearningDataException(message: String, cause: Throwable? = null) : Exception(message, cause)
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
class CriticalLearningException(message: String, cause: Throwable? = null) : Exception(message, cause)
