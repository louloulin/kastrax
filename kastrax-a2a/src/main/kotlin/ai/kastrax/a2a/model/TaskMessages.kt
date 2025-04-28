package ai.kastrax.a2a.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * JSON-RPC 错误
 */
@Serializable
data class JSONRPCError(
    /**
     * 错误代码
     */
    val code: Int,

    /**
     * 错误消息
     */
    val message: String,

    /**
     * 错误数据
     */
    val data: JsonElement? = null
)

/**
 * JSON-RPC 响应
 */
@Serializable
data class JSONRPCResponse(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val responseId: String? = null,

    /**
     * 结果
     */
    val result: JsonElement? = null,

    /**
     * 错误
     */
    val error: JSONRPCError? = null,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val jsonrpc: String = "2.0"
)

/**
 * 发送任务请求
 */
@Serializable
@SerialName("tasks/send")
data class SendTaskRequest(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val requestId: String? = null,

    /**
     * 方法名
     */
    val method: String = "tasks/send",

    /**
     * 参数
     */
    val params: TaskSendParams,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val jsonrpc: String = "2.0"
)

/**
 * 发送任务响应
 */
@Serializable
data class SendTaskResponse(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val responseId: String? = null,

    /**
     * 结果
     */
    val result: Task? = null,

    /**
     * 错误
     */
    val error: JSONRPCError? = null,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val responseJsonrpc: String = "2.0"
)

/**
 * 发送任务并订阅更新请求
 */
@Serializable
@SerialName("tasks/sendSubscribe")
data class SendTaskStreamingRequest(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val requestId: String? = null,

    /**
     * 方法名
     */
    val method: String = "tasks/sendSubscribe",

    /**
     * 参数
     */
    val params: TaskSendParams,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val requestJsonrpc: String = "2.0"
)

/**
 * 发送任务并订阅更新响应
 */
@Serializable
data class SendTaskStreamingResponse(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val responseId: String? = null,

    /**
     * 结果
     */
    val result: TaskStatusUpdateEvent? = null,

    /**
     * 错误
     */
    val error: JSONRPCError? = null,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val responseJsonrpc: String = "2.0"
)

/**
 * 获取任务请求
 */
@Serializable
@SerialName("tasks/get")
data class GetTaskRequest(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val requestId: String? = null,

    /**
     * 方法名
     */
    val method: String = "tasks/get",

    /**
     * 参数
     */
    val params: TaskQueryParams,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val requestJsonrpc: String = "2.0"
)

/**
 * 获取任务响应
 */
@Serializable
data class GetTaskResponse(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val responseId: String? = null,

    /**
     * 结果
     */
    val result: Task? = null,

    /**
     * 错误
     */
    val error: JSONRPCError? = null,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val responseJsonrpc: String = "2.0"
)

/**
 * 取消任务请求
 */
@Serializable
@SerialName("tasks/cancel")
data class CancelTaskRequest(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val requestId: String? = null,

    /**
     * 方法名
     */
    val method: String = "tasks/cancel",

    /**
     * 参数
     */
    val params: TaskIdParams,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val requestJsonrpc: String = "2.0"
)

/**
 * 取消任务响应
 */
@Serializable
data class CancelTaskResponse(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val responseId: String? = null,

    /**
     * 结果
     */
    val result: Task? = null,

    /**
     * 错误
     */
    val error: JSONRPCError? = null,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val responseJsonrpc: String = "2.0"
)

/**
 * 设置任务推送通知请求
 */
@Serializable
@SerialName("tasks/pushNotification/set")
data class SetTaskPushNotificationRequest(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val requestId: String? = null,

    /**
     * 方法名
     */
    val method: String = "tasks/pushNotification/set",

    /**
     * 参数
     */
    val params: TaskPushNotificationConfig,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val requestJsonrpc: String = "2.0"
)

/**
 * 设置任务推送通知响应
 */
@Serializable
data class SetTaskPushNotificationResponse(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val responseId: String? = null,

    /**
     * 结果
     */
    val result: TaskPushNotificationConfig? = null,

    /**
     * 错误
     */
    val error: JSONRPCError? = null,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val responseJsonrpc: String = "2.0"
)

/**
 * 获取任务推送通知请求
 */
@Serializable
@SerialName("tasks/pushNotification/get")
data class GetTaskPushNotificationRequest(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val requestId: String? = null,

    /**
     * 方法名
     */
    val method: String = "tasks/pushNotification/get",

    /**
     * 参数
     */
    val params: TaskIdParams,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val requestJsonrpc: String = "2.0"
)

