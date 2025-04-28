package ai.kastrax.a2a.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Agent Card 是描述代理能力的 JSON 文件，通常位于 /.well-known/agent.json
 * 它包含代理的基本信息、能力列表和认证需求
 */
@Serializable
data class AgentCard(
    /**
     * 代理的唯一标识符
     */
    val id: String,
    
    /**
     * 代理的名称
     */
    val name: String,
    
    /**
     * 代理的描述
     */
    val description: String,
    
    /**
     * 代理的版本
     */
    val version: String,
    
    /**
     * 代理的 API 端点
     */
    val endpoint: String,
    
    /**
     * 代理的能力列表
     */
    val capabilities: List<Capability>,
    
    /**
     * 代理的认证需求
     */
    val authentication: Authentication,
    
    /**
     * 代理的元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 代理能力，描述代理可以执行的操作
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
     * 能力的参数列表
     */
    val parameters: List<Parameter>,
    
    /**
     * 能力的返回类型
     */
    @SerialName("return_type")
    val returnType: String,
    
    /**
     * 能力的示例列表
     */
    val examples: List<Example> = emptyList()
)

/**
 * 能力参数，描述能力的输入参数
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
     * 参数的 JSON Schema
     */
    val schema: JsonObject? = null
)

/**
 * 能力示例，描述能力的使用示例
 */
@Serializable
data class Example(
    /**
     * 示例输入
     */
    val input: Map<String, String>,
    
    /**
     * 示例输出
     */
    val output: Map<String, String>,
    
    /**
     * 示例描述
     */
    val description: String? = null
)

/**
 * 代理认证，描述代理的认证需求
 */
@Serializable
data class Authentication(
    /**
     * 认证类型
     */
    val type: AuthType,
    
    /**
     * 认证详情
     */
    val details: Map<String, String> = emptyMap()
)

/**
 * 认证类型
 */
@Serializable
enum class AuthType {
    @SerialName("none")
    NONE,
    
    @SerialName("api_key")
    API_KEY,
    
    @SerialName("oauth2")
    OAUTH2,
    
    @SerialName("jwt")
    JWT
}
