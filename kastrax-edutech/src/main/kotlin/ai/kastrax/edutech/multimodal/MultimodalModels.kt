package ai.kastrax.edutech.multimodal

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * 多模态智能教学助手数据模型
 * Week 21-22 扩展功能
 */

// 语音交互相关
@Serializable
data class AudioInput(
    val audioData: ByteArray,
    val format: String = "wav",
    val sampleRate: Int = 44100,
    val duration: Duration
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioInput) return false
        return audioData.contentEquals(other.audioData) && format == other.format
    }
    
    override fun hashCode(): Int {
        return audioData.contentHashCode() + format.hashCode()
    }
}

@Serializable
data class TeachingContext(
    val subject: Subject,
    val currentTopic: String,
    val difficultyLevel: DifficultyLevel,
    val sessionType: SessionType,
    val studentLevel: String
)

@Serializable
enum class SessionType {
    LECTURE, PRACTICE, REVIEW, ASSESSMENT, Q_AND_A
}

@Serializable
data class SpeechRecognitionResult(
    val text: String,
    val confidence: Double,
    val language: String,
    val duration: Duration
)

@Serializable
data class IntentAnalysis(
    val intent: TeachingIntent,
    val entities: List<String>,
    val confidence: Double,
    val context: TeachingContext
)

@Serializable
enum class TeachingIntent {
    QUESTION, EXPLANATION_REQUEST, PRACTICE_REQUEST, CLARIFICATION, FEEDBACK, HELP
}

@Serializable
data class TeachingResponse(
    val text: String,
    val voiceSettings: VoiceSettings,
    val additionalResources: List<String>
)

@Serializable
data class VoiceSettings(
    val voice: String = "teacher_female",
    val speed: Double = 1.0,
    val pitch: Double = 1.0,
    val volume: Double = 1.0
)

@Serializable
data class AudioOutput(
    val audioUrl: String,
    val duration: Duration,
    val format: String
)

@Serializable
data class VoiceInteraction(
    val studentId: StudentId,
    val timestamp: Instant,
    val input: SpeechRecognitionResult,
    val intent: IntentAnalysis,
    val response: TeachingResponse,
    val audioOutput: AudioOutput,
    val context: TeachingContext
)

// 视觉内容相关
@Serializable
data class VisualInput(
    val imageData: ByteArray,
    val format: String = "png",
    val width: Int,
    val height: Int,
    val metadata: Map<String, String> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VisualInput) return false
        return imageData.contentEquals(other.imageData) && format == other.format
    }
    
    override fun hashCode(): Int {
        return imageData.contentHashCode() + format.hashCode()
    }
}

@Serializable
data class VisualProcessingRequest(
    val enableImageUnderstanding: Boolean = true,
    val enableTextExtraction: Boolean = true,
    val enableContentGeneration: Boolean = true,
    val enableQuestionGeneration: Boolean = true,
    val subject: Subject,
    val difficultyLevel: DifficultyLevel
)

@Serializable
data class VisualProcessingOutput(
    val type: VisualOutputType,
    val content: String,
    val metadata: Map<String, String>
)

@Serializable
enum class VisualOutputType {
    IMAGE_ANALYSIS, TEXT_EXTRACTION, TEACHING_CONTENT, QUESTIONS, ANNOTATIONS
}

@Serializable
data class VisualProcessing(
    val studentId: StudentId,
    val timestamp: Instant,
    val input: VisualInput,
    val request: VisualProcessingRequest,
    val outputs: List<VisualProcessingOutput>,
    val processingTime: Duration
)

@Serializable
data class ImageAnalysis(
    val description: String,
    val objects: List<String>,
    val metadata: Map<String, String>
)

@Serializable
data class TextExtractionResult(
    val text: String,
    val confidence: Double
)

@Serializable
data class GeneratedContent(
    val content: String,
    val metadata: Map<String, String>
)

@Serializable
data class GeneratedQuestion(
    val text: String,
    val difficulty: DifficultyLevel,
    val type: QuestionType = QuestionType.OPEN_ENDED
)

@Serializable
enum class QuestionType {
    MULTIPLE_CHOICE, TRUE_FALSE, OPEN_ENDED, CONCEPTUAL, ANALYTICAL
}

// 多模态内容创建相关
@Serializable
data class MultimodalContentRequest(
    val topic: String,
    val difficultyLevel: DifficultyLevel,
    val targetAudience: String,
    val estimatedDuration: Duration,
    val includeText: Boolean = true,
    val includeVisuals: Boolean = true,
    val includeAudio: Boolean = true,
    val includeInteractive: Boolean = true,
    val visualStyle: VisualStyle = VisualStyle.EDUCATIONAL,
    val voiceSettings: VoiceSettings = VoiceSettings()
)

