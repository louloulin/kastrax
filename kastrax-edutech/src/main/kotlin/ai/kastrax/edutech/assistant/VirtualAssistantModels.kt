package ai.kastrax.edutech.assistant

import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * 虚拟教学助手数据模型
 * 定义智能虚拟教学助手系统的核心数据结构
 */

/**
 * 虚拟教学助手
 */
@Serializable
data class VirtualTeachingAssistant(
    val assistantId: String,
    val name: String,
    val personality: AssistantPersonality,
    val specializations: Set<Subject>,
    val capabilities: AssistantCapabilities,
    val knowledgeBase: AssistantKnowledgeBase,
    val conversationStyle: ConversationStyle,
    val languageSupport: Set<Language>,
    val createdAt: Instant,
    val lastUpdated: Instant,
    val isActive: Boolean = true
)

/**
 * 助手个性特征
 */
@Serializable
data class AssistantPersonality(
    val friendliness: Float, // 0.0-1.0
    val formality: Float, // 0.0-1.0
    val patience: Float, // 0.0-1.0
    val enthusiasm: Float, // 0.0-1.0
    val empathy: Float, // 0.0-1.0
    val humor: Float, // 0.0-1.0
    val encouragement: Float, // 0.0-1.0
    val adaptability: Float // 0.0-1.0
)

/**
 * 助手能力
 */
@Serializable
data class AssistantCapabilities(
    val canAnswerQuestions: Boolean = true,
    val canProvideExplanations: Boolean = true,
    val canCreateExercises: Boolean = true,
    val canGradePapers: Boolean = true,
    val canProvideHints: Boolean = true,
    val canDetectEmotions: Boolean = true,
    val canAdaptDifficulty: Boolean = true,
    val canGenerateContent: Boolean = true,
    val canTranslateLanguages: Boolean = true,
    val canProvideVisualAids: Boolean = true,
    val canConductAssessments: Boolean = true,
    val canTrackProgress: Boolean = true,
    val maxConcurrentStudents: Int = 100,
    val responseTimeMs: Long = 1000
)

/**
 * 助手知识库
 */
@Serializable
data class AssistantKnowledgeBase(
    val subjects: Map<Subject, SubjectKnowledge>,
    val pedagogicalMethods: Set<TeachingMethod>,
    val assessmentStrategies: Set<AssessmentStrategy>,
    val learningTheories: Set<LearningTheory>,
    val lastKnowledgeUpdate: Instant
)

/**
 * 学科知识
 */
@Serializable
data class SubjectKnowledge(
    val subject: Subject,
    val gradeLevel: GradeLevel,
    val topics: List<KnowledgeTopic>,
    val competencyLevel: CompetencyLevel,
    val lastUpdated: Instant
)

/**
 * 知识主题
 */
@Serializable
data class KnowledgeTopic(
    val topicId: String,
    val title: String,
    val description: String,
    val prerequisites: List<String>,
    val difficulty: DifficultyLevel,
    val concepts: List<Concept>,
    val examples: List<Example>,
    val commonMisconceptions: List<Misconception>
)

/**
 * 概念
 */
@Serializable
data class Concept(
    val conceptId: String,
    val name: String,
    val definition: String,
    val explanation: String,
    val visualAids: List<VisualAid>,
    val relatedConcepts: List<String>
)

/**
 * 示例
 */
@Serializable
data class Example(
    val exampleId: String,
    val title: String,
    val description: String,
    val solution: String,
    val stepByStep: List<SolutionStep>,
    val difficulty: DifficultyLevel
)

/**
 * 解决步骤
 */
@Serializable
data class SolutionStep(
    val stepNumber: Int,
    val description: String,
    val explanation: String,
    val visualAid: VisualAid? = null
)

/**
 * 视觉辅助
 */
@Serializable
data class VisualAid(
    val aidId: String,
    val type: VisualAidType,
    val url: String,
    val description: String,
    val altText: String
)

/**
 * 常见误解
 */
@Serializable
data class Misconception(
    val misconceptionId: String,
    val description: String,
    val correctExplanation: String,
    val commonCauses: List<String>,
    val correctionStrategies: List<String>
)

/**
 * 对话风格
 */
