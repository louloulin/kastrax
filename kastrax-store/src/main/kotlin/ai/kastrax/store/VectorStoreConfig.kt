package ai.kastrax.store

import kotlinx.serialization.Serializable

/**
 * 向量存储配置
 *
 * @property type 向量存储类型
 * @property dimension 向量维度
 * @property metric 相似度度量方式
 * @property indexName 索引名称
 * @property options 其他配置选项
 */
@Serializable
data class VectorStoreConfig(
    val type: String = "memory",
    val dimension: Int = 1536,
    val metric: SimilarityMetric = SimilarityMetric.COSINE,
    val indexName: String = "default",
    val options: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * 创建内存向量存储配置
         *
         * @param dimension 向量维度
         * @param metric 相似度度量方式
         * @param indexName 索引名称
         * @return 内存向量存储配置
         */
        fun memory(
            dimension: Int = 1536,
            metric: SimilarityMetric = SimilarityMetric.COSINE,
            indexName: String = "default"
        ): VectorStoreConfig {
            return VectorStoreConfig(
                type = "memory",
                dimension = dimension,
                metric = metric,
                indexName = indexName
            )
        }

        /**
         * 创建 LanceDB 向量存储配置
         *
         * @param dimension 向量维度
         * @param metric 相似度度量方式
         * @param indexName 索引名称
         * @param uri LanceDB URI
         * @return LanceDB 向量存储配置
         */
        fun lancedb(
            dimension: Int = 1536,
            metric: SimilarityMetric = SimilarityMetric.COSINE,
            indexName: String = "default",
            uri: String = "memory://lancedb"
        ): VectorStoreConfig {
            return VectorStoreConfig(
                type = "lancedb",
                dimension = dimension,
                metric = metric,
                indexName = indexName,
                options = mapOf("uri" to uri)
            )
        }

        /**
         * 创建 Chroma 向量存储配置
         *
         * @param dimension 向量维度
         * @param metric 相似度度量方式
         * @param indexName 索引名称
         * @param host Chroma 主机
         * @param port Chroma 端口
         * @return Chroma 向量存储配置
         */
        fun chroma(
            dimension: Int = 1536,
            metric: SimilarityMetric = SimilarityMetric.COSINE,
            indexName: String = "default",
            host: String = "localhost",
            port: Int = 8000
        ): VectorStoreConfig {
            return VectorStoreConfig(
                type = "chroma",
                dimension = dimension,
                metric = metric,
                indexName = indexName,
                options = mapOf(
                    "host" to host,
                    "port" to port.toString()
                )
            )
        }
    }
}
