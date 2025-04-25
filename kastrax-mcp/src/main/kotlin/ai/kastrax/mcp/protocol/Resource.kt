package ai.kastrax.mcp.protocol

import kotlinx.serialization.Serializable

/**
 * MCP 资源
 * 
 * 资源是 MCP 服务器提供的数据，可以是文本、图像、音频等。
 */
@Serializable
data class Resource(
    /**
     * 资源 ID
     */
    val id: String,
    
    /**
     * 资源名称
     */
    val name: String,
    
    /**
     * 资源描述
     */
    val description: String,
    
    /**
     * 资源类型
     */
    val type: ResourceType,
    
    /**
     * 资源元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 资源类型
 */
@Serializable
enum class ResourceType {
    /**
     * 文本资源
     */
    TEXT,
    
    /**
     * Markdown 资源
     */
    MARKDOWN,
    
    /**
     * HTML 资源
     */
    HTML,
    
    /**
     * JSON 资源
     */
    JSON,
    
    /**
     * 图像资源
     */
    IMAGE,
    
    /**
     * 音频资源
     */
    AUDIO,
    
    /**
     * 视频资源
     */
    VIDEO,
    
    /**
     * 二进制资源
     */
    BINARY,
    
    /**
     * 其他资源
     */
    OTHER
}

/**
 * 资源内容提供者
 */
interface ResourceContentProvider {
    /**
     * 获取资源内容
     * 
     * @param resourceId 资源 ID
     * @return 资源内容
     */
    suspend fun getResourceContent(resourceId: String): String
}

/**
 * 静态资源内容提供者
 */
class StaticResourceContentProvider(
    private val resources: Map<String, String>
) : ResourceContentProvider {
    override suspend fun getResourceContent(resourceId: String): String {
        return resources[resourceId] ?: throw ResourceNotFoundException(resourceId)
    }
}

/**
 * 动态资源内容提供者
 */
class DynamicResourceContentProvider(
    private val contentProvider: suspend (String) -> String
) : ResourceContentProvider {
    override suspend fun getResourceContent(resourceId: String): String {
        return try {
            contentProvider(resourceId)
        } catch (e: Exception) {
            throw ResourceNotFoundException(resourceId, e)
        }
    }
}

/**
 * 资源不存在异常
 */
class ResourceNotFoundException : Exception {
    constructor(resourceId: String) : super("Resource not found: $resourceId")
    constructor(resourceId: String, cause: Throwable) : super("Resource not found: $resourceId", cause)
}
