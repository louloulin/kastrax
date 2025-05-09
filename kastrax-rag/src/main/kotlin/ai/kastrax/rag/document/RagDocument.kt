package ai.kastrax.rag.document

/**
 * RAG 文档。
 * 表示一个可以用于检索增强生成的文档。
 *
 * @property id 文档 ID
 * @property content 文档内容
 * @property metadata 文档元数据
 * @property embedding 文档嵌入向量
 */
data class RagDocument(
    val id: String,
    val content: String,
    val metadata: Map<String, Any> = emptyMap(),
    val embedding: FloatArray? = null
) {
    /**
     * 获取文档的分数。
     * 如果元数据中包含 score 字段，则返回该字段的值，否则返回 0.0。
     *
     * @return 文档分数
     */
    val score: Double
        get() = metadata["score"] as? Double ?: 0.0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RagDocument

        if (id != other.id) return false
        if (content != other.content) return false
        if (metadata != other.metadata) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}
