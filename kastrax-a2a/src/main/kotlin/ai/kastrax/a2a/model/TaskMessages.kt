package ai.kastrax.a2a.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-RPC 消息基类
 */
@Serializable
sealed class JSONRPCMessage {
    /**
     * JSON-RPC 版本
     */
    @SerialName("jsonrpc")
    val jsonrpc: String = "2.0"
    
    /**
     * 消息 ID
     */
    val id: String? = null
}

/**
 * JSON-RPC 请求
 */
@Serializable
sealed class JSONRPCRequest : JSONRPCMessage() {
    /**
     * 方法名
     */
    abstract val method: String
}

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
    val data: Any? = null
)

/**
 * JSON-RPC 响应
 */
@Serializable
data class JSONRPCResponse(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 结果
     */
    val result: Any? = null,
    
    /**
     * 错误
     */
    val error: JSONRPCError? = null
) : JSONRPCMessage()

/**
 * 发送任务请求
 */
@Serializable
@SerialName("tasks/send")
data class SendTaskRequest(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 方法名
     */
    override val method: String = "tasks/send",
    
    /**
     * 参数
     */
    val params: TaskSendParams
) : JSONRPCRequest()

/**
 * 发送任务响应
 */
@Serializable
data class SendTaskResponse(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 结果
     */
    val result: Task? = null,
    
    /**
     * 错误
     */
    val error: JSONRPCError? = null
) : JSONRPCMessage()

/**
 * 发送任务并订阅更新请求
 */
@Serializable
@SerialName("tasks/sendSubscribe")
data class SendTaskStreamingRequest(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 方法名
     */
    override val method: String = "tasks/sendSubscribe",
    
    /**
     * 参数
     */
    val params: TaskSendParams
) : JSONRPCRequest()

/**
 * 发送任务并订阅更新响应
 */
@Serializable
data class SendTaskStreamingResponse(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 结果
     */
    val result: TaskStatusUpdateEvent? = null,
    
    /**
     * 错误
     */
    val error: JSONRPCError? = null
) : JSONRPCMessage()

/**
 * 获取任务请求
 */
@Serializable
@SerialName("tasks/get")
data class GetTaskRequest(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 方法名
     */
    override val method: String = "tasks/get",
    
    /**
     * 参数
     */
    val params: TaskQueryParams
) : JSONRPCRequest()

/**
 * 获取任务响应
 */
@Serializable
data class GetTaskResponse(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 结果
     */
    val result: Task? = null,
    
    /**
     * 错误
     */
    val error: JSONRPCError? = null
) : JSONRPCMessage()

/**
 * 取消任务请求
 */
@Serializable
@SerialName("tasks/cancel")
data class CancelTaskRequest(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 方法名
     */
    override val method: String = "tasks/cancel",
    
    /**
     * 参数
     */
    val params: TaskIdParams
) : JSONRPCRequest()

/**
 * 取消任务响应
 */
@Serializable
data class CancelTaskResponse(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 结果
     */
    val result: Task? = null,
    
    /**
     * 错误
     */
    val error: JSONRPCError? = null
) : JSONRPCMessage()

/**
 * 设置任务推送通知请求
 */
@Serializable
@SerialName("tasks/pushNotification/set")
data class SetTaskPushNotificationRequest(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 方法名
     */
    override val method: String = "tasks/pushNotification/set",
    
    /**
     * 参数
     */
    val params: TaskPushNotificationConfig
) : JSONRPCRequest()

/**
 * 设置任务推送通知响应
 */
@Serializable
data class SetTaskPushNotificationResponse(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 结果
     */
    val result: TaskPushNotificationConfig? = null,
    
    /**
     * 错误
     */
    val error: JSONRPCError? = null
) : JSONRPCMessage()

/**
 * 获取任务推送通知请求
 */
@Serializable
@SerialName("tasks/pushNotification/get")
data class GetTaskPushNotificationRequest(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 方法名
     */
    override val method: String = "tasks/pushNotification/get",
    
    /**
     * 参数
     */
    val params: TaskIdParams
) : JSONRPCRequest()

/**
 * 获取任务推送通知响应
 */
@Serializable
data class GetTaskPushNotificationResponse(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 结果
     */
    val result: TaskPushNotificationConfig? = null,
    
    /**
     * 错误
     */
    val error: JSONRPCError? = null
) : JSONRPCMessage()

/**
 * 重新订阅任务请求
 */
