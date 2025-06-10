package ai.kastrax.edutech.services

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.actors.*
import kotlinx.datetime.Instant

/**
 * 教育科技服务接口定义
 * 
 * 实现ed2.md第3.1节服务层架构
 */

// ============= 学习分析服务 =============

interface LearningAnalytics {
    suspend fun analyzePerformance(
        studentId: StudentId,
        activity: LearningActivity,
        historicalData: List<LearningActivity>
    ): PerformanceAnalysis
    
    suspend fun getStudentProfile(studentId: StudentId): StudentProfile
    
    suspend fun identifyImprovementAreas(learningState: LearningState): List<ImprovementArea>
    
    suspend fun getStudentKnowledgeState(studentId: StudentId, topic: String): KnowledgeState
    
    suspend fun updateKnowledgeState(
        studentId: StudentId,
        topic: String,
        performance: PerformanceMetrics
    )
    
    suspend fun analyzeLearningPattern(studentId: StudentId, activity: LearningActivity)
}

// ============= 个性化引擎服务 =============

interface PersonalizationEngine {
    suspend fun generateLearningPlan(
        studentId: StudentId,
        objectives: List<String>,
        currentState: LearningState
    ): PersonalizedLearningPlan
    
    suspend fun adaptPlan(
        currentPlan: PersonalizedLearningPlan,
        performanceAnalysis: PerformanceAnalysis,
        learningState: LearningState
    ): PersonalizedLearningPlan
    
    suspend fun regeneratePlan(
        studentId: StudentId,
        updatedState: LearningState,
        newPreferences: LearningPreferences
    ): PersonalizedLearningPlan
    
    suspend fun generateRecommendations(
        studentId: StudentId,
        context: RecommendationContext,
        learningState: LearningState,
        availableResources: List<Any>
    ): List<LearningRecommendation>
}

// ============= 内容生成服务 =============

interface ContentGenerationService {
    suspend fun generateContent(request: ContentGenerationRequest): GeneratedContent
}

// ============= 班级管理服务 =============

interface ClassManagementService {
    suspend fun addClass(classInfo: ClassInfo): ClassroomId
    suspend fun removeStudent(classId: ClassroomId, studentId: StudentId)
    suspend fun addStudent(classId: ClassroomId, studentId: StudentId)
}

// ============= 数据模型 =============

data class PerformanceAnalysis(
    val overallScore: Double,
    val identifiedStrengths: List<String>,
    val identifiedWeaknesses: List<String>,
    val improvementSuggestions: List<String>,
    val recommendedNextSteps: List<String>,
    val learningVelocity: Double,
    val difficultyAdjustment: Double
)

data class StudentProfile(
    val studentId: StudentId,
    val learningStyle: LearningStyle,
    val currentLevel: DifficultyLevel,
    val preferredStyle: LearningStyle,
    val gradeLevel: GradeLevel,
    val currentSubject: String,
    val knowledgeLevel: Map<Topic, MasteryLevel>,
    val preferredContentTypes: Set<ContentType>,
    val currentDifficultyLevel: DifficultyLevel,
    val difficultyRange: IntRange = 1..5
)

data class LearningState(
    val studentId: StudentId,
    val currentSession: LearningSession?,
    val learningProfile: LearningProfile,
    val currentDifficultyLevel: DifficultyLevel,
    val knowledgeMap: Map<Topic, MasteryLevel>,
    val recentActivities: List<LearningActivity>,
    val overallProgress: Double
) {
    companion object {
        fun initial(studentId: StudentId): LearningState = LearningState(
            studentId = studentId,
            currentSession = null,
            learningProfile = LearningProfile.default(),
            currentDifficultyLevel = DifficultyLevel.BEGINNER,
            knowledgeMap = emptyMap(),
            recentActivities = emptyList(),
            overallProgress = 0.0
        )
    }
    
    fun startNewSession(session: LearningSession): LearningState = copy(currentSession = session)
    
    fun updateFromActivity(activity: LearningActivity): LearningState = copy(
        recentActivities = (recentActivities + activity).takeLast(10),
        overallProgress = calculateNewProgress(activity)
    )
    
    fun updateProfile(updates: Map<String, String>): LearningState = copy(
        learningProfile = learningProfile.copy(
            learningStyle = updates["learningStyle"]?.let { LearningStyle.valueOf(it) } ?: learningProfile.learningStyle
        )
    )
    
    fun completeSession(session: LearningSession): LearningState = copy(currentSession = session)
    
    fun recordMetacognition(reflection: MetacognitiveReflection): LearningState = this
    
    fun calculateOverallProgress(): OverallProgress = OverallProgress(
        completionPercentage = overallProgress,
        averagePerformance = recentActivities.map { it.performance }.average().takeIf { !it.isNaN() } ?: 0.0,
        totalTimeSpent = recentActivities.map { it.timeSpent }.fold(kotlin.time.Duration.ZERO) { acc, duration -> acc + duration },
        activitiesCompleted = recentActivities.size,
        currentStreak = 0,
        level = 1,
        experiencePoints = 0
    )
    
    fun getSubjectProgress(): Map<Subject, SubjectProgress> = emptyMap()
    fun getSkillDevelopment(): Map<Skill, SkillProgress> = emptyMap()
    fun getRecentActivities(limit: Int): List<LearningActivity> = recentActivities.take(limit)
    fun getAchievements(): List<Achievement> = emptyList()
    fun getHistoricalPerformance(): List<LearningActivity> = recentActivities
    
    private fun calculateNewProgress(activity: LearningActivity): Double {
        return (overallProgress + activity.performance / 100.0) / 2.0
    }
}

