package ai.kastrax.a2x.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * EntityCard 是描述实体能力的 JSON 文件
 * 它包含实体的基本信息、能力列表和交互需求
 */
@Serializable
data class EntityCard(
    /**
     * 实体的唯一标识符
     */
    val id: String,
    
    /**
     * 实体的名称
     */
    val name: String,
    
    /**
     * 实体的描述
     */
    val description: String,
    
    /**
     * 实体的版本
     */
    val version: String,
    
    /**
     * 实体的类型
     */
    val type: EntityType,
    
    /**
     * 实体的 API 端点
     */
    val endpoint: String,
    
    /**
     * 实体的能力列表
     */
    val capabilities: List<Capability>,
    
    /**
     * 实体的认证需求
     */
    val authentication: Authentication,
    
    /**
     * 实体的元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 实体类型
 */
@Serializable
enum class EntityType {
    /**
     * 代理
     */
    @SerialName("agent")
    AGENT,
    
    /**
     * 系统
     */
    @SerialName("system")
    SYSTEM,
    
    /**
     * 环境
     */
    @SerialName("environment")
    ENVIRONMENT,
    
    /**
     * 人类
     */
    @SerialName("human")
    HUMAN,
    
    /**
     * 网络
     */
    @SerialName("network")
    NETWORK,
    
    /**
     * 其他
     */
    @SerialName("other")
    OTHER
}

/**
 * 能力
 */
@Serializable
data class Capability(
    /**
     * 能力的唯一标识符
     */
    val id: String,
    
    /**
     * 能力的名称
     */
    val name: String,
    
    /**
     * 能力的描述
     */
    val description: String,
    
    /**
     * 能力的版本
     */
    val version: String = "1.0.0",
    
    /**
     * 能力的参数列表
     */
    val parameters: List<Parameter> = emptyList(),
    
    /**
     * 能力的返回类型
     */
    @SerialName("return_type")
    val returnType: String = "json",
    
    /**
     * 能力的元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 参数
 */
@Serializable
data class Parameter(
    /**
     * 参数的名称
     */
    val name: String,
    
    /**
     * 参数的类型
     */
    val type: String,
    
    /**
     * 参数的描述
     */
    val description: String,
    
    /**
     * 参数是否必需
     */
    val required: Boolean = false,
    
    /**
     * 参数的默认值
     */
    @SerialName("default_value")
    val defaultValue: String? = null,
    
    /**
     * 参数的元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 认证
 */
@Serializable
data class Authentication(
    /**
     * 认证类型
     */
    val type: AuthenticationType,
    
    /**
     * 认证元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 认证类型
 */
@Serializable
enum class AuthenticationType {
    /**
     * 无认证
     */
    @SerialName("none")
    NONE,
    
    /**
     * API 密钥
     */
    @SerialName("api_key")
    API_KEY,
    
    /**
     * OAuth2
     */
    @SerialName("oauth2")
    OAUTH2,
    
    /**
     * JWT
     */
    @SerialName("jwt")
    JWT,
    
    /**
     * 基本认证
     */
    @SerialName("basic")
    BASIC,
    
    /**
     * 其他
     */
    @SerialName("other")
    OTHER
}
