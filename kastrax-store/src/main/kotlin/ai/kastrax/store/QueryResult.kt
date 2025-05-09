package ai.kastrax.store

/**
 * 查询结果类，表示一个向量查询的结果。
 *
 * @property id 向量 ID
 * @property score 相似度分数
 * @property metadata 元数据
 * @property vector 向量数据
 */
data class QueryResult(
    val id: String,
    val score: Double,
    val metadata: Map<String, Any>? = null,
    val vector: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueryResult

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
        result = 31 * result + (metadata?.hashCode() ?: 0)
        result = 31 * result + (vector?.contentHashCode() ?: 0)
        return result
    }
}
