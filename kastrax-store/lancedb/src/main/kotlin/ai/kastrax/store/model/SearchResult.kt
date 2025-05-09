package ai.kastrax.store.model

/**
 * 搜索结果。
 *
 * @property id 向量 ID
 * @property score 相似度分数
 * @property metadata 元数据
 * @property vector 向量
 */
data class SearchResult(
    val id: String,
    val score: Double,
    val metadata: Map<String, Any>,
    val vector: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchResult

        if (id != other.id) return false
        if (score != other.score) return false
        if (metadata != other.metadata) return false
        if (vector != null) {
            if (other.vector == null) return false
            if (!vector.contentEquals(other.vector)) return false
        } else if (other.vector != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + score.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + (vector?.contentHashCode() ?: 0)
        return result
    }
}
