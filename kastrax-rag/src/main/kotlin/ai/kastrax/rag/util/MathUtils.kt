package ai.kastrax.rag.util

import kotlin.math.sqrt

/**
 * 计算两个向量的余弦相似度。
 *
 * @param vec1 第一个向量
 * @param vec2 第二个向量
 * @return 余弦相似度，范围为 [-1, 1]
 */
fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
    require(vec1.size == vec2.size) { "Vectors must have the same dimension" }
    
    var dotProduct = 0.0
    var normA = 0.0
    var normB = 0.0
    
    for (i in vec1.indices) {
        dotProduct += vec1[i] * vec2[i]
        normA += vec1[i] * vec1[i]
        normB += vec2[i] * vec2[i]
    }
    
    if (normA <= 0.0 || normB <= 0.0) {
        return 0.0
    }
    
    return dotProduct / (sqrt(normA) * sqrt(normB))
}
