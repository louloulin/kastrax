package ai.kastrax.ragx.context

import ai.kastrax.ragx.ContextFormat
import ai.kastrax.ragx.RagXOptions
import ai.kastrax.store.document.DocumentSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 上下文构建器，用于构建上下文。
 */
class ContextBuilder {
    private val json = Json { prettyPrint = true }
    
    /**
     * 构建上下文。
     *
     * @param query 查询文本
     * @param results 检索结果列表
     * @param options 选项
     * @return 构建的上下文
     */
    suspend fun buildContext(
        query: String,
        results: List<DocumentSearchResult>,
        options: RagXOptions
    ): String = withContext(Dispatchers.IO) {
        if (results.isEmpty()) {
            return@withContext ""
        }
        
        // 根据格式构建上下文
        val context = when (options.contextFormat) {
            ContextFormat.TEXT -> buildTextContext(results, options)
            ContextFormat.MARKDOWN -> buildMarkdownContext(results, options)
            ContextFormat.JSON -> buildJsonContext(results, options)
        }
        
        // 限制上下文长度
        return@withContext if (context.length > options.maxContextLength) {
            context.substring(0, options.maxContextLength)
        } else {
            context
        }
    }
    
    /**
     * 构建文本格式的上下文。
     *
     * @param results 检索结果列表
     * @param options 选项
     * @return 构建的上下文
     */
    private fun buildTextContext(
        results: List<DocumentSearchResult>,
        options: RagXOptions
    ): String {
        val contextBuilder = StringBuilder()
        
        results.forEachIndexed { index, result ->
            // 添加文档内容
            contextBuilder.append(result.document.content)
            
            // 添加元数据（如果需要）
            if (options.includeMetadata) {
                val metadata = if (options.metadataFields.isEmpty()) {
                    result.document.metadata
                } else {
                    result.document.metadata.filterKeys { key -> key in options.metadataFields }
                }
                
                if (metadata.isNotEmpty()) {
                    contextBuilder.append("\nMetadata: ")
                    metadata.forEach { (key, value) ->
                        contextBuilder.append("$key: $value, ")
                    }
                    // 移除最后的逗号和空格
                    contextBuilder.setLength(contextBuilder.length - 2)
                }
            }
            
            // 添加分隔符（除了最后一个文档）
            if (index < results.size - 1) {
                contextBuilder.append(options.separator)
            }
        }
        
        return contextBuilder.toString()
    }
    
    /**
     * 构建 Markdown 格式的上下文。
     *
     * @param results 检索结果列表
     * @param options 选项
     * @return 构建的上下文
     */
    private fun buildMarkdownContext(
        results: List<DocumentSearchResult>,
        options: RagXOptions
    ): String {
        val contextBuilder = StringBuilder()
        
        results.forEachIndexed { index, result ->
            // 添加文档标题
            contextBuilder.append("## Document ${index + 1}\n\n")
            
            // 添加文档内容
            contextBuilder.append(result.document.content)
            
            // 添加元数据（如果需要）
            if (options.includeMetadata) {
                val metadata = if (options.metadataFields.isEmpty()) {
                    result.document.metadata
                } else {
                    result.document.metadata.filterKeys { key -> key in options.metadataFields }
                }
                
                if (metadata.isNotEmpty()) {
                    contextBuilder.append("\n\n### Metadata\n\n")
                    metadata.forEach { (key, value) ->
                        contextBuilder.append("- **$key**: $value\n")
                    }
                }
            }
            
            // 添加分隔符（除了最后一个文档）
            if (index < results.size - 1) {
                contextBuilder.append(options.separator)
            }
        }
        
        return contextBuilder.toString()
    }
    
    /**
     * 构建 JSON 格式的上下文。
     *
     * @param results 检索结果列表
     * @param options 选项
     * @return 构建的上下文
     */
    private fun buildJsonContext(
        results: List<DocumentSearchResult>,
        options: RagXOptions
    ): String {
        val documents = results.map { result ->
            val metadata = if (options.includeMetadata) {
                if (options.metadataFields.isEmpty()) {
                    result.document.metadata
                } else {
                    result.document.metadata.filterKeys { key -> key in options.metadataFields }
                }
            } else {
                emptyMap()
            }
            
            mapOf(
                "id" to result.document.id,
                "content" to result.document.content,
                "metadata" to metadata,
                "score" to result.score
            )
        }
        
        val contextMap = mapOf("documents" to documents)
        return json.encodeToString(contextMap)
    }
}
