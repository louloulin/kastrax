package ai.kastrax.edutech.assistant

import ai.kastrax.core.actor.Message
import ai.kastrax.edutech.models.*
import kotlinx.datetime.Instant

/**
 * 虚拟教学助手Actor消息定义
 */

// 输入消息
data class InitializeAssistant(
    val name: String,
    val specializations: Set<Subject>,
    val personality: AssistantPersonality,
    val capabilities: AssistantCapabilities
) : Message

data class StartConversation(
    val studentId: StudentId,
    val subject: Subject,
    val initialMessage: String? = null
) : Message

data class ProcessStudentMessage(
    val conversationId: String,
    val studentId: StudentId,
    val message: String
) : Message

data class EndConversation(
    val conversationId: String
) : Message

data class UpdateAssistantKnowledge(
    val subject: Subject,
    val knowledge: SubjectKnowledge
) : Message

data class GetAssistantStatus(
    val requestId: String = ""
) : Message

data class GetConversationHistory(
    val studentId: StudentId,
    val limit: Int = 10
) : Message

data class AdaptAssistantBehavior(
    val adaptationType: PersonalityAdaptationType,
    val targetValue: Float,
    val reason: String
) : Message

data class AnalyzePerformance(
    val timeRange: Pair<Instant, Instant>
) : Message

// 响应消息
data class AssistantInitialized(
    val assistantId: String,
    val success: Boolean,
    val assistant: VirtualTeachingAssistant? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class ConversationStarted(
    val conversationId: String,
    val success: Boolean,
    val conversation: AssistantConversation? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class MessageProcessed(
    val conversationId: String,
    val success: Boolean,
    val response: AssistantResponse? = null,
    val detectedEmotion: DetectedEmotion? = null,
    val emotionSuggestions: List<EmotionSuggestion> = emptyList(),
    val error: String? = null,
    val timestamp: Instant
) : Message

data class ConversationEnded(
    val conversationId: String,
    val success: Boolean,
    val summary: ConversationSummary? = null,
    val metrics: ConversationMetrics? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class KnowledgeUpdated(
    val assistantId: String,
    val success: Boolean,
    val subject: Subject? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class AssistantStatusResponse(
    val status: AssistantStatus,
    val timestamp: Instant
) : Message

data class ConversationHistoryResponse(
    val studentId: StudentId,
    val conversations: List<AssistantConversation>,
    val timestamp: Instant
) : Message

data class BehaviorAdapted(
    val assistantId: String,
    val success: Boolean,
    val adaptationType: PersonalityAdaptationType? = null,
    val newValue: Float? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

data class PerformanceAnalysisResponse(
    val assistantId: String,
    val success: Boolean,
    val performance: AssistantPerformanceMetrics? = null,
    val error: String? = null,
    val timestamp: Instant
) : Message

// 通知消息
data class EmotionAlert(
    val conversationId: String,
    val studentId: StudentId,
    val emotion: DetectedEmotion,
    val severity: AlertSeverity,
    val recommendedActions: List<String>,
    val timestamp: Instant
) : Message

data class PerformanceAlert(
    val assistantId: String,
    val alertType: PerformanceAlertType,
    val currentValue: Float,
    val threshold: Float,
    val recommendedActions: List<String>,
    val timestamp: Instant
) : Message

data class KnowledgeGapDetected(
    val conversationId: String,
    val studentId: StudentId,
    val subject: Subject,
    val missingConcepts: List<String>,
    val recommendedResources: List<String>,
    val timestamp: Instant
) : Message

data class LearningProgressUpdate(
    val conversationId: String,
    val studentId: StudentId,
    val subject: Subject,
    val conceptsMastered: List<String>,
    val strugglingAreas: List<String>,
    val nextRecommendations: List<String>,
    val timestamp: Instant
) : Message

// 系统消息
data class AssistantHealthCheck(
    val assistantId: String,
    val timestamp: Instant
) : Message

data class AssistantHealthResponse(
    val assistantId: String,
    val isHealthy: Boolean,
    val metrics: HealthMetrics,
    val timestamp: Instant
) : Message

data class ConversationTimeout(
    val conversationId: String,
    val studentId: StudentId,
    val lastActivity: Instant,
    val timeoutDuration: kotlin.time.Duration,
    val timestamp: Instant
) : Message

data class LoadBalancingRequest(
    val currentLoad: Float,
    val maxCapacity: Int,
    val requestNewAssistant: Boolean,
    val timestamp: Instant
) : Message

// 批量操作消息
data class BatchProcessMessages(
    val messages: List<StudentMessageBatch>,
    val priority: ProcessingPriority = ProcessingPriority.NORMAL
) : Message

data class BatchProcessResponse(
    val processedCount: Int,
    val failedCount: Int,
    val responses: List<BatchMessageResponse>,
    val timestamp: Instant
) : Message

// 分析和报告消息
data class GenerateInsightReport(
    val assistantId: String,
    val timeRange: Pair<Instant, Instant>,
    val includeEmotionAnalysis: Boolean = true,
    val includePerformanceMetrics: Boolean = true,
    val includeRecommendations: Boolean = true
) : Message

data class InsightReportResponse(
    val assistantId: String,
    val report: AssistantInsightReport,
    val timestamp: Instant
) : Message

data class RequestPersonalityAdjustment(
    val assistantId: String,
    val studentId: StudentId,
    val adjustmentReason: String,
    val suggestedChanges: Map<PersonalityAdaptationType, Float>
) : Message

data class PersonalityAdjustmentResponse(
    val assistantId: String,
    val success: Boolean,
    val appliedChanges: Map<PersonalityAdaptationType, Float>,
    val error: String? = null,
    val timestamp: Instant
) : Message

// 枚举类型
enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class PerformanceAlertType {
    HIGH_RESPONSE_TIME, LOW_ACCURACY, HIGH_ERROR_RATE, CAPACITY_EXCEEDED,
    LOW_ENGAGEMENT, KNOWLEDGE_GAP_DETECTED
}

enum class ProcessingPriority {
    LOW, NORMAL, HIGH, URGENT
}

// 辅助数据类
data class StudentMessageBatch(
    val conversationId: String,
    val studentId: StudentId,
    val message: String,
    val timestamp: Instant
)

data class BatchMessageResponse(
    val conversationId: String,
    val success: Boolean,
    val response: AssistantResponse? = null,
    val error: String? = null
)

data class HealthMetrics(
    val responseTime: kotlin.time.Duration,
    val errorRate: Float,
    val memoryUsage: Float,
    val activeConversations: Int,
    val lastHealthCheck: Instant
)

data class AssistantInsightReport(
    val assistantId: String,
    val timeRange: Pair<Instant, Instant>,
    val conversationStats: ConversationStatistics,
    val emotionAnalysis: EmotionAnalysisSummary,
    val performanceMetrics: AssistantPerformanceMetrics,
    val topChallenges: List<String>,
    val successStories: List<String>,
    val recommendations: List<String>,
    val generatedAt: Instant
)

data class ConversationStatistics(
    val totalConversations: Int,
    val averageConversationLength: kotlin.time.Duration,
    val averageMessagesPerConversation: Int,
    val completionRate: Float,
    val satisfactionScore: Float,
    val topTopics: List<String>
)

data class EmotionAnalysisSummary(
    val dominantEmotions: Map<Emotion, Int>,
    val emotionTrends: Map<Emotion, TrendDirection>,
    val interventionSuccessRate: Float,
    val emotionalEngagementScore: Float,
    val criticalEmotionEvents: Int
)
