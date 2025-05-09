package ai.kastrax.rag.context

import ai.kastrax.rag.ContextBuilderConfig
import ai.kastrax.store.document.DocumentSearchResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 上下文构建器，用于从检索结果构建上下文。
 *
 * @property config 上下文构建器配置
 */
class ContextBuilder(
    private val config: ContextBuilderConfig = ContextBuilderConfig()
) {
    /**
     * 构建上下文。
     *
     * @param results 检索结果列表
     * @param query 查询文本
     * @return 构建的上下文
     */
    fun buildContext(
        results: List<DocumentSearchResult>,
        query: String
    ): String {
        logger.debug { "Building context for query: '$query' with ${results.size} results" }
        
        if (results.isEmpty()) {
            return ""
        }
        
        // 构建上下文
        val contextBuilder = StringBuilder()
        
        // 添加每个检索结果
        for (result in results) {
            // 添加文档内容
            contextBuilder.append(result.document.content)
            
            // 添加元数据（如果需要）
            if (config.includeMetadata) {
                val source = result.document.metadata["source"] as? String ?: ""
                if (source.isNotEmpty()) {
                    val metadata = config.metadataTemplate.replace("{source}", source)
                    contextBuilder.append("\n").append(metadata)
                }
            }
            
            // 添加分隔符
            contextBuilder.append(config.separator)
        }
        
        // 截断上下文（如果需要）
        val context = contextBuilder.toString()
        return if (config.maxTokens > 0) {
            // 这里我们使用一个简单的启发式方法来估计令牌数
            // 在实际应用中，你可能需要使用更准确的方法
            val estimatedTokens = context.length / 4
            if (estimatedTokens > config.maxTokens) {
                // 截断上下文
                val truncatedLength = (config.maxTokens * 4).coerceAtMost(context.length)
                context.substring(0, truncatedLength)
            } else {
                context
            }
        } else {
            context
        }
    }
}