@Serializable
data class ConversationStyle(
    val tone: ConversationTone,
    val complexity: ComplexityLevel,
    val verbosity: VerbosityLevel,
    val useOfExamples: ExampleUsage,
    val questioningStyle: QuestioningStyle,
    val feedbackStyle: FeedbackStyle
)

/**
 * 助手会话
 */
@Serializable
data class AssistantConversation(
    val conversationId: String,
    val assistantId: String,
    val studentId: StudentId,
    val subject: Subject,
    val startTime: Instant,
    val endTime: Instant? = null,
    val messages: List<ConversationMessage>,
    val context: ConversationContext,
    val status: ConversationStatus,
    val summary: ConversationSummary? = null
)

/**
 * 对话消息
 */
@Serializable
data class ConversationMessage(
    val messageId: String,
    val sender: MessageSender,
    val content: MessageContent,
    val timestamp: Instant,
    val metadata: MessageMetadata
)

/**
 * 消息内容
 */
@Serializable
data class MessageContent(
    val text: String,
    val attachments: List<MessageAttachment> = emptyList(),
    val intent: MessageIntent,
    val emotion: DetectedEmotion? = null,
    val confidence: Float = 1.0f
)

/**
 * 消息附件
 */
@Serializable
data class MessageAttachment(
    val attachmentId: String,
    val type: AttachmentType,
    val url: String,
    val description: String,
    val size: Long
)

/**
 * 消息元数据
 */
@Serializable
data class MessageMetadata(
    val responseTime: Duration,
    val processingSteps: List<ProcessingStep>,
    val knowledgeUsed: List<String>,
    val confidence: Float,
    val alternatives: List<AlternativeResponse> = emptyList()
)

/**
 * 处理步骤
 */
@Serializable
data class ProcessingStep(
    val stepName: String,
    val duration: Duration,
    val result: String
)

/**
 * 替代回复
 */
@Serializable
data class AlternativeResponse(
    val response: String,
    val confidence: Float,
    val reasoning: String
)

/**
 * 对话上下文
 */
@Serializable
data class ConversationContext(
    val currentTopic: String,
    val learningObjectives: List<String>,
    val studentLevel: DifficultyLevel,
    val previousTopics: List<String>,
    val strugglingAreas: List<String>,
    val strengths: List<String>,
    val preferredLearningStyle: LearningStyle,
    val sessionGoals: List<String>
)

/**
 * 对话摘要
 */
@Serializable
data class ConversationSummary(
    val mainTopics: List<String>,
    val questionsAsked: Int,
    val questionsAnswered: Int,
    val conceptsExplained: List<String>,
    val difficultiesEncountered: List<String>,
    val progressMade: List<String>,
    val recommendedNextSteps: List<String>,
    val overallSentiment: Sentiment,
    val engagementLevel: EngagementLevel
)

/**
 * 检测到的情绪
 */
@Serializable
data class DetectedEmotion(
    val primary: Emotion,
    val secondary: List<Emotion>,
    val intensity: Float, // 0.0-1.0
    val confidence: Float // 0.0-1.0
)

/**
 * 助手响应
 */
@Serializable
data class AssistantResponse(
    val responseId: String,
    val conversationId: String,
    val content: MessageContent,
    val reasoning: ResponseReasoning,
    val adaptations: List<ResponseAdaptation>,
    val followUpSuggestions: List<String>,
    val timestamp: Instant
)

/**
 * 响应推理
 */
@Serializable
data class ResponseReasoning(
    val strategy: ResponseStrategy,
    val knowledgeUsed: List<String>,
    val personalityFactors: List<String>,
    val contextFactors: List<String>,
    val adaptationReasons: List<String>
)

/**
 * 响应适应
 */
@Serializable
data class ResponseAdaptation(
    val adaptationType: AdaptationType,
    val reason: String,
    val originalValue: String,
    val adaptedValue: String
)

/**
 * 助手性能指标
 */
@Serializable
data class AssistantPerformanceMetrics(
    val assistantId: String,
    val timeRange: Pair<Instant, Instant>,
    val totalConversations: Int,
    val averageResponseTime: Duration,
    val studentSatisfactionScore: Float,
    val accuracyRate: Float,
    val engagementRate: Float,
    val problemResolutionRate: Float,
    val knowledgeGapIdentificationRate: Float,
    val adaptationSuccessRate: Float,
    val topPerformingSubjects: List<Subject>,
    val improvementAreas: List<String>
)

