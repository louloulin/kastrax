package ai.kastrax.rag.embedding

import ai.kastrax.fastembed.AsyncTextEmbedding
import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.TextEmbedding
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * FastEmbed Kotlin 嵌入服务，使用 FastEmbed Kotlin 库生成文本的嵌入向量。
 *
 * 这个实现使用 FastEmbed Kotlin 库，它是 fastembed-rs 的 Kotlin 绑定，
 * 提供了高性能的文本嵌入生成功能，无需外部 API 或 Python 环境。
 *
 * @property model 嵌入模型
 * @property cacheDir 模型缓存目录
 * @property showDownloadProgress 是否显示模型下载进度
 */
class FastEmbedKotlinEmbeddingService private constructor(
    private val model: EmbeddingModel,
    private val cacheDir: Path?,
    private val showDownloadProgress: Boolean,
    private val textEmbedding: TextEmbedding,
    private val asyncTextEmbedding: AsyncTextEmbedding
) : EmbeddingService() {

    /**
     * 嵌入向量的维度。
     */
    override val dimension: Int = textEmbedding.dimension

    init {
        logger.info { "Initializing FastEmbed Kotlin with model: $model" }
        logger.info { "FastEmbed Kotlin initialized with embedding dimensions: $dimension" }
    }

    /**
     * 为文本生成嵌入向量。
     *
     * @param text 要嵌入的文本
     * @return 嵌入向量
     */
    override suspend fun embed(text: String): FloatArray {
        return withContext(Dispatchers.IO) {
            try {
                val embedding = asyncTextEmbedding.embed(text)
                embedding.vector
            } catch (e: Exception) {
                logger.error(e) { "Error generating embedding for text" }
                // 返回零向量作为后备
                FloatArray(dimension) { 0f }
            }
        }
    }

    /**
     * 为多个文本生成嵌入向量。
     * 这个方法使用 FastEmbed 的批处理功能，比单独处理每个文本更高效。
     *
     * @param texts 要嵌入的文本列表
     * @return 嵌入向量列表
     */
    override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
        if (texts.isEmpty()) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            try {
                // 使用 FastEmbed 的并行嵌入功能
                val embeddings = asyncTextEmbedding.embedParallel(texts)
                embeddings.map { it.vector }
            } catch (e: Exception) {
                logger.error(e) { "Error generating batch embeddings" }
                // 返回零向量作为后备
                texts.map { FloatArray(dimension) { 0f } }
            }
        }
    }



    /**
     * 关闭服务，释放资源。
     */
    override fun close() {
        try {
            asyncTextEmbedding.close()
            textEmbedding.close()
            logger.info { "FastEmbed Kotlin service closed" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing FastEmbedKotlinEmbeddingService" }
        }
    }

    companion object {
        /**
         * 创建 FastEmbedKotlinEmbeddingService 实例。
         *
         * @param model 嵌入模型，默认为 BGE_SMALL_ZH（中文小型模型）
         * @param cacheDir 模型缓存目录，默认为 null（使用系统默认目录）
         * @param showDownloadProgress 是否显示模型下载进度，默认为 false
         * @return FastEmbedKotlinEmbeddingService 实例
         */
        fun create(
            model: EmbeddingModel = EmbeddingModel.BGE_SMALL_ZH,
            cacheDir: Path? = null,
            showDownloadProgress: Boolean = false
        ): FastEmbedKotlinEmbeddingService {
            logger.info { "Creating FastEmbedKotlinEmbeddingService with model: $model" }

            // 创建同步和异步 TextEmbedding 实例
            val textEmbedding = TextEmbedding.create(
                model = model,
                cacheDir = cacheDir,
                showDownloadProgress = showDownloadProgress
            )

            val asyncTextEmbedding = AsyncTextEmbedding.create(
                model = model,
                cacheDir = cacheDir,
                showDownloadProgress = showDownloadProgress
            )

            return FastEmbedKotlinEmbeddingService(
                model = model,
                cacheDir = cacheDir,
                showDownloadProgress = showDownloadProgress,
                textEmbedding = textEmbedding,
                asyncTextEmbedding = asyncTextEmbedding
            )
        }
    }
}
