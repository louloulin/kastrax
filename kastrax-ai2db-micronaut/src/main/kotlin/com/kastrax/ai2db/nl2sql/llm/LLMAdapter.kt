package com.kastrax.ai2db.nl2sql.llm

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant

/**
 * LLM配置属性
 */
@ConfigurationProperties("kastrax.ai2db.llm")
data class LLMConfig(
    val provider: String = "openai",
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-3.5-turbo",
    val timeout: Long = 30000,
    val maxTokens: Int = 2048,
    val temperature: Double = 0.1,
    val retryAttempts: Int = 3
)

/**
 * LLM响应数据类
 */
data class LLMResponse(
    val sqlText: String,
    val confidence: Double,
    val reasoning: String? = null,
    val tokensUsed: Int = 0,
    val responseTime: Duration = Duration.ZERO
)

/**
 * OpenAI API请求/响应数据类
 */
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    @JsonProperty("max_tokens") val maxTokens: Int,
    val temperature: Double,
    val stream: Boolean = false
)

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIResponse(
    val id: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage?
)

data class OpenAIChoice(
    val message: OpenAIMessage,
    @JsonProperty("finish_reason") val finishReason: String
)

data class OpenAIUsage(
    @JsonProperty("total_tokens") val totalTokens: Int,
    @JsonProperty("prompt_tokens") val promptTokens: Int,
    @JsonProperty("completion_tokens") val completionTokens: Int
)

/**
 * LLM适配器 - Micronaut版本
 * 
 * 支持多种LLM提供商，用于生成SQL查询
 */
@Singleton
class LLMAdapter(
    @Client("\${kastrax.ai2db.llm.base-url}") private val httpClient: HttpClient,
    private val config: LLMConfig,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(LLMAdapter::class.java)
    
    /**
     * 生成SQL查询
     *
     * @param prompt 构建的提示
     * @param timeout 超时时间
     * @return LLM响应
     */
    suspend fun generateSQL(
        prompt: String,
        timeout: Long = config.timeout
    ): LLMResponse = withContext(Dispatchers.IO) {
        logger.debug("Generating SQL using LLM provider: {}", config.provider)
        
        val startTime = Instant.now()
        
        try {
            val response = when (config.provider.lowercase()) {
                "openai" -> generateWithOpenAI(prompt, timeout)
                "anthropic" -> generateWithAnthropic(prompt, timeout)
                "deepseek" -> generateWithDeepSeek(prompt, timeout)
                else -> throw UnsupportedOperationException("Unsupported LLM provider: ${config.provider}")
            }
            
            val endTime = Instant.now()
            val responseTime = Duration.between(startTime, endTime)
            
            logger.info("LLM response generated in {}ms, tokens used: {}", 
                responseTime.toMillis(), response.tokensUsed)
            
            return@withContext response.copy(responseTime = responseTime)
            
        } catch (e: Exception) {
            logger.error("Failed to generate SQL with LLM provider: {}", config.provider, e)
            throw LLMException("Failed to generate SQL: ${e.message}", e)
        }
    }
    
    /**
     * 使用OpenAI生成SQL
     */
    private suspend fun generateWithOpenAI(prompt: String, timeout: Long): LLMResponse {
        logger.debug("Using OpenAI to generate SQL")
        
        val request = OpenAIRequest(
            model = config.model,
            messages = listOf(
                OpenAIMessage(
                    role = "system",
                    content = "You are a SQL expert. Generate only valid SQL queries based on the given schema and natural language request. Return only the SQL query without any explanation."
                ),
                OpenAIMessage(
                    role = "user",
                    content = prompt
                )
            ),
            maxTokens = config.maxTokens,
            temperature = config.temperature
        )
        
        val httpRequest = HttpRequest.POST("/chat/completions", request)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
        
        val response = httpClient.toBlocking().retrieve(httpRequest, OpenAIResponse::class.java)
        
        val sqlText = response.choices.firstOrNull()?.message?.content?.trim() ?: ""
        val tokensUsed = response.usage?.totalTokens ?: 0
        
        // 计算置信度（简单实现）
        val confidence = calculateConfidence(sqlText, response.choices.firstOrNull()?.finishReason)
        
        return LLMResponse(
            sqlText = cleanSQLText(sqlText),
            confidence = confidence,
            tokensUsed = tokensUsed
        )
    }
    
    /**
     * 使用Anthropic生成SQL
     */
    private suspend fun generateWithAnthropic(prompt: String, timeout: Long): LLMResponse {
        logger.debug("Using Anthropic to generate SQL")
        
        // Anthropic API实现
        // 这里需要根据Anthropic的API格式实现
        throw UnsupportedOperationException("Anthropic provider not yet implemented")
    }
    
    /**
     * 使用DeepSeek生成SQL
     */
    private suspend fun generateWithDeepSeek(prompt: String, timeout: Long): LLMResponse {
        logger.debug("Using DeepSeek to generate SQL")
        
        // DeepSeek API实现（通常兼容OpenAI格式）
        return generateWithOpenAI(prompt, timeout)
    }
    
    /**
     * 清理SQL文本
     */
    private fun cleanSQLText(sqlText: String): String {
        return sqlText
            .replace("```sql", "")
            .replace("```", "")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .removeSuffix(";")
    }
    
    /**
     * 计算置信度
     */
    private fun calculateConfidence(sqlText: String, finishReason: String?): Double {
        var confidence = 0.5 // 基础置信度
        
        // 根据完成原因调整置信度
        when (finishReason) {
            "stop" -> confidence += 0.3
            "length" -> confidence += 0.1
            else -> confidence += 0.0
        }
        
        // 根据SQL质量调整置信度
        if (sqlText.isNotBlank()) {
            confidence += 0.2
        }
        
        if (sqlText.uppercase().contains("SELECT")) {
            confidence += 0.1
        }
        
        if (sqlText.uppercase().contains("FROM")) {
            confidence += 0.1
        }
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    /**
     * 测试LLM连接
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.debug("Testing LLM connection for provider: {}", config.provider)
            
            val testPrompt = "Generate a simple SELECT statement for a table named 'users'"
            val response = generateSQL(testPrompt, 10000)
            
            val isValid = response.sqlText.isNotBlank() && 
                         response.sqlText.uppercase().contains("SELECT")
            
            logger.info("LLM connection test {}: {}", 
                if (isValid) "passed" else "failed", response.sqlText)
            
            return@withContext isValid
            
        } catch (e: Exception) {
            logger.error("LLM connection test failed", e)
            return@withContext false
        }
    }
    
    /**
     * 获取LLM状态信息
     */
    fun getStatus(): LLMStatus {
        return LLMStatus(
            provider = config.provider,
            model = config.model,
            baseUrl = config.baseUrl,
            isConfigured = config.apiKey.isNotBlank()
        )
    }
}

/**
 * LLM状态信息
 */
data class LLMStatus(
    val provider: String,
    val model: String,
    val baseUrl: String,
    val isConfigured: Boolean
)

/**
 * LLM异常
 */
class LLMException(message: String, cause: Throwable? = null) : Exception(message, cause)