// 枚举类型定义

enum class Language {
    ENGLISH, CHINESE, SPANISH, FRENCH, GERMAN, JAPANESE, KOREAN, ARABIC, RUSSIAN, PORTUGUESE
}

enum class TeachingMethod {
    SOCRATIC_METHOD, DIRECT_INSTRUCTION, INQUIRY_BASED, COLLABORATIVE_LEARNING,
    PROBLEM_BASED, EXPERIENTIAL_LEARNING, FLIPPED_CLASSROOM, GAMIFICATION
}

enum class AssessmentStrategy {
    FORMATIVE_ASSESSMENT, SUMMATIVE_ASSESSMENT, PEER_ASSESSMENT, SELF_ASSESSMENT,
    AUTHENTIC_ASSESSMENT, DIAGNOSTIC_ASSESSMENT, ADAPTIVE_ASSESSMENT
}

enum class LearningTheory {
    CONSTRUCTIVISM, BEHAVIORISM, COGNITIVISM, CONNECTIVISM, HUMANISM,
    SOCIAL_LEARNING_THEORY, MULTIPLE_INTELLIGENCE_THEORY
}

enum class CompetencyLevel {
    NOVICE, BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
}

enum class VisualAidType {
    IMAGE, DIAGRAM, CHART, GRAPH, VIDEO, ANIMATION, INTERACTIVE_SIMULATION, INFOGRAPHIC
}

enum class ConversationTone {
    FORMAL, INFORMAL, FRIENDLY, PROFESSIONAL, ENCOURAGING, PATIENT, ENTHUSIASTIC
}

enum class ComplexityLevel {
    VERY_SIMPLE, SIMPLE, MODERATE, COMPLEX, VERY_COMPLEX
}

enum class VerbosityLevel {
    CONCISE, MODERATE, DETAILED, COMPREHENSIVE
}

enum class ExampleUsage {
    MINIMAL, MODERATE, FREQUENT, EXTENSIVE
}

enum class QuestioningStyle {
    DIRECT, SOCRATIC, GUIDED_DISCOVERY, OPEN_ENDED, SCAFFOLDED
}

enum class FeedbackStyle {
    IMMEDIATE, DELAYED, DETAILED, BRIEF, ENCOURAGING, CONSTRUCTIVE
}

enum class MessageSender {
    STUDENT, ASSISTANT, SYSTEM
}

enum class MessageIntent {
    QUESTION, ANSWER, EXPLANATION, HINT, ENCOURAGEMENT, CORRECTION,
    ASSESSMENT, FEEDBACK, GREETING, FAREWELL, CLARIFICATION
}

enum class AttachmentType {
    IMAGE, DOCUMENT, AUDIO, VIDEO, LINK, CODE, FORMULA, DIAGRAM
}

enum class ConversationStatus {
    ACTIVE, PAUSED, COMPLETED, TERMINATED, WAITING_FOR_RESPONSE
}

enum class Emotion {
    HAPPY, SAD, FRUSTRATED, CONFUSED, EXCITED, BORED, ANXIOUS, CONFIDENT,
    CURIOUS, OVERWHELMED, SATISFIED, DISAPPOINTED
}

enum class Sentiment {
    VERY_POSITIVE, POSITIVE, NEUTRAL, NEGATIVE, VERY_NEGATIVE
}

enum class EngagementLevel {
    VERY_LOW, LOW, MODERATE, HIGH, VERY_HIGH
}

enum class ResponseStrategy {
    DIRECT_ANSWER, GUIDED_DISCOVERY, SOCRATIC_QUESTIONING, EXAMPLE_BASED,
    ANALOGY_BASED, STEP_BY_STEP, ENCOURAGEMENT_FIRST, CLARIFICATION_SEEKING
}

enum class AdaptationType {
    DIFFICULTY_ADJUSTMENT, TONE_MODIFICATION, VERBOSITY_CHANGE, EXAMPLE_ADDITION,
    LANGUAGE_SIMPLIFICATION, PACE_ADJUSTMENT, STYLE_ADAPTATION
}
