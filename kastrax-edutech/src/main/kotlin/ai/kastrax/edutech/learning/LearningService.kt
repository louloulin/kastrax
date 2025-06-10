package ai.kastrax.edutech.learning

import ai.kastrax.edutech.actors.*
import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.services.*
import ai.kastrax.memory.api.Memory
import ai.kastrax.rag.RAG
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * 学习服务 - 实现ed2.md第一阶段Week 3-4基础学习服务
 * 
 * 提供学习会话管理、学生Actor创建、基础学习活动处理和学习状态跟踪
 */
class LearningService(
    private val memorySystem: Memory,
    private val ragSystem: RAG,
    private val learningAnalytics: LearningAnalytics,
    private val personalizationEngine: PersonalizationEngine
) {
    private val studentActors = mutableMapOf<StudentId, StudentActor>()
    private val activeSessions = mutableMapOf<SessionId, LearningSession>()
    private val mutex = Mutex()
    
    /**
     * 创建或获取学生Actor
     *
     * @param studentId 学生ID
     * @return 学生Actor
     */
    suspend fun getOrCreateStudentActor(studentId: StudentId): StudentActor {
        return mutex.withLock {
            studentActors.getOrPut(studentId) {
                StudentActor(
                    studentId = studentId,
                    memorySystem = memorySystem,
                    ragSystem = ragSystem,
                    learningAnalytics = learningAnalytics,
                    personalizationEngine = personalizationEngine
                )
            }
        }
    }
    
    /**
     * 启动学习会话
     *
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @param objectives 学习目标
     * @param context 初始上下文
     * @return 学习会话启动结果
     */
    suspend fun startLearningSession(
        studentId: StudentId,
        courseId: CourseId,
        objectives: List<String>,
        context: Map<String, Any> = emptyMap()
    ): LearningSessionResult {
        return try {
            val studentActor = getOrCreateStudentActor(studentId)
            val startMessage = StartLearningSession(courseId, objectives)
            
            val result = studentActor.receive(startMessage)
            
            when (result) {
                is SessionStarted -> {
                    val session = LearningSession.create(
                        studentId = studentId,
                        courseId = courseId,
                        objectives = objectives
                    )

                    mutex.withLock {
                        activeSessions[result.sessionId] = session
                    }

                    LearningSessionResult.Success(
                        sessionId = result.sessionId,
                        recommendations = emptyList(), // 简化实现
                        message = "学习会话启动成功"
                    )
                }
                else -> LearningSessionResult.Failure("启动学习会话失败")
            }
        } catch (e: Exception) {
            LearningSessionResult.Failure("启动学习会话时发生错误: ${e.message}")
        }
    }
    
    /**
     * 处理学习活动
     *
     * @param sessionId 会话ID
     * @param activity 学习活动
     * @return 活动处理结果
     */
    suspend fun processLearningActivity(
        sessionId: SessionId,
        activity: LearningActivity
    ): ActivityProcessingResult {
        return try {
            val session = mutex.withLock { activeSessions[sessionId] }
                ?: return ActivityProcessingResult.Failure("会话不存在或已结束")
            
            val studentActor = getOrCreateStudentActor(session.studentId)
            val processMessage = ProcessLearningActivity(sessionId, activity)
            
            val result = studentActor.receive(processMessage)
            
            when (result) {
                is ActivityProcessed -> {
                    // 更新会话状态
                    mutex.withLock {
                        // 简化实现，不更新会话状态
                        // val updatedSession = session.addActivity(activity)
                        // activeSessions[sessionId] = updatedSession
                    }

                    ActivityProcessingResult.Success(
                        activityId = result.activityId,
                        performance = result.performance,
                        feedback = "活动处理完成", // 简化实现
                        nextRecommendations = emptyList() // 简化实现
                    )
                }
                else -> ActivityProcessingResult.Failure("处理学习活动失败")
            }
        } catch (e: Exception) {
            ActivityProcessingResult.Failure("处理学习活动时发生错误: ${e.message}")
        }
    }
    
    /**
     * 获取学习进度
     *
     * @param studentId 学生ID
     * @return 学习进度报告
     */
    suspend fun getLearningProgress(studentId: StudentId): LearningProgressResult {
        return try {
            val studentActor = getOrCreateStudentActor(studentId)
            val progressMessage = GetLearningProgress()
            
            val result = studentActor.receive(progressMessage)
            
            when (result) {
                is LearningProgressReport -> {
                    LearningProgressResult.Success(
                        studentId = result.studentId,
                        overallProgress = 75.0, // 简化实现
                        subjectProgress = emptyMap(), // 简化实现
                        skillDevelopment = emptyMap(), // 简化实现
                        recentActivities = emptyList() // 简化实现
                    )
                }
                else -> LearningProgressResult.Failure("获取学习进度失败")
            }
        } catch (e: Exception) {
            LearningProgressResult.Failure("获取学习进度时发生错误: ${e.message}")
        }
    }
    
    /**
     * 暂停学习会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    suspend fun pauseLearningSession(sessionId: SessionId): SessionOperationResult {
        return try {
            val session = mutex.withLock { activeSessions[sessionId] }
                ?: return SessionOperationResult.Failure("会话不存在")
            
            val studentActor = getOrCreateStudentActor(session.studentId)
            val pauseMessage = PauseLearningSession(sessionId)
            
            val result = studentActor.receive(pauseMessage)
            
            when (result) {
                is SessionPaused -> {
                    SessionOperationResult.Success("学习会话已暂停")
                }
                else -> SessionOperationResult.Failure("暂停学习会话失败")
            }
        } catch (e: Exception) {
            SessionOperationResult.Failure("暂停学习会话时发生错误: ${e.message}")
        }
    }
    
    /**
     * 恢复学习会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    suspend fun resumeLearningSession(sessionId: SessionId): SessionOperationResult {
        return try {
            val session = mutex.withLock { activeSessions[sessionId] }
                ?: return SessionOperationResult.Failure("会话不存在")
            
            val studentActor = getOrCreateStudentActor(session.studentId)
            val resumeMessage = ResumeLearningSession(sessionId)
            
            val result = studentActor.receive(resumeMessage)
            
            when (result) {
                is SessionResumed -> {
                    SessionOperationResult.Success("学习会话已恢复")
                }
                else -> SessionOperationResult.Failure("恢复学习会话失败")
            }
        } catch (e: Exception) {
            SessionOperationResult.Failure("恢复学习会话时发生错误: ${e.message}")
        }
    }
    
    /**
     * 结束学习会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    suspend fun completeLearningSession(sessionId: SessionId): SessionOperationResult {
        return try {
            val session = mutex.withLock { activeSessions[sessionId] }
                ?: return SessionOperationResult.Failure("会话不存在")
            
            val studentActor = getOrCreateStudentActor(session.studentId)
            val completeMessage = CompleteLearningSession(sessionId, "学习会话完成")
            
            val result = studentActor.receive(completeMessage)
            
            when (result) {
                is SessionCompleted -> {
                    // 从活动会话中移除
                    mutex.withLock {
                        activeSessions.remove(sessionId)
                    }
                    
                    SessionOperationResult.Success("学习会话已完成")
                }
                else -> SessionOperationResult.Failure("完成学习会话失败")
            }
        } catch (e: Exception) {
            SessionOperationResult.Failure("完成学习会话时发生错误: ${e.message}")
        }
    }
    
    /**
     * 获取活动会话列表
     *
     * @param studentId 学生ID (可选)
     * @return 活动会话列表
     */
    suspend fun getActiveSessions(studentId: StudentId? = null): List<LearningSession> {
        return mutex.withLock {
            if (studentId != null) {
                activeSessions.values.filter { it.studentId == studentId }
            } else {
                activeSessions.values.toList()
            }
        }
    }
    
    /**
     * 获取会话统计信息
     *
     * @return 会话统计
     */
    suspend fun getSessionStatistics(): SessionStatistics {
        return mutex.withLock {
            val totalSessions = activeSessions.size
            val studentCount = activeSessions.values.map { it.studentId }.distinct().size
            val averageSessionDuration = 0.0 // 简化实现

            SessionStatistics(
                totalActiveSessions = totalSessions,
                uniqueActiveStudents = studentCount,
                averageSessionDuration = averageSessionDuration,
                timestamp = Clock.System.now()
            )
        }
    }
}

