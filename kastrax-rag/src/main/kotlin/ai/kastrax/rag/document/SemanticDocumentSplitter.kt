package ai.kastrax.rag.document

import ai.kastrax.rag.embedding.Embedding
import ai.kastrax.rag.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

/**
 * 基于语义相似性的文档分割器，将语义相似的文本片段组合在一起。
 *
 * 这个分割器首先使用初始分隔符将文档分割成小片段，然后基于语义相似性将相似的片段合并成更大的块。
 * 它使用嵌入服务计算文本片段的嵌入向量，并使用余弦相似度度量它们之间的语义相似性。
 *
 * @property embeddingService 嵌入服务，用于计算文本的嵌入向量
 * @property chunkSize 每个块的目标大小（字符数）
 * @property chunkOverlap 相邻块之间的重叠字符数
 * @property similarityThreshold 相似性阈值，用于确定是否合并片段，范围为 [0, 1]
 * @property initialSeparators 初始分隔符列表，用于初始分割
 * @property addMetadata 是否添加分割相关的元数据
 */
class SemanticDocumentSplitter(
    private val embeddingService: EmbeddingService,
    private val chunkSize: Int = 1000,
    private val chunkOverlap: Int = 200,
    private val similarityThreshold: Double = 0.7,
    private val initialSeparators: List<String> = listOf("\n\n", "\n", ". ", "! ", "? "),
    private val addMetadata: Boolean = true
) : DocumentSplitter {
    
    init {
        require(chunkSize > 0) { "Chunk size must be positive" }
        require(chunkOverlap >= 0) { "Chunk overlap must be non-negative" }
        require(chunkOverlap < chunkSize) { "Chunk overlap must be less than chunk size" }
        require(similarityThreshold in 0.0..1.0) { "Similarity threshold must be between 0 and 1" }
        require(initialSeparators.isNotEmpty()) { "Initial separators list must not be empty" }
    }
    
    override fun split(document: Document): List<Document> {
        val text = document.content
        
        // 如果文本小于块大小，直接返回
        if (text.length <= chunkSize) {
            return listOf(document)
        }
        
        try {
            // 使用初始分隔符进行初始分割
            val initialSegments = splitInitial(text)
            
            // 计算每个片段的嵌入向量
            val segmentEmbeddings = runBlocking {
                initialSegments.map { segment ->
                    segment to embeddingService.embed(segment)
                }
            }
            
            // 基于语义相似性合并片段
            val mergedSegments = mergeSegmentsBySimilarity(segmentEmbeddings)
            
            // 处理合并后的片段，确保不超过最大块大小
            val finalChunks = processMergedSegments(mergedSegments)
            
            // 创建文档列表
            return finalChunks.mapIndexed { index, chunk ->
                val metadata = if (addMetadata) {
                    document.metadata.toMutableMap().apply {
                        put("chunk_index", index)
                        put("chunk_total", finalChunks.size)
                        put("original_document", document.metadata["source"] ?: "unknown")
                        put("chunk_type", "semantic")
                    }
                } else {
                    document.metadata
                }
                
                Document(chunk, metadata)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in semantic splitting, falling back to recursive character splitting" }
            
            // 如果语义分割失败，回退到递归字符分割
            val fallbackSplitter = RecursiveCharacterTextSplitter(
                chunkSize = chunkSize,
                chunkOverlap = chunkOverlap,
                separators = initialSeparators,
                addMetadata = addMetadata
            )
            
            return fallbackSplitter.split(document)
        }
    }
    
    /**
     * 使用初始分隔符进行初始分割。
     *
     * @param text 要分割的文本
     * @return 初始分割后的片段列表
     */
    private fun splitInitial(text: String): List<String> {
        var segments = listOf(text)
        
        // 使用每个分隔符进行分割
        for (separator in initialSeparators) {
            val newSegments = mutableListOf<String>()
            
            for (segment in segments) {
                if (segment.length <= chunkSize / 2) {
                    // 如果片段已经足够小，保留它
                    newSegments.add(segment)
                } else {
                    // 否则，使用当前分隔符进一步分割
                    val subSegments = segment.split(separator)
                    if (subSegments.size > 1) {
                        newSegments.addAll(subSegments.filter { it.isNotBlank() })
                    } else {
                        newSegments.add(segment)
                    }
                }
            }
            
            segments = newSegments
            
            // 如果所有片段都已经足够小，停止分割
            if (segments.all { it.length <= chunkSize / 2 }) {
                break
            }
        }
        
        // 过滤掉空白片段并返回
        return segments.filter { it.isNotBlank() }
    }
    
    /**
     * 基于语义相似性合并片段。
     *
     * @param segmentEmbeddings 片段及其嵌入向量的列表
     * @return 合并后的片段列表
     */
    private fun mergeSegmentsBySimilarity(segmentEmbeddings: List<Pair<String, Embedding>>): List<String> {
        if (segmentEmbeddings.isEmpty()) {
            return emptyList()
        }
        
        if (segmentEmbeddings.size == 1) {
            return listOf(segmentEmbeddings.first().first)
        }
        
        val mergedSegments = mutableListOf<String>()
        var currentSegment = segmentEmbeddings.first().first
        var currentEmbedding = segmentEmbeddings.first().second
        
        for (i in 1 until segmentEmbeddings.size) {
            val (nextSegment, nextEmbedding) = segmentEmbeddings[i]
            
            // 计算当前片段和下一个片段之间的相似度
            val similarity = currentEmbedding.cosineSimilarity(nextEmbedding)
            
            // 如果相似度高于阈值，并且合并后的长度不超过块大小，则合并片段
            if (similarity >= similarityThreshold && currentSegment.length + nextSegment.length <= chunkSize) {
                currentSegment += " " + nextSegment
                
                // 重新计算合并后片段的嵌入向量
                currentEmbedding = runBlocking {
                    embeddingService.embed(currentSegment)
                }
            } else {
                // 否则，保存当前片段并开始新的片段
                mergedSegments.add(currentSegment)
                currentSegment = nextSegment
                currentEmbedding = nextEmbedding
            }
        }
        
        // 添加最后一个片段
        mergedSegments.add(currentSegment)
        
        return mergedSegments
    }
    
    /**
     * 处理合并后的片段，确保不超过最大块大小。
     *
     * @param mergedSegments 合并后的片段列表
     * @return 最终的块列表
     */
    private fun processMergedSegments(mergedSegments: List<String>): List<String> {
        val finalChunks = mutableListOf<String>()
        
        for (segment in mergedSegments) {
            if (segment.length <= chunkSize) {
                finalChunks.add(segment)
            } else {
                // 如果片段仍然太长，使用字符分割器
                val charSplitter = CharacterTextSplitter(
                    chunkSize = chunkSize,
                    chunkOverlap = chunkOverlap,
                    addMetadata = false
                )
                val subChunks = charSplitter.split(Document(segment))
                finalChunks.addAll(subChunks.map { it.content })
            }
        }
        
        return finalChunks
    }
}