data class PersonalizedLearningPlan(
    val studentId: StudentId,
    val objectives: List<String>,
    val nextRecommendations: List<LearningRecommendation>,
    val summary: String,
    val upcomingMilestones: List<Milestone>
) {
    companion object {
        fun empty(studentId: StudentId): PersonalizedLearningPlan = PersonalizedLearningPlan(
            studentId = studentId,
            objectives = emptyList(),
            nextRecommendations = emptyList(),
            summary = "Empty plan",
            upcomingMilestones = emptyList()
        )
    }
    
    fun getNextRecommendation(): LearningRecommendation? = nextRecommendations.firstOrNull()
    fun getUpcomingMilestones(): List<Milestone> = upcomingMilestones
    fun updateGoals(newGoals: List<LearningGoal>): PersonalizedLearningPlan = this
}

data class KnowledgeState(
    val topics: Map<String, Double>,
    val skills: Map<String, Double>,
    val overallLevel: Double
)

data class PerformanceMetrics(
    val accuracy: Double,
    val speed: Double,
    val consistency: Double
)

data class ContentGenerationRequest(
    val type: ContentType,
    val subject: Subject,
    val difficulty: DifficultyLevel,
    val objectives: List<String>,
    val targetAudience: String,
    val constraints: ContentConstraints,
    val context: List<Any>
)

data class GeneratedContent(
    val id: String,
    val title: String,
    val content: String,
    val type: ContentType,
    val metadata: Map<String, String>
)

data class ClassInfo(
    val name: String,
    val description: String,
    val teacherId: TeacherId
)

// ============= ID类型 =============

@JvmInline
value class TeacherId(val value: String) {
    companion object {
        fun generate(): TeacherId = TeacherId("teacher_${java.util.UUID.randomUUID()}")
    }
    override fun toString(): String = value
}

@JvmInline
value class ClassroomId(val value: String) {
    companion object {
        fun generate(): ClassroomId = ClassroomId("classroom_${java.util.UUID.randomUUID()}")
    }
    override fun toString(): String = value
}

// ============= 分析结果类型 =============

sealed class AnalysisResult {
    data class OverallPerformance(
        val averagePerformance: Double,
        val completionRate: Double,
        val totalActivities: Int,
        val studentCount: Int,
        val performanceDistribution: Map<String, Int>
    ) : AnalysisResult()
    
    data class IndividualProgress(
        val analyses: List<IndividualAnalysis>
    ) : AnalysisResult()
    
    data class SubjectPerformance(
        val subjectPerformances: Map<Subject, SubjectAnalysis>
    ) : AnalysisResult()
    
    data class SkillDevelopment(
        val skillAnalyses: Map<String, Any>
    ) : AnalysisResult()
    
    data class EngagementMetrics(
        val metrics: Map<String, Any>
    ) : AnalysisResult()
    
    data class PredictiveAnalysis(
        val predictions: List<Any>
    ) : AnalysisResult()
}

data class IndividualAnalysis(
    val studentId: StudentId,
    val progressTrend: String,
    val strengthAreas: List<String>,
    val improvementAreas: List<String>,
    val recommendedActions: List<String>
)

data class SubjectAnalysis(
    val subject: Subject,
    val averagePerformance: Double,
    val completionRate: Double,
    val totalTimeSpent: kotlin.time.Duration,
    val strugglingStudents: List<StudentId>,
    val excellingStudents: List<StudentId>
)

data class ClassImprovement(
    val area: String,
    val description: String,
    val priority: Priority,
    val suggestedActions: List<String>,
    val estimatedImpact: ImpactLevel
)

enum class ImpactLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class StudentProgressData(
    val studentId: StudentId,
    val progressReport: LearningProgressReport,
    val timestamp: Instant
)

// ============= 其他支持类型 =============

data class CourseContent(
    val classroomId: ClassroomId,
    val contents: List<GeneratedContent>
) {
    companion object {
        fun empty(classroomId: ClassroomId): CourseContent = CourseContent(classroomId, emptyList())
    }
    
    fun addContent(content: GeneratedContent): CourseContent = copy(contents = contents + content)
}

data class ClassAnalytics(
    val classroomId: ClassroomId,
    val students: Set<StudentId>,
    val overallPerformance: Double
) {
    companion object {
        fun initial(classroomId: ClassroomId): ClassAnalytics = ClassAnalytics(
            classroomId = classroomId,
            students = emptySet(),
            overallPerformance = 0.0
        )
    }
    
    fun addStudent(studentId: StudentId): ClassAnalytics = copy(students = students + studentId)
    fun removeStudent(studentId: StudentId): ClassAnalytics = copy(students = students - studentId)
    fun updateWith(result: AnalysisResult): ClassAnalytics = this
    fun getOverallPerformance(): Any = overallPerformance
}
