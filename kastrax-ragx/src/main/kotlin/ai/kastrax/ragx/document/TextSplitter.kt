package ai.kastrax.ragx.document

import ai.kastrax.store.document.Document
import java.util.UUID

/**
 * 文本分割器，基于分隔符分割文档。
 *
 * @property separator 分隔符
 * @property chunkSize 块大小
 * @property chunkOverlap 块重叠大小
 */
class TextSplitter(
    private val separator: String = "\n\n",
    private val chunkSize: Int = 1000,
    private val chunkOverlap: Int = 200
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
        
        // 按分隔符分割文本
        val chunks = mutableListOf<String>()
        val segments = content.split(separator)
        
        var currentChunk = StringBuilder()
        
        for (segment in segments) {
            // 如果当前块加上新段落超过块大小，保存当前块并开始新块
            if (currentChunk.length + segment.length > chunkSize) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    
                    // 保留重叠部分
                    val lastPart = currentChunk.toString()
                    currentChunk = StringBuilder()
                    if (lastPart.length > chunkOverlap) {
                        currentChunk.append(lastPart.substring(lastPart.length - chunkOverlap))
                        currentChunk.append(separator)
                    }
                }
            }
            
            // 添加段落到当前块
            currentChunk.append(segment)
            currentChunk.append(separator)
        }
        
        // 添加最后一个块
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        
        // 创建新文档
        return chunks.map { chunk ->
            Document(
                id = "${document.id}-${UUID.randomUUID()}",
                content = chunk,
                metadata = document.metadata + ("parent_id" to document.id)
            )
        }
    }
}
