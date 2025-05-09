package ai.kastrax.rag.document

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 文档分割器接口，用于将文档分割成更小的块。
 */
interface DocumentSplitter {
    /**
     * 分割文档。
     *
     * @param document 要分割的文档
     * @return 分割后的文档列表
     */
    fun split(document: Document): List<Document>
}

/**
 * 基于字符的文档分割器，按照字符数量分割文档。
 *
 * @property chunkSize 每个块的最大字符数
 * @property chunkOverlap 相邻块之间的重叠字符数
 * @property addMetadata 是否添加分割相关的元数据
 */
class CharacterTextSplitter(
    private val chunkSize: Int = 1000,
    private val chunkOverlap: Int = 200,
    private val addMetadata: Boolean = true
) : DocumentSplitter {
    
    init {
        require(chunkSize > 0) { "Chunk size must be positive" }
        require(chunkOverlap >= 0) { "Chunk overlap must be non-negative" }
        require(chunkOverlap < chunkSize) { "Chunk overlap must be less than chunk size" }
    }
    
    override fun split(document: Document): List<Document> {
        val text = document.content
        
        if (text.length <= chunkSize) {
            return listOf(document)
        }
        
        val chunks = mutableListOf<String>()
        var start = 0
        
        while (start < text.length) {
            val end = minOf(start + chunkSize, text.length)
            chunks.add(text.substring(start, end))
            start += (chunkSize - chunkOverlap)
        }
        
        return chunks.mapIndexed { index, chunk ->
            val metadata = if (addMetadata) {
                document.metadata.toMutableMap().apply {
                    put("chunk_index", index)
                    put("chunk_total", chunks.size)
                    put("original_document", document.metadata["source"] ?: "unknown")
                }
            } else {
                document.metadata
            }
            
            Document(chunk, metadata)
        }
    }
}

/**
 * 基于段落的文档分割器，按照段落分割文档。
 *
 * @property maxParagraphLength 每个段落的最大长度
 * @property separators 段落分隔符列表
 * @property addMetadata 是否添加分割相关的元数据
 */
class ParagraphTextSplitter(
    private val maxParagraphLength: Int = 1000,
    private val separators: List<String> = listOf("\n\n", "\n", ". ", "! ", "? "),
    private val addMetadata: Boolean = true
) : DocumentSplitter {
    
    init {
        require(maxParagraphLength > 0) { "Max paragraph length must be positive" }
        require(separators.isNotEmpty()) { "Separators list must not be empty" }
    }
    
    override fun split(document: Document): List<Document> {
        var text = document.content
        val chunks = mutableListOf<String>()
        
        // 使用第一个分隔符进行初始分割
        var segments = text.split(separators[0])
        
        // 如果某些段落仍然太长，使用下一个分隔符继续分割
        for (i in 1 until separators.size) {
            val separator = separators[i]
            val newSegments = mutableListOf<String>()
            
            for (segment in segments) {
                if (segment.length <= maxParagraphLength) {
                    newSegments.add(segment)
                } else {
                    newSegments.addAll(segment.split(separator))
                }
            }
            
            segments = newSegments
        }
        
        // 处理仍然超过最大长度的段落
        for (segment in segments) {
            if (segment.length <= maxParagraphLength) {
                chunks.add(segment)
            } else {
                // 如果段落仍然太长，使用字符分割器
                val charSplitter = CharacterTextSplitter(
                    chunkSize = maxParagraphLength,
                    chunkOverlap = 0,
                    addMetadata = false
                )
                val subChunks = charSplitter.split(Document(segment))
                chunks.addAll(subChunks.map { it.content })
            }
        }
        
        return chunks.mapIndexed { index, chunk ->
            val metadata = if (addMetadata) {
                document.metadata.toMutableMap().apply {
                    put("chunk_index", index)
                    put("chunk_total", chunks.size)
                    put("original_document", document.metadata["source"] ?: "unknown")
                }
            } else {
                document.metadata
            }
            
            Document(chunk, metadata)
        }
    }
}

