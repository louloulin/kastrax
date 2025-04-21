package ai.kastrax.rag.util

import kotlin.math.sqrt

/**
 * 计算两个向量的余弦相似度。
 *
 * @param other 另一个向量
 * @return 余弦相似度，范围为 [-1, 1]
 */
fun FloatArray.cosineSimilarity(other: FloatArray): Double {
    require(this.size == other.size) { "Vectors must have the same dimension" }

    var dotProduct = 0.0
    var normA = 0.0
    var normB = 0.0

    for (i in this.indices) {
        dotProduct += this[i] * other[i]
        normA += this[i] * this[i]
        normB += other[i] * other[i]
    }

    if (normA <= 0.0 || normB <= 0.0) {
        return 0.0
    }

    return dotProduct / (sqrt(normA) * sqrt(normB))
}

/**
 * 计算列表中的最大值，如果列表为空则返回 0.0。
 *
 * @return 最大值，如果列表为空则返回 0.0
 */
fun List<Double>.maxOrDefault(): Double {
    return if (this.isEmpty()) 0.0 else this.maxOrNull() ?: 0.0
}
