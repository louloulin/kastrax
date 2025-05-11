package ai.kastrax.rag.multimodal

import ai.kastrax.rag.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.math.pow
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * 基于 OpenAI API 的多模态嵌入服务。
 *
 * @property apiKey OpenAI API 密钥
 * @property model 嵌入模型
 * @property dimensions 嵌入向量维度
 * @property visionModel 视觉模型
 * @property audioModel 音频模型
 * @property batchSize 批处理大小
 * @property maxRetries 最大重试次数
 * @property timeout 请求超时时间（毫秒）
 * @property tempDir 临时目录
 */
class OpenAIMultimodalEmbeddingService(
    private val apiKey: String,
    private val model: String = "text-embedding-3-small",
    private val dimensions: Int = 1536,
    private val visionModel: String = "gpt-4-vision-preview",
    private val audioModel: String = "whisper-1",
    private val batchSize: Int = 16,
    private val maxRetries: Int = 3,
    private val timeout: Long = 30000,
    private val tempDir: String = System.getProperty("java.io.tmpdir")
) : MultimodalEmbeddingService() {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = maxRetries)
            exponentialDelay()
        }
    }

    /**
     * 生成文本的嵌入向量。
     *
     * @param text 文本
     * @return 嵌入向量
     */
    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        try {
            var retries = 0
            var lastException: Exception? = null

            while (retries < maxRetries) {
                try {
                    val response = client.post("https://api.openai.com/v1/embeddings") {
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $apiKey")
                            append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        }
                        setBody(buildJsonObject {
                            put("model", model)
                            put("input", text)
                            put("dimensions", dimensions)
                        })
                    }

                    val responseBody = response.body<JsonObject>()
                    val data = responseBody["data"]?.jsonArray?.firstOrNull()?.jsonObject
                    val embedding = data?.get("embedding")?.jsonArray

                    if (embedding != null) {
                        return@withContext FloatArray(embedding.size) { i ->
                            embedding[i].jsonPrimitive.float
                        }
                    } else {
                        throw Exception("Failed to get embedding from response: $responseBody")
                    }
                } catch (e: Exception) {
                    lastException = e
                    logger.warn(e) { "Error generating embedding for text (attempt ${retries + 1}/$maxRetries): ${e.message}" }
                    retries++
                    if (retries < maxRetries) {
                        val backoffTime = (2.0.pow(retries.toDouble()) * 1000).toLong()
                        delay(backoffTime)
                    }
                }
            }

            throw lastException ?: Exception("Failed to generate embedding after $maxRetries attempts")
        } catch (e: Exception) {
            logger.error(e) { "Error generating embedding for text: ${e.message}" }
            throw e
        }
    }

    /**
     * 生成多个文本的嵌入向量。
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    override suspend fun embedBatch(texts: List<String>): List<FloatArray> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) {
            return@withContext emptyList<FloatArray>()
        }

        // 分批处理，每批最多 batchSize 个文本
        val batches = texts.chunked(batchSize)
        val results = mutableListOf<FloatArray>()

        for (batch in batches) {
            try {
                var retries = 0
                var lastException: Exception? = null

                while (retries < maxRetries) {
                    try {
                        val response = client.post("https://api.openai.com/v1/embeddings") {
                            headers {
                                append(HttpHeaders.Authorization, "Bearer $apiKey")
                                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            }
                            setBody(buildJsonObject {
                                put("model", model)
                                putJsonArray("input") {
                                    batch.forEach { add(it) }
                                }
                                put("dimensions", dimensions)
                            })
                        }

                        val responseBody = response.body<JsonObject>()
                        val data = responseBody["data"]?.jsonArray

                        if (data != null) {
                            val batchResults = data.map { item ->
                                val embedding = item.jsonObject["embedding"]?.jsonArray
                                if (embedding != null) {
                                    FloatArray(embedding.size) { i ->
                                        embedding[i].jsonPrimitive.float
                                    }
                                } else {
                                    throw Exception("Failed to get embedding from response item: $item")
                                }
                            }
                            results.addAll(batchResults)
                            break
                        } else {
                            throw Exception("Failed to get data from response: $responseBody")
                        }
                    } catch (e: Exception) {
                        lastException = e
                        logger.warn(e) { "Error generating batch embeddings (attempt ${retries + 1}/$maxRetries): ${e.message}" }
                        retries++
                        if (retries < maxRetries) {
                            val backoffTime = (2.0.pow(retries.toDouble()) * 1000).toLong()
                            delay(backoffTime)
                        }
                    }
                }

                if (retries >= maxRetries) {
                    throw lastException ?: Exception("Failed to generate batch embeddings after $maxRetries attempts")
                }
            } catch (e: Exception) {
                logger.error(e) { "Error generating batch embeddings: ${e.message}" }
                throw e
            }
        }

        return@withContext results
    }

    /**
     * 嵌入向量的维度。
     */
    override val dimension: Int = dimensions

    /**
     * 生成图像的嵌入向量。
     *
     * @param imageUrl 图像 URL
     * @return 嵌入向量
     */
    override suspend fun embedImage(imageUrl: String): FloatArray = withContext(Dispatchers.IO) {
        try {
            logger.info { "Generating embedding for image: $imageUrl" }

            // 下载图像
            val imageFile = downloadFile(imageUrl)

            // 使用 OpenAI API 生成图像描述
            val imageDescription = generateImageDescription(imageFile)

            // 删除临时文件
            imageFile.delete()

            // 使用文本嵌入服务生成嵌入向量
            embed(imageDescription)
        } catch (e: Exception) {
            logger.error(e) { "Error generating embedding for image: $imageUrl" }
            FloatArray(dimensions) { 0f }
        }
    }

    /**
     * 生成音频的嵌入向量。
     *
     * @param audioUrl 音频 URL
     * @return 嵌入向量
     */
    override suspend fun embedAudio(audioUrl: String): FloatArray = withContext(Dispatchers.IO) {
        try {
            logger.info { "Generating embedding for audio: $audioUrl" }

            // 下载音频
            val audioFile = downloadFile(audioUrl)

            // 使用 OpenAI API 转录音频
            val audioTranscription = transcribeAudio(audioFile)

            // 删除临时文件
            audioFile.delete()

            // 使用文本嵌入服务生成嵌入向量
            embed(audioTranscription)
        } catch (e: Exception) {
            logger.error(e) { "Error generating embedding for audio: $audioUrl" }
            FloatArray(dimensions) { 0f }
        }
    }

    /**
     * 生成视频的嵌入向量。
     *
     * @param videoUrl 视频 URL
     * @return 嵌入向量
     */
    override suspend fun embedVideo(videoUrl: String): FloatArray = withContext(Dispatchers.IO) {
        try {
            logger.info { "Generating embedding for video: $videoUrl" }

            // 下载视频
            val videoFile = downloadFile(videoUrl)

            // 提取视频的关键帧
            val keyFrames = extractKeyFrames(videoFile)

            // 为每个关键帧生成描述
            val frameDescriptions = keyFrames.map { generateImageDescription(it) }

            // 删除临时文件
            videoFile.delete()
            keyFrames.forEach { it.delete() }

            // 组合所有描述
            val combinedDescription = frameDescriptions.joinToString("\n\n")

            // 使用文本嵌入服务生成嵌入向量
            embed(combinedDescription)
        } catch (e: Exception) {
            logger.error(e) { "Error generating embedding for video: $videoUrl" }
            FloatArray(dimensions) { 0f }
        }
    }

    /**
     * 下载文件。
     *
     * @param url 文件 URL
     * @return 下载的文件
     */
    private suspend fun downloadFile(url: String): File = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("kastrax_", getFileExtension(url), File(tempDir))

        try {
            val connection = URL(url).openConnection()
            connection.connect()

            connection.getInputStream().use { input ->
                Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            tempFile
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    /**
     * 获取文件扩展名。
     *
     * @param url 文件 URL
     * @return 文件扩展名
     */
    private fun getFileExtension(url: String): String {
        val lastDotIndex = url.lastIndexOf('.')
        return if (lastDotIndex != -1) {
            url.substring(lastDotIndex)
        } else {
            ""
        }
    }

    /**
     * 生成图像描述。
     *
     * @param imageFile 图像文件
     * @return 图像描述
     */
    private suspend fun generateImageDescription(imageFile: File): String = withContext(Dispatchers.IO) {
        val response = client.post("https://api.openai.com/v1/chat/completions") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $apiKey")
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }

            setBody(
                buildJsonObject {
                    put("model", visionModel)
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", "user")
                            putJsonArray("content") {
                                addJsonObject {
                                    put("type", "text")
                                    put("text", "Describe this image in detail.")
                                }
                                addJsonObject {
                                    put("type", "image_url")
                                    putJsonObject("image_url") {
                                        put("url", "data:image/jpeg;base64,${imageFile.readBytes().encodeBase64()}")
                                    }
                                }
                            }
                        }
                    }
                    put("max_tokens", 300)
                }
            )
        }

        val responseBody = response.bodyAsText()
        val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject

        jsonResponse["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content ?: "No description available"
    }

    /**
     * 转录音频。
     *
     * @param audioFile 音频文件
     * @return 音频转录
     */
    private suspend fun transcribeAudio(audioFile: File): String = withContext(Dispatchers.IO) {
        val response = client.submitFormWithBinaryData(
            url = "https://api.openai.com/v1/audio/transcriptions",
            formData = formData {
                append("file", audioFile.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "audio/mpeg")
                    append(HttpHeaders.ContentDisposition, "filename=${audioFile.name}")
                })
                append("model", audioModel)
            }
        ) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $apiKey")
            }
        }

        val responseBody = response.bodyAsText()
        val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject

        jsonResponse["text"]?.jsonPrimitive?.content ?: "No transcription available"
    }

    /**
     * 提取视频的关键帧。
     *
     * @param videoFile 视频文件
     * @return 关键帧文件列表
     */
    private suspend fun extractKeyFrames(videoFile: File): List<File> = withContext(Dispatchers.IO) {
        // 这里使用 FFmpeg 提取关键帧
        // 为了简化示例，我们只提取 3 个关键帧
        val keyFrames = mutableListOf<File>()

        try {
            val outputDir = File(tempDir, "kastrax_keyframes_${System.currentTimeMillis()}")
            outputDir.mkdirs()

            // 使用 FFmpeg 提取关键帧
            val process = ProcessBuilder(
                "ffmpeg",
                "-i", videoFile.absolutePath,
                "-vf", "select='eq(pict_type,PICT_TYPE_I)'",
                "-vsync", "0",
                "-frames:v", "3",
                "-q:v", "2",
                "${outputDir.absolutePath}/frame_%03d.jpg"
            ).start()

            process.waitFor()

            // 获取生成的关键帧
            outputDir.listFiles()?.filter { it.name.startsWith("frame_") }?.forEach {
                keyFrames.add(it)
            }

            keyFrames
        } catch (e: Exception) {
            logger.error(e) { "Error extracting key frames from video: ${videoFile.absolutePath}" }
            emptyList()
        }
    }

    /**
     * 将字节数组编码为 Base64 字符串。
     *
     * @return Base64 编码的字符串
     */
    private fun ByteArray.encodeBase64(): String {
        return java.util.Base64.getEncoder().encodeToString(this)
    }

    /**
     * 关闭资源。
     */
    override fun close() {
        client.close()
    }
}
