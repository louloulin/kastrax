package ai.kastrax.rag.embedding

import ai.kastrax.fastembed.AsyncTextEmbedding
import ai.kastrax.fastembed.EmbeddingModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 基于 FastEmbed 的嵌入服务实现。
 *
 * @property modelName 模型名称，默认为 "BAAI/bge-small-en-v1.5"
 * @property maxLength 最大文本长度，默认为 512
 * @property normalize 是否归一化嵌入向量，默认为 true
 */
class FastEmbeddingService(
    private val modelName: String = "BAAI/bge-small-en-v1.5",
    private val maxLength: Int = 512,
    private val normalize: Boolean = true
) : EmbeddingService {

    // 懒加载 AsyncTextEmbedding 实例
    private val asyncTextEmbedding by lazy {
        logger.info { "Initializing FastEmbed with model: $modelName" }
        AsyncTextEmbedding.create(
            model = EmbeddingModel.valueOf(modelName.uppercase().replace("-", "_")),
            showDownloadProgress = true
        )
    }

    // 嵌入向量的维度
    private val dimensions = 384 // 默认维度，实际会在第一次使用时更新

    init {
        logger.info { "Initializing FastEmbed with model: $modelName" }
    }

    /**
     * 计算文本的嵌入向量。
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    override suspend fun embed(text: String): FloatArray {
        logger.debug { "Embedding text of length: ${text.length}" }

        return try {
            // 使用 AsyncTextEmbedding 计算嵌入向量
            val embedding = asyncTextEmbedding.embed(text)
            embedding.vector
        } catch (e: Exception) {
            logger.error(e) { "Error embedding text" }
            throw e
        }
    }

    /**
     * 批量计算文本的嵌入向量。
     *
     * @param texts 输入文本列表
     * @return 嵌入向量列表
     */
    override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
        logger.debug { "Embedding batch of ${texts.size} texts" }

        return try {
            // 使用 AsyncTextEmbedding 批量计算嵌入向量
            val embeddings = asyncTextEmbedding.embed(texts)
            embeddings.map { it.vector }
        } catch (e: Exception) {
            logger.error(e) { "Error embedding batch of texts" }
            throw e
        }
    }

    /**
     * 获取嵌入向量的维度。
     *
     * @return 嵌入向量的维度
     */
    override fun dimension(): Int {
        return asyncTextEmbedding.dimension
    }

    /**
     * 关闭嵌入服务，释放资源。
     */
    override fun close() {
        try {
            asyncTextEmbedding.close()
            logger.info { "Closed FastEmbed embedding service" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing FastEmbed embedding service" }
        }
    }
}
