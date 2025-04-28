package ai.kastrax.a2a.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A2A 消息基类，所有 A2A 消息都继承自此类
 */
@Serializable
sealed class A2AMessage {
    /**
     * 消息类型
     */
    abstract val type: String
    
    /**
     * 消息 ID
     */
    abstract val id: String
}

/**
 * 能力请求消息，用于请求代理的能力列表
 */
@Serializable
@SerialName("capability_request")
data class CapabilityRequest(
    override val id: String,
    
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
) : A2AMessage()

/**
 * 能力响应消息，用于返回代理的能力列表
 */
@Serializable
@SerialName("capability_response")
data class CapabilityResponse(
    override val id: String,
    
    /**
     * 代理的能力列表
     */
    val capabilities: List<Capability>,
    
    /**
     * 响应元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "capability_response"
) : A2AMessage()

/**
 * 调用请求消息，用于调用代理的能力
 */
@Serializable
@SerialName("invoke_request")
data class InvokeRequest(
    override val id: String,
    
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
) : A2AMessage()

/**
 * 调用响应消息，用于返回代理能力的调用结果
 */
@Serializable
@SerialName("invoke_response")
data class InvokeResponse(
    override val id: String,
    
    /**
     * 调用结果
     */
    val result: JsonElement,
    
    /**
     * 响应元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "invoke_response"
) : A2AMessage()

/**
 * 查询请求消息，用于查询代理的状态
 */
@Serializable
@SerialName("query_request")
data class QueryRequest(
    override val id: String,
    
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
) : A2AMessage()

/**
 * 查询响应消息，用于返回代理的状态
 */
@Serializable
@SerialName("query_response")
data class QueryResponse(
    override val id: String,
    
    /**
     * 查询结果
     */
    val result: JsonElement,
    
    /**
     * 响应元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "query_response"
) : A2AMessage()

/**
 * 错误消息，用于返回错误信息
 */
@Serializable
@SerialName("error")
data class ErrorMessage(
    override val id: String,
    
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
     * 响应元数据
     */
    val metadata: Map<String, String> = emptyMap(),
    
    override val type: String = "error"
) : A2AMessage()