@Serializable
enum class VisualStyle {
    EDUCATIONAL, PROFESSIONAL, CASUAL, ARTISTIC, TECHNICAL
}

@Serializable
data class ContentComponent(
    val type: ContentType,
    val content: String,
    val metadata: Map<String, String>
)

@Serializable
data class MultimodalContent(
    val id: String,
    val topic: String,
    val objectives: List<String>,
    val components: List<ContentComponent>,
    val integratedContent: IntegratedContent,
    val createdAt: Instant,
    val metadata: Map<String, String>
)

@Serializable
data class IntegratedContent(
    val layout: String,
    val structure: String,
    val interactions: List<String>,
    val accessibility: Map<String, String>
)

@Serializable
data class GeneratedVisualContent(
    val imageUrl: String,
    val metadata: Map<String, String>
)

@Serializable
data class GeneratedAudioContent(
    val audioUrl: String,
    val metadata: Map<String, String>
)

// 智能问答相关
@Serializable
data class StudentQuestion(
    val text: String,
    val timestamp: Instant,
    val context: LearningContext,
    val questionType: QuestionType = QuestionType.OPEN_ENDED
)

@Serializable
data class LearningContext(
    val currentCourse: String,
    val currentTopic: String,
    val difficultyLevel: DifficultyLevel,
    val previousTopics: List<String> = emptyList()
)

@Serializable
data class QuestionAnalysis(
    val questionType: QuestionType,
    val difficulty: DifficultyLevel,
    val topics: List<String>,
    val requiredKnowledge: List<String>
)

@Serializable
data class KnowledgeItem(
    val title: String,
    val content: String,
    val relevance: Double = 1.0
)

@Serializable
data class IntelligentAnswer(
    val content: String,
    val reasoning: String,
    val sources: List<String>
)

@Serializable
data class DetailedExplanation(
    val stepByStep: List<String>,
    val examples: List<String>,
    val analogies: List<String>
)

@Serializable
data class SupplementaryResource(
    val title: String,
    val url: String,
    val type: ResourceType
)

@Serializable
enum class ResourceType {
    VIDEO, ARTICLE, EXERCISE, SIMULATION, REFERENCE
}

@Serializable
data class FollowUpQuestion(
    val text: String,
    val purpose: QuestionPurpose
)

@Serializable
enum class QuestionPurpose {
    CLARIFICATION, DEEPER_UNDERSTANDING, APPLICATION, CONNECTION, ASSESSMENT
}

@Serializable
data class IntelligentQAResponse(
    val studentId: StudentId,
    val originalQuestion: StudentQuestion,
    val questionAnalysis: QuestionAnalysis,
    val answer: IntelligentAnswer,
    val explanation: DetailedExplanation,
    val supplementaryResources: List<SupplementaryResource>,
    val followUpQuestions: List<FollowUpQuestion>,
    val confidence: Double,
    val timestamp: Instant
)

// 教学策略推荐相关
@Serializable
data class StudentPerformanceData(
    val studentId: StudentId,
    val recentScores: List<Double>,
    val timeSpentOnTopics: Map<String, Duration>,
    val difficultyProgression: List<DifficultyLevel>,
    val engagementMetrics: Map<String, Double>
)

@Serializable
data class LearningStyleAnalysis(
    val primaryStyle: LearningStyle,
    val secondaryStyle: LearningStyle,
    val preferences: Map<String, Double>
)

@Serializable
data class KnowledgeMasteryAssessment(
    val overallMastery: Double,
    val topicMastery: Map<String, Double>,
    val strengths: List<String>,
    val weaknesses: List<String>
)

@Serializable
data class LearningDifficulty(
    val area: String,
    val severity: DifficultyLevel,
    val description: String
)

@Serializable
data class TeachingStrategy(
    val name: String,
    val description: String,
    val type: StrategyType
)

@Serializable
enum class StrategyType {
    VISUAL, AUDITORY, KINESTHETIC, PRACTICE, CONCEPTUAL, COLLABORATIVE
}

@Serializable
data class PrioritizedStrategy(
    val strategy: TeachingStrategy,
    val priority: Int,
    val expectedEffectiveness: Double,
    val implementationComplexity: ImplementationComplexity
)

@Serializable
enum class ImplementationComplexity {
    LOW, MEDIUM, HIGH
}

@Serializable
data class ImplementationGuidance(
    val timeline: String,
    val resources: List<String>,
    val steps: List<String>,
    val successMetrics: List<String>
)

