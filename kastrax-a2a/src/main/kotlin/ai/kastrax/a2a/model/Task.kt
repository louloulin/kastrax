package ai.kastrax.a2a.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import java.util.UUID

/**
 * 任务状态
 */
enum class TaskState {
    /**
     * 已提交
     */
    SUBMITTED,
    
    /**
     * 处理中
     */
    WORKING,
    
    /**
     * 需要输入
     */
    INPUT_REQUIRED,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 已取消
     */
    CANCELED,
    
    /**
     * 失败
     */
    FAILED,
    
    /**
     * 未知
     */
    UNKNOWN
}

/**
 * 任务状态信息
 */
@Serializable
data class TaskStatus(
    /**
     * 任务状态
     */
    val state: TaskState,
    
    /**
     * 状态消息
     */
    val message: String? = null,
    
    /**
     * 状态时间戳
     */
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 任务部分内容
 */
@Serializable
sealed class TaskPart {
    /**
     * 部分类型
     */
    abstract val type: String
    
    /**
     * 元数据
     */
    val metadata: Map<String, String>? = null
}

/**
 * 文本部分
 */
@Serializable
@SerialName("text")
data class TextPart(
    /**
     * 文本内容
     */
    val text: String,
    
    override val type: String = "text"
) : TaskPart()

/**
 * 文件内容
 */
@Serializable
data class FileContent(
    /**
     * 文件名
     */
    val name: String? = null,
    
    /**
     * MIME 类型
     */
    @SerialName("mimeType")
    val mimeType: String? = null,
    
    /**
     * 文件字节（Base64 编码）
     */
    val bytes: String? = null,
    
    /**
     * 文件 URI
     */
    val uri: String? = null
)

/**
 * 文件部分
 */
@Serializable
@SerialName("file")
data class FilePart(
    /**
     * 文件内容
     */
    val file: FileContent,
    
    override val type: String = "file"
) : TaskPart()

/**
 * 数据部分
 */
@Serializable
@SerialName("data")
data class DataPart(
    /**
     * 数据内容
     */
    val data: Map<String, JsonElement>,
    
    override val type: String = "data"
) : TaskPart()

/**
 * 消息
 */
@Serializable
data class Message(
    /**
     * 角色
     */
    val role: String,
    
    /**
     * 部分内容
     */
    val parts: List<TaskPart>,
    
    /**
     * 元数据
     */
    val metadata: Map<String, String>? = null
)

/**
 * 任务产物
 */
@Serializable
data class Artifact(
    /**
     * 产物名称
     */
    val name: String? = null,
    
    /**
     * 产物描述
     */
    val description: String? = null,
    
    /**
     * 部分内容
     */
    val parts: List<TaskPart>,
    
    /**
     * 元数据
     */
    val metadata: Map<String, String>? = null,
    
    /**
     * 索引
     */
    val index: Int = 0,
    
    /**
     * 是否追加
     */
    val append: Boolean? = null,
    
    /**
     * 是否最后一块
     */
    @SerialName("lastChunk")
    val lastChunk: Boolean? = null
)

/**
 * 任务
 */
@Serializable
data class Task(
    /**
     * 任务 ID
     */
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * 会话 ID
     */
    @SerialName("sessionId")
    val sessionId: String? = null,
    
    /**
     * 任务状态
     */
    val status: TaskStatus,
    
    /**
     * 任务产物
     */
    val artifacts: List<Artifact>? = null,
    
    /**
     * 任务历史
     */
    val history: List<Message>? = null,
    
    /**
     * 元数据
     */
    val metadata: Map<String, String>? = null
)

/**
 * 任务状态更新事件
 */
@Serializable
data class TaskStatusUpdateEvent(
    /**
     * 任务 ID
     */
    val id: String,
    
    /**
     * 任务状态
     */
    val status: TaskStatus,
    
    /**
     * 是否最终状态
     */
    val final: Boolean = false,
    
    /**
     * 元数据
     */
    val metadata: Map<String, String>? = null
)

/**
 * 任务产物更新事件
 */
@Serializable
data class TaskArtifactUpdateEvent(
    /**
     * 任务 ID
     */
    val id: String,
    
    /**
     * 任务产物
     */
    val artifact: Artifact,
    
    /**
     * 元数据
     */
    val metadata: Map<String, String>? = null
)

/**
 * 推送通知配置
 */
@Serializable
data class PushNotificationConfig(
    /**
     * 通知 URL
     */
    val url: String,
    
    /**
     * 通知令牌
     */
    val token: String? = null,
    
    /**
     * 认证信息
     */
    val authentication: AuthenticationInfo? = null
)

/**
 * 认证信息
 */
@Serializable
data class AuthenticationInfo(
    /**
     * 认证方案
     */
    val schemes: List<String>,
    
    /**
     * 认证凭证
     */
    val credentials: String? = null
)

/**
 * 任务 ID 参数
 */
@Serializable
data class TaskIdParams(
    /**
     * 任务 ID
     */
    val id: String,
    
    /**
     * 元数据
     */
    val metadata: Map<String, String>? = null
)

/**
 * 任务查询参数
 */
@Serializable
data class TaskQueryParams(
    /**
     * 任务 ID
     */
    val id: String,
    
    /**
     * 历史长度
     */
    @SerialName("historyLength")
    val historyLength: Int? = null,
    
    /**
     * 元数据
     */
    val metadata: Map<String, String>? = null
)

/**
 * 任务发送参数
 */
@Serializable
data class TaskSendParams(
    /**
     * 任务 ID
     */
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * 会话 ID
     */
    @SerialName("sessionId")
    val sessionId: String = UUID.randomUUID().toString(),
    
    /**
     * 消息
     */
    val message: Message,
    
    /**
     * 接受的输出模式
     */
    @SerialName("acceptedOutputModes")
    val acceptedOutputModes: List<String>? = null,
    
    /**
     * 推送通知
     */
    @SerialName("pushNotification")
    val pushNotification: PushNotificationConfig? = null,
    
    /**
     * 历史长度
     */
    @SerialName("historyLength")
    val historyLength: Int? = null,
    
    /**
     * 元数据
     */
    val metadata: Map<String, String>? = null
)

/**
 * 任务推送通知配置
 */
@Serializable
data class TaskPushNotificationConfig(
    /**
     * 任务 ID
     */
    val id: String,
    
    /**
     * 推送通知配置
     */
    @SerialName("pushNotificationConfig")
    val pushNotificationConfig: PushNotificationConfig
)