/**
 * 获取任务推送通知响应
 */
@Serializable
data class GetTaskPushNotificationResponse(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val responseId: String? = null,

    /**
     * 结果
     */
    val result: TaskPushNotificationConfig? = null,

    /**
     * 错误
     */
    val error: JSONRPCError? = null,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val responseJsonrpc: String = "2.0"
)

/**
 * 重新订阅任务请求
 */
@Serializable
@SerialName("tasks/resubscribe")
data class TaskResubscriptionRequest(
    /**
     * 消息 ID
     */
    @SerialName("id")
    val requestId: String? = null,

    /**
     * 方法名
     */
    val method: String = "tasks/resubscribe",

    /**
     * 参数
     */
    val params: TaskIdParams,

    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val requestJsonrpc: String = "2.0"
)



/**
 * JSON-RPC 错误类型
 */
object JSONRPCErrorCodes {
    /**
     * 解析错误
     */
    const val PARSE_ERROR = -32700

    /**
     * 无效请求
     */
    const val INVALID_REQUEST = -32600

    /**
     * 方法不存在
     */
    const val METHOD_NOT_FOUND = -32601

    /**
     * 无效参数
     */
    const val INVALID_PARAMS = -32602

    /**
     * 内部错误
     */
    const val INTERNAL_ERROR = -32603

    /**
     * 任务不存在
     */
    const val TASK_NOT_FOUND = -32001

    /**
     * 任务不可取消
     */
    const val TASK_NOT_CANCELABLE = -32002

    /**
     * 不支持推送通知
     */
    const val PUSH_NOTIFICATION_NOT_SUPPORTED = -32003

    /**
     * 不支持的操作
     */
    const val UNSUPPORTED_OPERATION = -32004

    /**
     * 不支持的内容类型
     */
    const val CONTENT_TYPE_NOT_SUPPORTED = -32005
}

/**
 * JSON 解析错误
 */
@Serializable
data class JSONParseError(
    val code: Int = JSONRPCErrorCodes.PARSE_ERROR,
    val message: String = "Invalid JSON payload",
    val data: JsonElement? = null
)

/**
 * 无效请求错误
 */
@Serializable
data class InvalidRequestError(
    val code: Int = JSONRPCErrorCodes.INVALID_REQUEST,
    val message: String = "Request payload validation error",
    val data: JsonElement? = null
)

/**
 * 方法不存在错误
 */
@Serializable
data class MethodNotFoundError(
    val code: Int = JSONRPCErrorCodes.METHOD_NOT_FOUND,
    val message: String = "Method not found",
    val data: JsonElement? = null
)

/**
 * 无效参数错误
 */
@Serializable
data class InvalidParamsError(
    val code: Int = JSONRPCErrorCodes.INVALID_PARAMS,
    val message: String = "Invalid parameters",
    val data: JsonElement? = null
)

/**
 * 内部错误
 */
@Serializable
data class InternalError(
    val code: Int = JSONRPCErrorCodes.INTERNAL_ERROR,
    val message: String = "Internal error",
    val data: JsonElement? = null
)

/**
 * 任务不存在错误
 */
@Serializable
data class TaskNotFoundError(
    val code: Int = JSONRPCErrorCodes.TASK_NOT_FOUND,
    val message: String = "Task not found",
    val data: JsonElement? = null
)

/**
 * 任务不可取消错误
 */
@Serializable
data class TaskNotCancelableError(
    val code: Int = JSONRPCErrorCodes.TASK_NOT_CANCELABLE,
    val message: String = "Task cannot be canceled",
    val data: JsonElement? = null
)

/**
 * 不支持推送通知错误
 */
@Serializable
data class PushNotificationNotSupportedError(
    val code: Int = JSONRPCErrorCodes.PUSH_NOTIFICATION_NOT_SUPPORTED,
    val message: String = "Push Notification is not supported",
    val data: JsonElement? = null
)

/**
 * 不支持的操作错误
 */
@Serializable
data class UnsupportedOperationError(
    val code: Int = JSONRPCErrorCodes.UNSUPPORTED_OPERATION,
    val message: String = "This operation is not supported",
    val data: JsonElement? = null
)

/**
 * 不支持的内容类型错误
 */
@Serializable
data class ContentTypeNotSupportedError(
    val code: Int = JSONRPCErrorCodes.CONTENT_TYPE_NOT_SUPPORTED,
    val message: String = "Incompatible content types",
    val data: JsonElement? = null
)