@Serializable
data class ExpectedOutcomes(
    val learningImprovement: Double,
    val engagementIncrease: Double,
    val timeToMastery: String,
    val confidenceLevel: Double
)

@Serializable
data class TeachingStrategyRecommendation(
    val studentId: StudentId,
    val topic: String,
    val learningStyleAnalysis: LearningStyleAnalysis,
    val knowledgeMastery: KnowledgeMasteryAssessment,
    val identifiedDifficulties: List<LearningDifficulty>,
    val recommendedStrategies: List<PrioritizedStrategy>,
    val implementationGuidance: ImplementationGuidance,
    val expectedOutcomes: ExpectedOutcomes,
    val recommendationDate: Instant
)

// 结果类型
sealed class VoiceInteractionResult {
    data class Success(val interaction: VoiceInteraction, val message: String) : VoiceInteractionResult()
    data class Failure(val error: String) : VoiceInteractionResult()
}

sealed class VisualProcessingResult {
    data class Success(val processing: VisualProcessing, val message: String) : VisualProcessingResult()
    data class Failure(val error: String) : VisualProcessingResult()
}

sealed class MultimodalContentResult {
    data class Success(val content: MultimodalContent, val message: String) : MultimodalContentResult()
    data class Failure(val error: String) : MultimodalContentResult()
}

sealed class IntelligentQAResult {
    data class Success(val response: IntelligentQAResponse, val message: String) : IntelligentQAResult()
    data class Failure(val error: String) : IntelligentQAResult()
}

sealed class TeachingStrategyResult {
    data class Success(val recommendation: TeachingStrategyRecommendation, val message: String) : TeachingStrategyResult()
    data class Failure(val error: String) : TeachingStrategyResult()
}

// 多模态教学服务相关数据类型
@Serializable
data class MultimodalSessionConfig(
    val subject: Subject,
    val topic: String,
    val duration: Duration,
    val enableVoiceInteraction: Boolean = true,
    val enableVisualProcessing: Boolean = true,
    val enableTextAnalysis: Boolean = true,
    val maxParticipants: Int = 30
)

@Serializable
data class MultimodalSession(
    val id: String,
    val teacherId: String,
    val studentIds: List<StudentId>,
    val config: MultimodalSessionConfig,
    val resources: MultimodalResources,
    val interactionModes: List<InteractionMode>,
    val monitoring: MonitoringInfo,
    val startTime: Instant,
    val status: MultimodalSessionStatus
)

@Serializable
enum class MultimodalSessionStatus {
    ACTIVE, PAUSED, COMPLETED, CANCELLED
}

@Serializable
data class MultimodalResources(
    val audioResources: List<String>,
    val visualResources: List<String>,
    val interactiveResources: List<String>
)

@Serializable
enum class InteractionMode {
    VOICE, VISUAL, TEXT, GESTURE, TOUCH
}

@Serializable
data class MonitoringInfo(
    val sessionId: String,
    val monitoringEnabled: Boolean,
    val metricsCollected: List<String>
)

@Serializable
data class MultimodalInteraction(
    val audioInput: AudioInput? = null,
    val visualInput: VisualInput? = null,
    val textQuestion: StudentQuestion? = null,
    val context: TeachingContext,
    val visualProcessingRequest: VisualProcessingRequest? = null
)

@Serializable
data class InteractionResponse(
    val type: InteractionType,
    val content: String,
    val audioOutput: AudioOutput? = null,
    val visualOutputs: List<VisualProcessingOutput>? = null,
    val explanation: DetailedExplanation? = null,
    val supplementaryResources: List<SupplementaryResource>? = null,
    val timestamp: Instant
)

@Serializable
enum class InteractionType {
    VOICE, VISUAL, TEXT, MULTIMODAL
}

@Serializable
data class ComprehensiveResponse(
    val summary: String,
    val recommendations: List<String>,
    val nextSteps: List<String>
)

@Serializable
data class MultimodalInteractionProcessing(
    val sessionId: String,
    val studentId: StudentId,
    val originalInteraction: MultimodalInteraction,
    val responses: List<InteractionResponse>,
    val comprehensiveResponse: ComprehensiveResponse,
    val processingTime: Instant
)

@Serializable
data class PersonalizedContentRequest(
    val topic: String,
    val difficultyLevel: DifficultyLevel,
    val duration: Duration,
    val learningObjectives: List<String>
)

@Serializable
data class StudentProfile(
    val studentId: StudentId,
    val learningLevel: String,
    val preferredModalities: List<String>,
    val preferredVisualStyle: VisualStyle,
    val preferredVoiceSettings: VoiceSettings
)

@Serializable
data class OptimalModalities(
    val includeText: Boolean,
    val includeVisuals: Boolean,
    val includeAudio: Boolean,
    val includeInteractive: Boolean
)