/**
 * 递归字符文本分割器，递归地使用多个分隔符分割文档。
 *
 * @property chunkSize 每个块的最大字符数
 * @property chunkOverlap 相邻块之间的重叠字符数
 * @property separators 分隔符列表，按优先级排序
 * @property addMetadata 是否添加分割相关的元数据
 */
class RecursiveCharacterTextSplitter(
    private val chunkSize: Int = 1000,
    private val chunkOverlap: Int = 200,
    private val separators: List<String> = listOf("\n\n", "\n", ". ", "! ", "? ", ", ", " ", ""),
    private val addMetadata: Boolean = true
) : DocumentSplitter {
    
    init {
        require(chunkSize > 0) { "Chunk size must be positive" }
        require(chunkOverlap >= 0) { "Chunk overlap must be non-negative" }
        require(chunkOverlap < chunkSize) { "Chunk overlap must be less than chunk size" }
        require(separators.isNotEmpty()) { "Separators list must not be empty" }
    }
    
    override fun split(document: Document): List<Document> {
        return splitText(document.content).mapIndexed { index, chunk ->
            val metadata = if (addMetadata) {
                document.metadata.toMutableMap().apply {
                    put("chunk_index", index)
                    put("chunk_total", splitText(document.content).size)
                    put("original_document", document.metadata["source"] ?: "unknown")
                }
            } else {
                document.metadata
            }
            
            Document(chunk, metadata)
        }
    }
    
    private fun splitText(text: String): List<String> {
        val finalChunks = mutableListOf<String>()
        
        // 如果文本小于块大小，直接返回
        if (text.length <= chunkSize) {
            return listOf(text)
        }
        
        // 尝试使用每个分隔符分割
        for (separator in separators) {
            if (separator.isEmpty()) {
                // 如果是空分隔符，使用字符分割
                return splitOnCharacters(text)
            }
            
            val splits = text.split(separator)
            
            // 如果只有一个分割（没有分隔符），继续尝试下一个分隔符
            if (splits.size == 1) {
                continue
            }
            
            // 重新组合分割，确保每个块不超过块大小
            val goodSplits = mutableListOf<String>()
            var currentSplit = StringBuilder()
            
            for (split in splits) {
                if (currentSplit.length + split.length + separator.length <= chunkSize) {
                    if (currentSplit.isNotEmpty()) {
                        currentSplit.append(separator)
                    }
                    currentSplit.append(split)
                } else {
                    if (currentSplit.isNotEmpty()) {
                        goodSplits.add(currentSplit.toString())
                    }
                    currentSplit = StringBuilder(split)
                }
            }
            
            if (currentSplit.isNotEmpty()) {
                goodSplits.add(currentSplit.toString())
            }
            
            // 递归处理每个好的分割
            for (split in goodSplits) {
                if (split.length <= chunkSize) {
                    finalChunks.add(split)
                } else {
                    // 递归分割
                    finalChunks.addAll(splitText(split))
                }
            }
            
            // 如果成功分割，返回结果
            if (finalChunks.isNotEmpty()) {
                return mergeSplits(finalChunks)
            }
        }
        
        // 如果所有分隔符都失败，使用字符分割
        return splitOnCharacters(text)
    }
    
    private fun splitOnCharacters(text: String): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0
        
        while (start < text.length) {
            val end = minOf(start + chunkSize, text.length)
            chunks.add(text.substring(start, end))
            start += (chunkSize - chunkOverlap)
        }
        
        return chunks
    }
    
    private fun mergeSplits(splits: List<String>): List<String> {
        val mergedSplits = mutableListOf<String>()
        var currentSplit = StringBuilder()
        
        for (split in splits) {
            if (currentSplit.length + split.length <= chunkSize) {
                if (currentSplit.isNotEmpty()) {
                    currentSplit.append(" ")
                }
                currentSplit.append(split)
            } else {
                if (currentSplit.isNotEmpty()) {
                    mergedSplits.add(currentSplit.toString())
                }
                currentSplit = StringBuilder(split)
            }
        }
        
        if (currentSplit.isNotEmpty()) {
            mergedSplits.add(currentSplit.toString())
        }
        
        return mergedSplits
    }
}
