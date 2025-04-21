package ai.kastrax.rag.context

import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 上下文构建器，用于将检索结果转换为结构化的上下文。
 *
 * @property config 上下文构建器配置
 * @property tokenCounter 令牌计数器，默认为 SimpleTokenCounter
 */
class ContextBuilder(
    private val config: ContextBuilderConfig = ContextBuilderConfig(),
    private val tokenCounter: TokenCounter = SimpleTokenCounter()
) {
    /**
     * 构建上下文。
     *
     * @param results 检索结果
     * @param query 查询文本
     * @return 构建的上下文
     */
    fun buildContext(results: List<SearchResult>, query: String): String {
        if (results.isEmpty()) {
            return ""
        }
        
        // 准备头部和尾部
        val header = config.headerTemplate.replace("{query}", query)
        val footer = config.footerTemplate.replace("{query}", query)
        
        // 计算头部和尾部的令牌数
        val headerTokens = tokenCounter.countTokens(header)
        val footerTokens = tokenCounter.countTokens(footer)
        val separatorTokens = tokenCounter.countTokens(config.separator)
        
        // 计算可用于文档内容的令牌数
        val availableTokens = config.maxTokens - headerTokens - footerTokens
        
        // 如果可用令牌数小于等于0，则返回空字符串
        if (availableTokens <= 0) {
            logger.warn { "Available tokens for documents is <= 0. Check your header and footer templates." }
            return ""
        }
        
        // 处理文档
        val processedDocs = if (config.compressionEnabled) {
            compressDocuments(results, availableTokens, separatorTokens)
        } else {
            truncateDocuments(results, availableTokens, separatorTokens)
        }
        
        // 构建最终上下文
        return buildString {
            append(header)
            
            processedDocs.forEachIndexed { index, doc ->
                if (index > 0) {
                    append(config.separator)
                }
                
                append(doc)
            }
            
            append(footer)
        }
    }
    
    /**
     * 截断文档，确保总令牌数不超过可用令牌数。
     *
     * @param results 检索结果
     * @param availableTokens 可用令牌数
     * @param separatorTokens 分隔符令牌数
     * @return 处理后的文档列表
     */
    private fun truncateDocuments(
        results: List<SearchResult>,
        availableTokens: Int,
        separatorTokens: Int
    ): List<String> {
        val processedDocs = mutableListOf<String>()
        var remainingTokens = availableTokens
        
        for (result in results) {
            // 准备文档内容
            val docContent = formatDocument(result)
            val docTokens = tokenCounter.countTokens(docContent)
            
            // 检查是否有足够的令牌
            if (processedDocs.isNotEmpty()) {
                remainingTokens -= separatorTokens
            }
            
            if (docTokens <= remainingTokens) {
                // 如果文档可以完全适应，则添加整个文档
                processedDocs.add(docContent)
                remainingTokens -= docTokens
            } else if (remainingTokens > 20) {
                // 如果剩余令牌数足够多，则截断文档
                val truncatedDoc = truncateText(docContent, remainingTokens)
                processedDocs.add(truncatedDoc)
                break
            } else {
                // 如果剩余令牌数太少，则停止添加文档
                break
            }
        }
        
        return processedDocs
    }
    
    /**
     * 压缩文档，通过减少每个文档的大小来包含更多文档。
     *
     * @param results 检索结果
     * @param availableTokens 可用令牌数
     * @param separatorTokens 分隔符令牌数
     * @return 处理后的文档列表
     */
    private fun compressDocuments(
        results: List<SearchResult>,
        availableTokens: Int,
        separatorTokens: Int
    ): List<String> {
        // 计算每个文档的平均令牌数
        val totalSeparatorTokens = (results.size - 1) * separatorTokens
        val remainingTokens = availableTokens - totalSeparatorTokens
        
        if (remainingTokens <= 0 || results.isEmpty()) {
            return emptyList()
        }
        
        // 计算每个文档的目标令牌数
        val targetTokensPerDoc = (remainingTokens.toDouble() * config.compressionRatio / results.size).toInt()
        
        if (targetTokensPerDoc <= 10) {
            // 如果目标令牌数太小，则回退到截断方法
            return truncateDocuments(results, availableTokens, separatorTokens)
        }
        
        // 压缩每个文档
        val processedDocs = mutableListOf<String>()
        var usedTokens = 0
        
        for (result in results) {
            // 准备文档内容
            val docContent = formatDocument(result)
            val docTokens = tokenCounter.countTokens(docContent)
            
            // 添加分隔符令牌数（除了第一个文档）
            if (processedDocs.isNotEmpty()) {
                usedTokens += separatorTokens
            }
            
            if (docTokens <= targetTokensPerDoc) {
                // 如果文档小于目标令牌数，则添加整个文档
                processedDocs.add(docContent)
                usedTokens += docTokens
            } else {
                // 否则，压缩文档
                val compressedDoc = truncateText(docContent, targetTokensPerDoc)
                processedDocs.add(compressedDoc)
                usedTokens += targetTokensPerDoc
            }
            
            // 检查是否超过可用令牌数
            if (usedTokens >= availableTokens) {
                break
            }
        }
        
        return processedDocs
    }
    
    /**
     * 截断文本，确保令牌数不超过指定值。
     *
     * @param text 文本
     * @param maxTokens 最大令牌数
     * @return 截断后的文本
     */
    private fun truncateText(text: String, maxTokens: Int): String {
        if (tokenCounter.countTokens(text) <= maxTokens) {
            return text
        }
        
        // 按句子分割文本
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        val truncatedText = StringBuilder()
        var currentTokens = 0
        
        for (sentence in sentences) {
            val sentenceTokens = tokenCounter.countTokens(sentence)
            
            if (currentTokens + sentenceTokens <= maxTokens) {
                if (truncatedText.isNotEmpty()) {
                    truncatedText.append(" ")
                }
                truncatedText.append(sentence)
                currentTokens += sentenceTokens
            } else {
                break
            }
        }
        
        return truncatedText.toString()
    }
    
    /**
     * 格式化文档，包括元数据（如果需要）。
     *
     * @param result 检索结果
     * @return 格式化后的文档
     */
    private fun formatDocument(result: SearchResult): String {
        val doc = result.document
        
        return if (config.includeMetadata && doc.metadata.isNotEmpty()) {
            val metadataStr = doc.metadata.entries.joinToString(", ") { "${it.key}:${it.value}" }
            "[Source: $metadataStr]\n${doc.content}"
        } else {
            doc.content
        }
    }
}
