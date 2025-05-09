package ai.kastrax.rag.document

import ai.kastrax.store.document.Document
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * 语义文档分割器，基于语义相似度分割文档。
 *
 * @property embeddingService 嵌入服务
 * @property chunkSize 块大小
 * @property chunkOverlap 块重叠大小
 * @property similarityThreshold 相似度阈值
 */
class SemanticDocumentSplitter(
    private val embeddingService: EmbeddingService,
    private val chunkSize: Int = 1000,
    private val chunkOverlap: Int = 200,
    private val similarityThreshold: Double = 0.7
) : DocumentSplitter {
    /**
     * 分割文档。
     *
     * @param document 文档
     * @return 分割后的文档列表
     */
    override fun split(document: Document): List<Document> {
        val content = document.content
        
        // 如果内容小于块大小，直接返回原始文档
        if (content.length <= chunkSize) {
            return listOf(document)
        }
        
        // 按段落分割文本
        val paragraphs = content.split("\n\n")
        
        // 如果只有一个段落，使用基本的分割方法
        if (paragraphs.size <= 1) {
            return splitBySize(document)
        }
        
        // 使用语义分割
        return splitBySemantic(document, paragraphs)
    }
    
    /**
     * 按大小分割文档。
     *
     * @param document 文档
     * @return 分割后的文档列表
     */
    private fun splitBySize(document: Document): List<Document> {
        val content = document.content
        val chunks = mutableListOf<String>()
        
        var i = 0
        while (i < content.length) {
            val end = min(i + chunkSize, content.length)
            chunks.add(content.substring(i, end))
            i += chunkSize - chunkOverlap
        }
        
        return chunks.mapIndexed { index, chunk ->
            Document(
                id = "${document.id}-${index}",
                content = chunk,
                metadata = document.metadata + mapOf(
                    "parent_id" to document.id,
                    "chunk_index" to index
                )
            )
        }
    }
    
    /**
     * 按语义分割文档。
     *
     * @param document 文档
     * @param paragraphs 段落列表
     * @return 分割后的文档列表
     */
    private fun splitBySemantic(document: Document, paragraphs: List<String>): List<Document> {
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()
        
        for (paragraph in paragraphs) {
            // 如果当前块加上新段落超过块大小，保存当前块并开始新块
            if (currentChunk.length + paragraph.length > chunkSize) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    
                    // 保留重叠部分
                    val lastPart = currentChunk.toString()
                    currentChunk = StringBuilder()
                    if (lastPart.length > chunkOverlap) {
                        val overlapText = lastPart.substring(lastPart.length - chunkOverlap)
                        currentChunk.append(overlapText)
                        currentChunk.append("\n\n")
                    }
                }
            }
            
            // 添加段落到当前块
            currentChunk.append(paragraph)
            currentChunk.append("\n\n")
        }
        
        // 添加最后一个块
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        
        // 创建新文档
        return chunks.mapIndexed { index, chunk ->
            Document(
                id = "${document.id}-${UUID.randomUUID()}",
                content = chunk,
                metadata = document.metadata + mapOf(
                    "parent_id" to document.id,
                    "chunk_index" to index
                )
            )
        }
    }
    
    /**
     * 计算两个文本的相似度。
     *
     * @param text1 文本 1
     * @param text2 文本 2
     * @return 相似度
     */
    private suspend fun calculateSimilarity(text1: String, text2: String): Double = withContext(Dispatchers.IO) {
        try {
            val embedding1 = embeddingService.embed(text1)
            val embedding2 = embeddingService.embed(text2)
            
            // 计算余弦相似度
            var dotProduct = 0.0
            var norm1 = 0.0
            var norm2 = 0.0
            
            for (i in embedding1.indices) {
                dotProduct += embedding1[i] * embedding2[i]
                norm1 += embedding1[i] * embedding1[i]
                norm2 += embedding2[i] * embedding2[i]
            }
            
            return@withContext dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2))
        } catch (e: Exception) {
            logger.error(e) { "Error calculating similarity" }
            return@withContext 0.0
        }
    }
}