@Serializable
data class AdaptiveElement(
    val name: String,
    val description: String
)

@Serializable
data class PersonalizedMultimodalContent(
    val studentId: StudentId,
    val originalRequest: PersonalizedContentRequest,
    val studentProfile: StudentProfile,
    val optimalModalities: OptimalModalities,
    val content: MultimodalContent,
    val adaptiveElements: List<AdaptiveElement>,
    val createdAt: Instant,
    val estimatedEffectiveness: Double
)

@Serializable
data class EffectivenessAnalysisRequest(
    val analysisType: AnalysisType,
    val timeRange: Duration,
    val includeEngagement: Boolean = true,
    val includeLearningOutcomes: Boolean = true
)

@Serializable
enum class AnalysisType {
    REAL_TIME, SUMMARY, DETAILED, COMPARATIVE
}

@Serializable
data class SessionData(
    val sessionId: String,
    val duration: Duration,
    val participantCount: Int,
    val interactionCount: Int
)

@Serializable
data class EngagementAnalysis(
    val averageEngagement: Double,
    val peakEngagementTime: String,
    val lowEngagementPeriods: List<String>
)

@Serializable
data class LearningEffectiveness(
    val comprehensionRate: Double,
    val retentionRate: Double,
    val applicationAbility: Double
)

@Serializable
data class ModalityEffectiveness(
    val visualEffectiveness: Double,
    val audioEffectiveness: Double,
    val textEffectiveness: Double,
    val interactiveEffectiveness: Double
)

@Serializable
data class ImprovementSuggestion(
    val title: String,
    val description: String
)

@Serializable
data class TeachingEffectivenessAnalysis(
    val sessionId: String,
    val analysisRequest: EffectivenessAnalysisRequest,
    val sessionData: SessionData,
    val engagementAnalysis: EngagementAnalysis,
    val learningEffectiveness: LearningEffectiveness,
    val modalityEffectiveness: ModalityEffectiveness,
    val improvementSuggestions: List<ImprovementSuggestion>,
    val overallScore: Double,
    val analysisTime: Instant
)

@Serializable
data class SessionContext(
    val currentTopic: String,
    val subject: Subject,
    val sessionType: SessionType,
    val duration: Duration
)

@Serializable
data class TeachingSuggestion(
    val studentId: StudentId,
    val type: SuggestionType,
    val title: String,
    val description: String,
    val strategies: List<PrioritizedStrategy>,
    val priority: SuggestionPriority,
    val implementationTime: String
)

@Serializable
enum class SuggestionType {
    PERSONALIZED_STRATEGY, CLASS_LEVEL, TECHNOLOGY, CONTENT, ASSESSMENT
}

@Serializable
enum class SuggestionPriority {
    LOW, MEDIUM, HIGH, URGENT
}

@Serializable
data class ImplementationPlan(
    val phases: List<String>,
    val timeline: String,
    val resources: List<String>
)

@Serializable
data class ExpectedImpact(
    val learningImprovement: Double,
    val engagementIncrease: Double,
    val efficiencyGain: Double
)

@Serializable
data class IntelligentTeachingSuggestions(
    val teacherId: String,
    val sessionContext: SessionContext,
    val suggestions: List<TeachingSuggestion>,
    val implementationPlan: ImplementationPlan,
    val expectedImpact: ExpectedImpact,
    val generatedAt: Instant
)

@Serializable
data class SessionInfo(
    val id: String,
    val teacherId: String,
    val studentCount: Int
)

// 扩展结果类型
sealed class MultimodalSessionResult {
    data class Success(val session: MultimodalSession, val message: String) : MultimodalSessionResult()
    data class Failure(val error: String) : MultimodalSessionResult()
}

sealed class InteractionProcessingResult {
    data class Success(val processing: MultimodalInteractionProcessing, val message: String) : InteractionProcessingResult()
    data class Failure(val error: String) : InteractionProcessingResult()
}

sealed class PersonalizedContentResult {
    data class Success(val content: PersonalizedMultimodalContent, val message: String) : PersonalizedContentResult()
    data class Failure(val error: String) : PersonalizedContentResult()
}

sealed class EffectivenessAnalysisResult {
    data class Success(val analysis: TeachingEffectivenessAnalysis, val message: String) : EffectivenessAnalysisResult()
    data class Failure(val error: String) : EffectivenessAnalysisResult()
}

sealed class TeachingSuggestionsResult {
    data class Success(val suggestions: IntelligentTeachingSuggestions, val message: String) : TeachingSuggestionsResult()
    data class Failure(val error: String) : TeachingSuggestionsResult()
}
