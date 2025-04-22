package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.VectorStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.sqrt

/**
 * 内存中的向量存储实现。
 */
class InMemoryVectorStorage : VectorStorage, KastraXBase(component = "VECTOR_STORAGE", name = "in-memory") {
    private val mutex = Mutex()
    private val vectors = mutableMapOf<String, Pair<List<Float>, Map<String, String>>>()
    
    override suspend fun saveVector(id: String, vector: List<Float>, metadata: Map<String, String>): Boolean {
        return mutex.withLock {
            vectors[id] = vector to metadata
            true
        }
    }
    
    override suspend fun saveVectors(vectors: List<Triple<String, List<Float>, Map<String, String>>>): Int {
        return mutex.withLock {
            var count = 0
            for ((id, vector, metadata) in vectors) {
                this.vectors[id] = vector to metadata
                count++
            }
            count
        }
    }
    
    override suspend fun searchVectors(
        vector: List<Float>,
        limit: Int,
        minScore: Float,
        filter: Map<String, String>
    ): List<Triple<String, Float, Map<String, String>>> {
        return mutex.withLock {
            // 过滤向量
            val filteredVectors = vectors.filter { (_, pair) ->
                val metadata = pair.second
                filter.all { (key, value) -> metadata[key] == value }
            }
            
            // 计算相似度并排序
            val results = filteredVectors.map { (id, pair) ->
                val storedVector = pair.first
                val metadata = pair.second
                val similarity = cosineSimilarity(vector, storedVector)
                Triple(id, similarity, metadata)
            }.filter { (_, similarity, _) ->
                similarity >= minScore
            }.sortedByDescending { (_, similarity, _) ->
                similarity
            }.take(limit)
            
            results
        }
    }
    
    override suspend fun deleteVector(id: String): Boolean {
        return mutex.withLock {
            vectors.remove(id) != null
        }
    }
    
    override suspend fun deleteVectors(ids: List<String>): Int {
        return mutex.withLock {
            var count = 0
            for (id in ids) {
                if (vectors.remove(id) != null) {
                    count++
                }
            }
            count
        }
    }
    
    override suspend fun deleteVectorsByFilter(filter: Map<String, String>): Int {
        return mutex.withLock {
            val idsToDelete = vectors.filter { (_, pair) ->
                val metadata = pair.second
                filter.all { (key, value) -> metadata[key] == value }
            }.keys.toList()
            
            deleteVectors(idsToDelete)
        }
    }
    
    /**
     * 计算两个向量的余弦相似度。
     */
    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.size != b.size) {
            throw IllegalArgumentException("向量维度不匹配")
        }
        
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        if (normA == 0f || normB == 0f) {
            return 0f
        }
        
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
}
