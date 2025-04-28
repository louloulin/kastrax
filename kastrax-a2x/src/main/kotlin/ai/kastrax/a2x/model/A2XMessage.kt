package ai.kastrax.a2x.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A2X 消息基类，所有 A2X 消息都继承自此类
 */
@Serializable
sealed class A2XMessage {
    /**
     * 消息类型
     */
    abstract val type: String
    
    /**
     * 消息 ID
     */
    abstract val id: String
    
    /**
     * 消息来源
     */
    abstract val source: EntityReference
    
    /**
     * 消息目标
     */
    abstract val target: EntityReference
}

/**
 * 实体引用
 */
@Serializable
data class EntityReference(
    /**
     * 实体 ID
     */
    val id: String,
    
    /**
     * 实体类型
     */
    val type: EntityType,
    
    /**
     * 实体端点
     */
    val endpoint: String? = null
)

/**
 * 能力请求消息，用于请求实体的能力列表
 */
@Serializable
@SerialName("capability_request")
data class CapabilityRequest(
    override val id: String,
    override val source: EntityReference,
    override val target: EntityReference,
    
    /**
     * 请求的能力 ID，如果为空则请求所有能力
     */
    @SerialName("capability_id")
    val capabilityId: String? = null,
    
    /**
     * 请求元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "capability_request"
) : A2XMessage()

/**
 * 能力响应消息，用于返回实体的能力列表
 */
@Serializable
@SerialName("capability_response")
data class CapabilityResponse(
    override val id: String,
    override val source: EntityReference,
    override val target: EntityReference,
    
    /**
     * 实体的能力列表
     */
    val capabilities: List<Capability>,
    
    /**
     * 响应元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "capability_response"
) : A2XMessage()

/**
 * 调用请求消息，用于调用实体的能力
 */
@Serializable
@SerialName("invoke_request")
data class InvokeRequest(
    override val id: String,
    override val source: EntityReference,
    override val target: EntityReference,
    
    /**
     * 要调用的能力 ID
     */
    @SerialName("capability_id")
    val capabilityId: String,
    
    /**
     * 调用参数
     */
    val parameters: Map<String, JsonElement>,
    
    /**
     * 请求元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "invoke_request"
) : A2XMessage()

/**
 * 调用响应消息，用于返回实体能力的调用结果
 */
@Serializable
@SerialName("invoke_response")
data class InvokeResponse(
    override val id: String,
    override val source: EntityReference,
    override val target: EntityReference,
    
    /**
     * 调用结果
     */
    val result: JsonElement,
    
    /**
     * 响应元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "invoke_response"
) : A2XMessage()

/**
 * 查询请求消息，用于查询实体的状态
 */
@Serializable
@SerialName("query_request")
data class QueryRequest(
    override val id: String,
    override val source: EntityReference,
    override val target: EntityReference,
    
    /**
     * 查询类型
     */
    @SerialName("query_type")
    val queryType: String,
    
    /**
     * 查询参数
     */
    val parameters: Map<String, JsonElement> = emptyMap(),
    
    /**
     * 请求元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "query_request"
) : A2XMessage()

/**
 * 查询响应消息，用于返回实体的状态
 */
@Serializable
@SerialName("query_response")
data class QueryResponse(
    override val id: String,
    override val source: EntityReference,
    override val target: EntityReference,
    
    /**
     * 查询结果
     */
    val result: JsonElement,
    
    /**
     * 响应元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "query_response"
) : A2XMessage()

/**
 * 事件消息，用于通知实体事件
 */
@Serializable
@SerialName("event")
data class EventMessage(
    override val id: String,
    override val source: EntityReference,
    override val target: EntityReference,
    
    /**
     * 事件类型
     */
    @SerialName("event_type")
    val eventType: String,
    
    /**
     * 事件数据
     */
    val data: JsonElement,
    
    /**
     * 事件元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "event"
) : A2XMessage()

/**
 * 错误消息，用于返回错误信息
 */
@Serializable
@SerialName("error")
data class ErrorMessage(
    override val id: String,
    override val source: EntityReference,
    override val target: EntityReference,
    
    /**
     * 错误代码
     */
    val code: String,
    
    /**
     * 错误消息
     */
    val message: String,
    
    /**
     * 错误详情
     */
    val details: JsonElement? = null,
    
    /**
     * 错误元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "error"
) : A2XMessage()