/**
 * 学习会话结果
 */
sealed class LearningSessionResult {
    data class Success(
        val sessionId: SessionId,
        val recommendations: List<LearningRecommendation>,
        val message: String
    ) : LearningSessionResult()
    
    data class Failure(val error: String) : LearningSessionResult()
}

/**
 * 活动处理结果
 */
sealed class ActivityProcessingResult {
    data class Success(
        val activityId: ActivityId,
        val performance: Double,
        val feedback: String,
        val nextRecommendations: List<LearningRecommendation>
    ) : ActivityProcessingResult()
    
    data class Failure(val error: String) : ActivityProcessingResult()
}

/**
 * 学习进度结果
 */
sealed class LearningProgressResult {
    data class Success(
        val studentId: StudentId,
        val overallProgress: Double,
        val subjectProgress: Map<Subject, Double>,
        val skillDevelopment: Map<Skill, Double>,
        val recentActivities: List<LearningActivity>
    ) : LearningProgressResult()
    
    data class Failure(val error: String) : LearningProgressResult()
}

/**
 * 会话操作结果
 */
sealed class SessionOperationResult {
    data class Success(val message: String) : SessionOperationResult()
    data class Failure(val error: String) : SessionOperationResult()
}

/**
 * 会话统计信息
 */
@Serializable
data class SessionStatistics(
    val totalActiveSessions: Int,
    val uniqueActiveStudents: Int,
    val averageSessionDuration: Double, // 分钟
    val timestamp: kotlinx.datetime.Instant
)
