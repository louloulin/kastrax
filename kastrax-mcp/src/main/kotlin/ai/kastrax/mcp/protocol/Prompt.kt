package ai.kastrax.mcp.protocol

import kotlinx.serialization.Serializable

/**
 * MCP 提示
 * 
 * 提示是 MCP 服务器提供的模板，可以被客户端使用。
 */
@Serializable
data class Prompt(
    /**
     * 提示 ID
     */
    val id: String,
    
    /**
     * 提示名称
     */
    val name: String,
    
    /**
     * 提示描述
     */
    val description: String,
    
    /**
     * 提示参数定义
     */
    val parameters: PromptParameters? = null,
    
    /**
     * 提示元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 提示参数定义
 */
@Serializable
data class PromptParameters(
    /**
     * 参数类型
     */
    val type: String = "object",
    
    /**
     * 必需的参数
     */
    val required: List<String> = emptyList(),
    
    /**
     * 参数属性
     */
    val properties: Map<String, PromptParameterProperty> = emptyMap()
)

/**
 * 提示参数属性
 */
@Serializable
data class PromptParameterProperty(
    /**
     * 参数类型
     */
    val type: String,
    
    /**
     * 参数描述
     */
    val description: String,
    
    /**
     * 参数默认值
     */
    val default: String? = null
)

/**
 * 提示内容提供者
 */
interface PromptContentProvider {
    /**
     * 获取提示内容
     * 
     * @param promptId 提示 ID
     * @return 提示内容
     */
    suspend fun getPromptContent(promptId: String): String
    
    /**
     * 渲染提示
     * 
     * @param promptId 提示 ID
     * @param parameters 提示参数
     * @return 渲染后的提示内容
     */
    suspend fun renderPrompt(promptId: String, parameters: Map<String, String>): String
}

/**
 * 静态提示内容提供者
 */
class StaticPromptContentProvider(
    private val prompts: Map<String, String>
) : PromptContentProvider {
    override suspend fun getPromptContent(promptId: String): String {
        return prompts[promptId] ?: throw PromptNotFoundException(promptId)
    }
    
    override suspend fun renderPrompt(promptId: String, parameters: Map<String, String>): String {
        val template = getPromptContent(promptId)
        return renderTemplate(template, parameters)
    }
    
    private fun renderTemplate(template: String, parameters: Map<String, String>): String {
        var result = template
        for ((key, value) in parameters) {
            result = result.replace("{{$key}}", value)
        }
        return result
    }
}

/**
 * 动态提示内容提供者
 */
class DynamicPromptContentProvider(
    private val contentProvider: suspend (String) -> String,
    private val renderer: suspend (String, Map<String, String>) -> String
) : PromptContentProvider {
    override suspend fun getPromptContent(promptId: String): String {
        return try {
            contentProvider(promptId)
        } catch (e: Exception) {
            throw PromptNotFoundException(promptId, e)
        }
    }
    
    override suspend fun renderPrompt(promptId: String, parameters: Map<String, String>): String {
        val template = getPromptContent(promptId)
        return try {
            renderer(template, parameters)
        } catch (e: Exception) {
            throw PromptRenderException(promptId, e)
        }
    }
}

/**
 * 提示不存在异常
 */
class PromptNotFoundException : Exception {
    constructor(promptId: String) : super("Prompt not found: $promptId")
    constructor(promptId: String, cause: Throwable) : super("Prompt not found: $promptId", cause)
}

/**
 * 提示渲染异常
 */
class PromptRenderException : Exception {
    constructor(promptId: String, message: String) : super("Prompt rendering failed: $promptId - $message")
    constructor(promptId: String, cause: Throwable) : super("Prompt rendering failed: $promptId - ${cause.message}", cause)
}
