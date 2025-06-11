package ai.kastrax.edutech.collaboration

import ai.kastrax.edutech.models.CourseId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 协作学习相关数据模型
 * Week 17-18 高级扩展功能
 */

/**
 * 协作学习会话
 */
@Serializable
data class CollaborationSession(
    val id: String,
    val courseId: CourseId,
    val title: String,
    val description: String,
    val creatorId: String,
    val participants: MutableSet<String>,
    val maxParticipants: Int,
    var status: CollaborationStatus,
    val createdAt: Instant,
    var endedAt: Instant? = null,
    val sharedResources: MutableList<SharedResource>,
    val discussionThreads: MutableList<DiscussionThread>,
    val collaborativeNotes: MutableMap<String, CollaborativeNote>
)

/**
 * 协作状态
 */
@Serializable
enum class CollaborationStatus {
    ACTIVE,      // 活跃中
    PAUSED,      // 暂停
    ENDED        // 已结束
}

/**
 * 协作消息
 */
@Serializable
data class CollaborationMessage(
    val id: String = "",
    val type: MessageType,
    val senderId: String,
    val content: String,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 消息类型
 */
@Serializable
enum class MessageType {
    CHAT,                    // 聊天消息
    DISCUSSION,              // 讨论消息
    COLLABORATIVE_NOTE,      // 协作笔记
    RESOURCE_SHARE,          // 资源分享
    ANNOTATION_ADDED,        // 添加标注
    NOTE_CREATED,           // 笔记创建
    NOTE_UPDATED,           // 笔记更新
    USER_JOINED,            // 用户加入
    USER_LEFT,              // 用户离开
    SESSION_CREATED,        // 会话创建
    SESSION_ENDED,          // 会话结束
    SYSTEM_NOTIFICATION     // 系统通知
}

/**
 * 协作笔记
 */
@Serializable
data class CollaborativeNote(
    val id: String,
    var title: String,
    var content: String,
    val creatorId: String,
    val contributors: MutableSet<String>,
    val createdAt: Instant,
    var lastModified: Instant,
    var version: Int,
    val annotations: MutableList<NoteAnnotation>
)

/**
 * 笔记标注
 */
@Serializable
data class NoteAnnotation(
    val id: String = "",
    val content: String,
    val type: AnnotationType,
    val position: AnnotationPosition,
    val authorId: String = "",
    val createdAt: Instant? = null,
    val isResolved: Boolean = false
)

/**
 * 标注类型
 */
@Serializable
enum class AnnotationType {
    COMMENT,        // 评论
    QUESTION,       // 问题
    SUGGESTION,     // 建议
    HIGHLIGHT,      // 高亮
    CORRECTION      // 纠正
}

/**
 * 标注位置
 */
@Serializable
data class AnnotationPosition(
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String
)

/**
 * 讨论线程
 */
@Serializable
data class DiscussionThread(
    val id: String,
    val topic: String,
    val createdAt: Instant,
    val messages: MutableList<CollaborationMessage>
)

/**
 * 共享资源
 */
@Serializable
data class SharedResource(
    val id: String,
    val name: String,
    val type: String,
    val url: String,
    val sharedBy: String,
    val sharedAt: Instant,
    val description: String = ""
)

/**
 * 协作学习统计
 */
@Serializable
data class CollaborationStatistics(
    val sessionId: String,
    val totalParticipants: Int,
    val activeParticipants: Int,
    val totalMessages: Int,
    val totalNotes: Int,
    val totalAnnotations: Int,
    val sessionDuration: Long, // 分钟
    val participantEngagement: Map<String, ParticipantEngagement>
)

/**
 * 参与者参与度
 */
@Serializable
data class ParticipantEngagement(
    val userId: String,
    val messagesSent: Int,
    val notesCreated: Int,
    val notesEdited: Int,
    val annotationsAdded: Int,
    val timeSpent: Long, // 分钟
    val engagementScore: Double
)

// 结果类型定义

/**
 * 协作会话结果
 */
sealed class CollaborationSessionResult {
    data class Success(val sessionId: String, val message: String) : CollaborationSessionResult()
    data class Failure(val error: String) : CollaborationSessionResult()
}

/**
 * 加入协作结果
 */
sealed class CollaborationJoinResult {
    data class Success(val session: CollaborationSession, val message: String) : CollaborationJoinResult()
    data class Failure(val error: String) : CollaborationJoinResult()
}

/**
 * 消息发送结果
 */
sealed class MessageSendResult {
    data class Success(val messageId: String, val message: String) : MessageSendResult()
    data class Failure(val error: String) : MessageSendResult()
}

/**
 * 协作笔记结果
 */
sealed class CollaborativeNoteResult {
    data class Success(val note: CollaborativeNote, val message: String) : CollaborativeNoteResult()
    data class Failure(val error: String) : CollaborativeNoteResult()
}

/**
 * 标注结果
 */
sealed class AnnotationResult {
    data class Success(val annotation: NoteAnnotation, val message: String) : AnnotationResult()
    data class Failure(val error: String) : AnnotationResult()
}

/**
 * 协作结束结果
 */
sealed class CollaborationEndResult {
    data class Success(val message: String) : CollaborationEndResult()
    data class Failure(val error: String) : CollaborationEndResult()
}

/**
 * 协作学习事件
 */
@Serializable
data class CollaborationEvent(
    val id: String,
    val sessionId: String,
    val type: CollaborationEventType,
    val userId: String,
    val timestamp: Instant,
    val data: Map<String, String> = emptyMap()
)

/**
 * 协作事件类型
 */
@Serializable
enum class CollaborationEventType {
    USER_JOINED,
    USER_LEFT,
    MESSAGE_SENT,
    NOTE_CREATED,
    NOTE_UPDATED,
    ANNOTATION_ADDED,
    RESOURCE_SHARED,
    DISCUSSION_STARTED,
    SESSION_PAUSED,
    SESSION_RESUMED,
    SESSION_ENDED
}

/**
 * 实时协作配置
 */
@Serializable
data class CollaborationConfig(
    val maxParticipants: Int = 10,
    val sessionTimeoutMinutes: Int = 120,
    val messageHistoryLimit: Int = 1000,
    val autoSaveIntervalSeconds: Int = 30,
    val enableVoiceChat: Boolean = false,
    val enableVideoChat: Boolean = false,
    val enableScreenShare: Boolean = false,
    val allowAnonymousParticipants: Boolean = false
)

/**
 * 协作权限
 */
@Serializable
data class CollaborationPermissions(
    val canCreateNotes: Boolean = true,
    val canEditNotes: Boolean = true,
    val canDeleteNotes: Boolean = false,
    val canAddAnnotations: Boolean = true,
    val canShareResources: Boolean = true,
    val canModerateDiscussion: Boolean = false,
    val canInviteParticipants: Boolean = false,
    val canEndSession: Boolean = false
)

/**
 * 协作学习模式
 */
@Serializable
enum class CollaborationMode {
    STUDY_GROUP,        // 学习小组
    PEER_REVIEW,        // 同伴评议
    PROJECT_WORK,       // 项目协作
    DISCUSSION_FORUM,   // 讨论论坛
    TUTORING_SESSION,   // 辅导会话
    BRAINSTORMING      // 头脑风暴
}
