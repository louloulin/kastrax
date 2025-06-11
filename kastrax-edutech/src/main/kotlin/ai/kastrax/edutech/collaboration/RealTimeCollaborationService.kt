package ai.kastrax.edutech.collaboration

import ai.kastrax.edutech.models.*
import ai.kastrax.edutech.auth.AuthService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

/**
 * 实时协作学习服务
 * 
 * Week 17-18 高级扩展功能：
 * - 实时协作学习空间
 * - 多人同步学习
 * - 实时讨论和问答
 * - 协作笔记和标注
 * - 学习小组管理
 */
class RealTimeCollaborationService(
    private val authService: AuthService
) {
    
    private val collaborationSessions = ConcurrentHashMap<String, CollaborationSession>()
    private val userConnections = ConcurrentHashMap<String, MutableSet<String>>()
    private val messageFlows = ConcurrentHashMap<String, MutableSharedFlow<CollaborationMessage>>()
    
    /**
     * 创建协作学习会话
     */
    suspend fun createCollaborationSession(
        creatorId: String,
        courseId: CourseId,
        title: String,
        description: String,
        maxParticipants: Int = 10
    ): CollaborationSessionResult {
        return try {
            val sessionId = generateSessionId()
            val session = CollaborationSession(
                id = sessionId,
                courseId = courseId,
                title = title,
                description = description,
                creatorId = creatorId,
                participants = mutableSetOf(creatorId),
                maxParticipants = maxParticipants,
                status = CollaborationStatus.ACTIVE,
                createdAt = Clock.System.now(),
                sharedResources = mutableListOf(),
                discussionThreads = mutableListOf(),
                collaborativeNotes = mutableMapOf()
            )
            
            collaborationSessions[sessionId] = session
            messageFlows[sessionId] = MutableSharedFlow()
            userConnections[creatorId] = mutableSetOf(sessionId)
            
            // 发送会话创建通知
            broadcastMessage(sessionId, CollaborationMessage(
                type = MessageType.SESSION_CREATED,
                senderId = "system",
                content = "协作学习会话已创建: $title",
                timestamp = Clock.System.now()
            ))
            
            CollaborationSessionResult.Success(sessionId, "协作学习会话创建成功")
        } catch (e: Exception) {
            CollaborationSessionResult.Failure("创建协作会话失败: ${e.message}")
        }
    }
    
    /**
     * 加入协作学习会话
     */
    suspend fun joinCollaborationSession(
        userId: String,
        sessionId: String
    ): CollaborationJoinResult {
        val session = collaborationSessions[sessionId]
            ?: return CollaborationJoinResult.Failure("协作会话不存在")
        
        if (session.participants.size >= session.maxParticipants) {
            return CollaborationJoinResult.Failure("协作会话已满")
        }
        
        if (session.status != CollaborationStatus.ACTIVE) {
            return CollaborationJoinResult.Failure("协作会话已结束")
        }
        
        // 添加参与者
        session.participants.add(userId)
        userConnections.getOrPut(userId) { mutableSetOf() }.add(sessionId)
        
        // 发送加入通知
        broadcastMessage(sessionId, CollaborationMessage(
            type = MessageType.USER_JOINED,
            senderId = "system",
            content = "用户 $userId 加入了协作学习",
            timestamp = Clock.System.now()
        ))
        
        return CollaborationJoinResult.Success(session, "成功加入协作学习会话")
    }
    
    /**
     * 发送协作消息
     */
    suspend fun sendMessage(
        sessionId: String,
        senderId: String,
        messageType: MessageType,
        content: String,
        metadata: Map<String, String> = emptyMap()
    ): MessageSendResult {
        val session = collaborationSessions[sessionId]
            ?: return MessageSendResult.Failure("协作会话不存在")
        
        if (!session.participants.contains(senderId)) {
            return MessageSendResult.Failure("用户未参与此协作会话")
        }
        
        val message = CollaborationMessage(
            id = generateMessageId(),
            type = messageType,
            senderId = senderId,
            content = content,
            timestamp = Clock.System.now(),
            metadata = metadata
        )
        
        // 处理不同类型的消息
        when (messageType) {
            MessageType.DISCUSSION -> {
                // 添加到讨论线程
                val threadId = metadata["threadId"] ?: createDiscussionThread(session, content)
                addToDiscussionThread(session, threadId, message)
            }
            MessageType.COLLABORATIVE_NOTE -> {
                // 更新协作笔记
                updateCollaborativeNote(session, message)
            }
            MessageType.RESOURCE_SHARE -> {
                // 添加共享资源
                addSharedResource(session, message)
            }
            else -> {
                // 普通消息
            }
        }
        
        // 广播消息
        broadcastMessage(sessionId, message)
        
        return MessageSendResult.Success(message.id, "消息发送成功")
    }
    
    /**
     * 获取协作会话的消息流
     */
    fun getMessageFlow(sessionId: String): Flow<CollaborationMessage>? {
        return messageFlows[sessionId]?.asSharedFlow()
    }
    
    /**
     * 创建协作笔记
     */
    suspend fun createCollaborativeNote(
        sessionId: String,
        creatorId: String,
        title: String,
        content: String
    ): CollaborativeNoteResult {
        val session = collaborationSessions[sessionId]
            ?: return CollaborativeNoteResult.Failure("协作会话不存在")
        
        if (!session.participants.contains(creatorId)) {
            return CollaborativeNoteResult.Failure("用户未参与此协作会话")
        }
        
        val noteId = generateNoteId()
        val note = CollaborativeNote(
            id = noteId,
            title = title,
            content = content,
            creatorId = creatorId,
            contributors = mutableSetOf(creatorId),
            createdAt = Clock.System.now(),
            lastModified = Clock.System.now(),
            version = 1,
            annotations = mutableListOf()
        )
        
        session.collaborativeNotes[noteId] = note
        
        // 通知其他参与者
        broadcastMessage(sessionId, CollaborationMessage(
            type = MessageType.NOTE_CREATED,
            senderId = creatorId,
            content = "创建了协作笔记: $title",
            timestamp = Clock.System.now(),
            metadata = mapOf("noteId" to noteId)
        ))
        
        return CollaborativeNoteResult.Success(note, "协作笔记创建成功")
    }
    
    /**
     * 更新协作笔记
     */
    suspend fun updateCollaborativeNote(
        sessionId: String,
        noteId: String,
        userId: String,
        newContent: String
    ): CollaborativeNoteResult {
        val session = collaborationSessions[sessionId]
            ?: return CollaborativeNoteResult.Failure("协作会话不存在")
        
        val note = session.collaborativeNotes[noteId]
            ?: return CollaborativeNoteResult.Failure("协作笔记不存在")
        
        if (!session.participants.contains(userId)) {
            return CollaborativeNoteResult.Failure("用户未参与此协作会话")
        }
        
        // 更新笔记
        note.content = newContent
        note.contributors.add(userId)
        note.lastModified = Clock.System.now()
        note.version++
        
        // 通知其他参与者
        broadcastMessage(sessionId, CollaborationMessage(
            type = MessageType.NOTE_UPDATED,
            senderId = userId,
            content = "更新了协作笔记: ${note.title}",
            timestamp = Clock.System.now(),
            metadata = mapOf("noteId" to noteId, "version" to note.version.toString())
        ))
        
        return CollaborativeNoteResult.Success(note, "协作笔记更新成功")
    }
    
    /**
     * 添加笔记标注
     */
    suspend fun addNoteAnnotation(
        sessionId: String,
        noteId: String,
        userId: String,
        annotation: NoteAnnotation
    ): AnnotationResult {
        val session = collaborationSessions[sessionId]
            ?: return AnnotationResult.Failure("协作会话不存在")
        
        val note = session.collaborativeNotes[noteId]
            ?: return AnnotationResult.Failure("协作笔记不存在")
        
        if (!session.participants.contains(userId)) {
            return AnnotationResult.Failure("用户未参与此协作会话")
        }
        
        val annotationWithId = annotation.copy(
            id = generateAnnotationId(),
            authorId = userId,
            createdAt = Clock.System.now()
        )
        
        note.annotations.add(annotationWithId)
        
        // 通知其他参与者
        broadcastMessage(sessionId, CollaborationMessage(
            type = MessageType.ANNOTATION_ADDED,
            senderId = userId,
            content = "添加了标注: ${annotation.content}",
            timestamp = Clock.System.now(),
            metadata = mapOf("noteId" to noteId, "annotationId" to annotationWithId.id)
        ))
        
        return AnnotationResult.Success(annotationWithId, "标注添加成功")
    }
    
    /**
     * 获取协作会话信息
     */
    fun getCollaborationSession(sessionId: String): CollaborationSession? {
        return collaborationSessions[sessionId]
    }
    
    /**
     * 获取用户参与的协作会话
     */
    fun getUserCollaborationSessions(userId: String): List<CollaborationSession> {
        val userSessionIds = userConnections[userId] ?: return emptyList()
        return userSessionIds.mapNotNull { collaborationSessions[it] }
    }
    
    /**
     * 结束协作会话
     */
    suspend fun endCollaborationSession(sessionId: String, userId: String): CollaborationEndResult {
        val session = collaborationSessions[sessionId]
            ?: return CollaborationEndResult.Failure("协作会话不存在")
        
        if (session.creatorId != userId) {
            return CollaborationEndResult.Failure("只有创建者可以结束协作会话")
        }
        
        session.status = CollaborationStatus.ENDED
        session.endedAt = Clock.System.now()
        
        // 通知所有参与者
        broadcastMessage(sessionId, CollaborationMessage(
            type = MessageType.SESSION_ENDED,
            senderId = "system",
            content = "协作学习会话已结束",
            timestamp = Clock.System.now()
        ))
        
        // 清理连接
        session.participants.forEach { participantId ->
            userConnections[participantId]?.remove(sessionId)
        }
        
        return CollaborationEndResult.Success("协作会话已结束")
    }
    
    // 私有辅助方法
    private suspend fun broadcastMessage(sessionId: String, message: CollaborationMessage) {
        messageFlows[sessionId]?.emit(message)
    }
    
    private fun createDiscussionThread(session: CollaborationSession, topic: String): String {
        val threadId = generateThreadId()
        val thread = DiscussionThread(
            id = threadId,
            topic = topic,
            createdAt = Clock.System.now(),
            messages = mutableListOf()
        )
        session.discussionThreads.add(thread)
        return threadId
    }
    
    private fun addToDiscussionThread(session: CollaborationSession, threadId: String, message: CollaborationMessage) {
        val thread = session.discussionThreads.find { it.id == threadId }
        thread?.messages?.add(message)
    }
    
    private fun updateCollaborativeNote(session: CollaborationSession, message: CollaborationMessage) {
        val noteId = message.metadata["noteId"] ?: return
        val note = session.collaborativeNotes[noteId] ?: return
        
        note.content = message.content
        note.contributors.add(message.senderId)
        note.lastModified = message.timestamp
        note.version++
    }
    
    private fun addSharedResource(session: CollaborationSession, message: CollaborationMessage) {
        val resource = SharedResource(
            id = generateResourceId(),
            name = message.metadata["resourceName"] ?: "未命名资源",
            type = message.metadata["resourceType"] ?: "unknown",
            url = message.metadata["resourceUrl"] ?: "",
            sharedBy = message.senderId,
            sharedAt = message.timestamp
        )
        session.sharedResources.add(resource)
    }
    
    // ID生成方法
    private fun generateSessionId(): String = "session_${java.util.UUID.randomUUID()}"
    private fun generateMessageId(): String = "msg_${java.util.UUID.randomUUID()}"
    private fun generateNoteId(): String = "note_${java.util.UUID.randomUUID()}"
    private fun generateAnnotationId(): String = "ann_${java.util.UUID.randomUUID()}"
    private fun generateThreadId(): String = "thread_${java.util.UUID.randomUUID()}"
    private fun generateResourceId(): String = "res_${java.util.UUID.randomUUID()}"
}
