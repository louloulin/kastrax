package ai.kastrax.evals.util

import kotlin.math.sqrt

/**
 * 计算两个向量的余弦相似度。
 *
 * @param a 第一个向量
 * @param b 第二个向量
 * @return 余弦相似度，范围为 -1.0 到 1.0
 */
fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    require(a.size == b.size) { "Vectors must have the same dimension" }
    
    var dotProduct = 0.0f
    var normA = 0.0f
    var normB = 0.0f
    
    for (i in a.indices) {
        dotProduct += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }
    
    if (normA <= 0.0f || normB <= 0.0f) {
        return 0.0f
    }
    
    return dotProduct / (sqrt(normA) * sqrt(normB))
}
