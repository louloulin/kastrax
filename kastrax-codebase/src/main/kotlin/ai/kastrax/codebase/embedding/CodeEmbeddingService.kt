package ai.kastrax.codebase.embedding

import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.rag.embedding.CachedEmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private val logger = KotlinLogging.logger {}

/**
 * 代码嵌入服务配置
 *
 * @property cacheSize 缓存大小
 * @property cacheExpirationDuration 缓存过期时间
 * @property batchSize 批处理大小
 * @property useGpu 是否使用 GPU
 * @property modelVersion 模型版本
 */
data class CodeEmbeddingServiceConfig(
    val cacheSize: Int = 10000,
    val cacheExpirationDuration: Duration = 24.hours,
    val batchSize: Int = 32,
    val useGpu: Boolean = false,
    val modelVersion: String = "latest"
)

/**
 * 代码嵌入服务
 *
 * 为代码文件生成高质量的嵌入向量
 *
 * @property baseEmbeddingService 基础嵌入服务
 * @property config 配置
 */
class CodeEmbeddingService(
    private val baseEmbeddingService: EmbeddingService,
    private val config: CodeEmbeddingServiceConfig = CodeEmbeddingServiceConfig()
) : Closeable {
    // 缓存嵌入服务
    private val cachedEmbeddingService: CachedEmbeddingService = CachedEmbeddingService(
        delegate = object : ai.kastrax.rag.embedding.EmbeddingService() {
            override val dimension: Int = 1536
            override suspend fun embed(text: String): FloatArray = baseEmbeddingService.embed(text)
            override suspend fun embedBatch(texts: List<String>): List<FloatArray> = baseEmbeddingService.embedBatch(texts)
            override fun close() {}
        },
        cacheSize = config.cacheSize
    )

    /**
     * 嵌入维度
     */
    val dimension: Int = 1536

    /**
     * 嵌入单个文本
     *
     * @param text 文本
     * @return 嵌入向量
     */
    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        // 预处理代码文本
        val processedText = preprocessCode(text)

        // 使用缓存嵌入服务生成嵌入
        return@withContext cachedEmbeddingService.embed(processedText)
    }

    /**
     * 批量嵌入文本
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray> = withContext(Dispatchers.Default) {
        // 预处理代码文本
        val processedTexts = texts.map { preprocessCode(it) }

        // 使用缓存嵌入服务生成嵌入
        return@withContext cachedEmbeddingService.embedBatch(processedTexts)
    }

    /**
     * 预处理代码文本
     *
     * @param code 代码文本
     * @return 预处理后的代码文本
     */
    private fun preprocessCode(code: String): String {
        // 移除注释
        var processedCode = removeComments(code)

        // 规范化空白字符
        processedCode = normalizeWhitespace(processedCode)

        // 移除不必要的字符
        processedCode = removeUnnecessaryCharacters(processedCode)

        return processedCode
    }

    /**
     * 移除注释
     *
     * @param code 代码文本
     * @return 移除注释后的代码文本
     */
    private fun removeComments(code: String): String {
        // 移除 Java/Kotlin 风格的多行注释
        var result = code.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")

        // 移除 Java/Kotlin 风格的单行注释
        result = result.replace(Regex("//.*"), "")

        // 移除 Python 风格的单行注释
        result = result.replace(Regex("#.*"), "")

        return result
    }

    /**
     * 规范化空白字符
     *
     * @param code 代码文本
     * @return 规范化空白字符后的代码文本
     */
    private fun normalizeWhitespace(code: String): String {
        // 将多个空白字符替换为单个空格
        var result = code.replace(Regex("\\s+"), " ")

        // 移除行首和行尾的空白字符
        result = result.trim()

        return result
    }

    /**
     * 移除不必要的字符
     *
     * @param code 代码文本
     * @return 移除不必要的字符后的代码文本
     */
    private fun removeUnnecessaryCharacters(code: String): String {
        // 保留代码的基本结构，但移除一些不必要的字符
        return code
    }

    /**
     * 关闭资源
     */
    override fun close() {
        cachedEmbeddingService.close()
    }

    companion object {
        /**
         * 创建代码嵌入服务
         *
         * @param baseEmbeddingService 基础嵌入服务
         * @param config 配置
         * @return 代码嵌入服务
         */
        fun create(
            baseEmbeddingService: EmbeddingService,
            config: CodeEmbeddingServiceConfig = CodeEmbeddingServiceConfig()
        ): CodeEmbeddingService {
            return CodeEmbeddingService(
                baseEmbeddingService = baseEmbeddingService,
                config = config
            )
        }
    }
}
