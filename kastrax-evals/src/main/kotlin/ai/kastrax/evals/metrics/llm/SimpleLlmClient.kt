package ai.kastrax.evals.metrics.llm

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * 简单的 LLM 客户端，用于生成评估结果。
 *
 * @param apiKey API 密钥
 * @param model 模型名称
 * @param temperature 温度参数
 * @param maxTokens 最大生成令牌数
 * @param timeout 超时时间（秒）
 */
class SimpleLlmClient(
    private val apiKey: String,
    private val model: String = "gpt-3.5-turbo",
    private val temperature: Double = 0.0,
    private val maxTokens: Int = 1024,
    private val timeout: Int = 30
) : LlmClient {
    
    override suspend fun generate(systemPrompt: String, userPrompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // 构建请求 URL
                val url = URL("https://api.openai.com/v1/chat/completions")
                
                // 打开连接
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.connectTimeout = timeout * 1000
                connection.readTimeout = timeout * 1000
                connection.doOutput = true
                
                // 构建请求体
                val requestBody = """
                {
                    "model": "$model",
                    "messages": [
                        {"role": "system", "content": "$systemPrompt"},
                        {"role": "user", "content": "$userPrompt"}
                    ],
                    "temperature": $temperature,
                    "max_tokens": $maxTokens
                }
                """.trimIndent()
                
                // 发送请求
                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                    os.flush()
                }
                
                // 获取响应
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 读取响应
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                    
                    // 解析响应
                    val contentRegex = Regex("\"content\":\\s*\"((?:\\\\.|[^\"])*?)\"")
                    val matchResult = contentRegex.find(response)
                    
                    if (matchResult != null) {
                        // 提取内容
                        val content = matchResult.groupValues[1]
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                        
                        return@withContext content
                    } else {
                        logger.error { "Failed to extract content from response: $response" }
                        return@withContext "Failed to extract content from response"
                    }
                } else {
                    // 读取错误响应
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                        reader.readText()
                    }
                    
                    logger.error { "Failed to generate text: $errorResponse" }
                    return@withContext "Failed to generate text: $errorResponse"
                }
            } catch (e: Exception) {
                logger.error(e) { "Error generating text" }
                return@withContext "Error generating text: ${e.message}"
            }
        }
    }
}

/**
 * 模拟 LLM 客户端，用于测试。
 *
 * @param responses 预定义的响应
 */
class MockLlmClient(
    private val responses: Map<String, String> = emptyMap(),
    private val defaultResponse: String = "评分：7"
) : LlmClient {
    
    override suspend fun generate(systemPrompt: String, userPrompt: String): String {
        // 模拟延迟
        TimeUnit.MILLISECONDS.sleep(100)
        
        // 返回预定义的响应或默认响应
        return responses[userPrompt] ?: defaultResponse
    }
}