@Serializable
@SerialName("tasks/resubscribe")
data class TaskResubscriptionRequest(
    /**
     * 消息 ID
     */
    override val id: String? = null,
    
    /**
     * 方法名
     */
    override val method: String = "tasks/resubscribe",
    
    /**
     * 参数
     */
    val params: TaskIdParams
) : JSONRPCRequest()

/**
 * A2A 请求类型
 */
@Serializable
sealed class A2ARequest {
    /**
     * 方法名
     */
    abstract val method: String
}

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
    /**
     * 错误代码
     */
    val code: Int = JSONRPCErrorCodes.PARSE_ERROR,
    
    /**
     * 错误消息
     */
    val message: String = "Invalid JSON payload",
    
    /**
     * 错误数据
     */
    val data: Any? = null
) : JSONRPCError(code, message, data)

/**
 * 无效请求错误
 */
@Serializable
data class InvalidRequestError(
    /**
     * 错误代码
     */
    val code: Int = JSONRPCErrorCodes.INVALID_REQUEST,
    
    /**
     * 错误消息
     */
    val message: String = "Request payload validation error",
    
    /**
     * 错误数据
     */
    val data: Any? = null
) : JSONRPCError(code, message, data)

/**
 * 方法不存在错误
 */
@Serializable
data class MethodNotFoundError(
    /**
     * 错误代码
     */
    val code: Int = JSONRPCErrorCodes.METHOD_NOT_FOUND,
    
    /**
     * 错误消息
     */
    val message: String = "Method not found",
    
    /**
     * 错误数据
     */
    val data: Any? = null
) : JSONRPCError(code, message, data)

/**
 * 无效参数错误
 */
@Serializable
data class InvalidParamsError(
    /**
     * 错误代码
     */
    val code: Int = JSONRPCErrorCodes.INVALID_PARAMS,
    
    /**
     * 错误消息
     */
    val message: String = "Invalid parameters",
    
    /**
     * 错误数据
     */
    val data: Any? = null
) : JSONRPCError(code, message, data)

/**
 * 内部错误
 */
@Serializable
data class InternalError(
    /**
     * 错误代码
     */
    val code: Int = JSONRPCErrorCodes.INTERNAL_ERROR,
    
    /**
     * 错误消息
     */
    val message: String = "Internal error",
    
    /**
     * 错误数据
     */
    val data: Any? = null
) : JSONRPCError(code, message, data)

/**
 * 任务不存在错误
 */
@Serializable
data class TaskNotFoundError(
    /**
     * 错误代码
     */
    val code: Int = JSONRPCErrorCodes.TASK_NOT_FOUND,
    
    /**
     * 错误消息
     */
    val message: String = "Task not found",
    
    /**
     * 错误数据
     */
    val data: Any? = null
) : JSONRPCError(code, message, data)

/**
 * 任务不可取消错误
 */
@Serializable
data class TaskNotCancelableError(
    /**
     * 错误代码
     */
    val code: Int = JSONRPCErrorCodes.TASK_NOT_CANCELABLE,
    
    /**
     * 错误消息
     */
    val message: String = "Task cannot be canceled",
    
    /**
     * 错误数据
     */
    val data: Any? = null
) : JSONRPCError(code, message, data)

/**
 * 不支持推送通知错误
 */
@Serializable
data class PushNotificationNotSupportedError(
    /**
     * 错误代码
     */
    val code: Int = JSONRPCErrorCodes.PUSH_NOTIFICATION_NOT_SUPPORTED,
    
    /**
     * 错误消息
     */
    val message: String = "Push Notification is not supported",
    
    /**
     * 错误数据
     */
    val data: Any? = null
) : JSONRPCError(code, message, data)

/**
 * 不支持的操作错误
 */
@Serializable
data class UnsupportedOperationError(
    /**
     * 错误代码
     */
    val code: Int = JSONRPCErrorCodes.UNSUPPORTED_OPERATION,
    
    /**
     * 错误消息
     */
    val message: String = "This operation is not supported",
    
    /**
     * 错误数据
     */
    val data: Any? = null
) : JSONRPCError(code, message, data)

/**
 * 不支持的内容类型错误
 */
@Serializable
data class ContentTypeNotSupportedError(
    /**
     * 错误代码
     */
    val code: Int = JSONRPCErrorCodes.CONTENT_TYPE_NOT_SUPPORTED,
    
    /**
     * 错误消息
     */
    val message: String = "Incompatible content types",
    
    /**
     * 错误数据
     */
    val data: Any? = null
) : JSONRPCError(code, message, data)
