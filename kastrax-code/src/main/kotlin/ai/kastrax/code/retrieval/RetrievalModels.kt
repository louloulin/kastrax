package ai.kastrax.code.retrieval

import java.nio.file.Path

/**
 * 检索请求
 *
 * @property query 查询
 * @property maxResults 最大结果数
 * @property minScore 最小分数
 * @property filters 过滤器
 */
data class RetrievalRequest(
    val query: String,
    val maxResults: Int = 10,
    val minScore: Double = 0.0,
    val filters: Map<String, Any> = emptyMap()
)

/**
 * 检索结果
 *
 * @property id 结果ID
 * @property content 内容
 * @property score 分数
 * @property metadata 元数据
 */
data class RetrievalResult(
    val id: String,
    val content: String,
    val score: Double,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 获取文件路径
     *
     * @return 文件路径
     */
    fun getFilePath(): Path? {
        val pathStr = metadata["filePath"] as? String
        return if (pathStr != null) Path.of(pathStr) else null
    }
    
    /**
     * 获取类型
     *
     * @return 类型
     */
    fun getType(): String {
        return metadata["type"] as? String ?: "unknown"
    }
    
    /**
     * 获取名称
     *
     * @return 名称
     */
    fun getName(): String {
        return metadata["name"] as? String ?: "unknown"
    }
}
