package com.kastrax.ai2db.nl2sql.converter

import com.kastrax.ai2db.nl2sql.llm.LLMAdapter
import com.kastrax.ai2db.nl2sql.model.ConversationContext
import com.kastrax.ai2db.nl2sql.model.SQLQuery
import com.kastrax.ai2db.nl2sql.parser.SQLParser
import com.kastrax.ai2db.nl2sql.prompt.SQLPromptBuilder
import com.kastrax.ai2db.schema.model.DatabaseSchema
import io.micronaut.context.annotation.ConfigurationProperties
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * NL2SQL配置属性
 */
@ConfigurationProperties("kastrax.ai2db.nl2sql")
data class NL2SQLConfig(
    val maxRetries: Int = 3,
    val timeout: Long = 30000,
    val enableCache: Boolean = true,
    val cacheTtl: Long = 3600,
    val enableValidation: Boolean = true
)

/**
 * 自然语言到SQL转换器 - Micronaut版本
 * 
 * 使用LLM将自然语言查询转换为SQL语句
 */
@Singleton
class NL2SQLConverter(
    private val llmAdapter: LLMAdapter,
    private val sqlParser: SQLParser,
    private val promptBuilder: SQLPromptBuilder,
    private val config: NL2SQLConfig
) {
    private val logger = LoggerFactory.getLogger(NL2SQLConverter::class.java)
    
    // 简单的内存缓存
    private val queryCache = mutableMapOf<String, CachedQuery>()
    
    /**
     * 将自然语言转换为SQL查询
     *
     * @param naturalLanguageQuery 自然语言查询
     * @param schema 数据库模式
     * @param context 对话上下文
     * @return SQL查询结果
     */
    suspend fun convertToSQL(
        naturalLanguageQuery: String,
        schema: DatabaseSchema,
        context: ConversationContext? = null
    ): SQLQuery = withContext(Dispatchers.IO) {
        logger.info("Converting natural language query to SQL: {}", naturalLanguageQuery)
        
        // 检查缓存
        if (config.enableCache) {
            val cacheKey = generateCacheKey(naturalLanguageQuery, schema)
            val cachedQuery = queryCache[cacheKey]
            
            if (cachedQuery != null && !cachedQuery.isExpired()) {
                logger.debug("Returning cached SQL query for: {}", naturalLanguageQuery)
                return@withContext cachedQuery.sqlQuery
            }
        }
        
        var lastException: Exception? = null
        
        // 重试机制
        repeat(config.maxRetries) { attempt ->
            try {
                logger.debug("Attempt {} to convert query: {}", attempt + 1, naturalLanguageQuery)
                
                // 构建提示
                val prompt = promptBuilder.buildPrompt(
                    naturalLanguageQuery = naturalLanguageQuery,
                    schema = schema,
                    context = context
                )
                
                // 调用LLM
                val llmResponse = llmAdapter.generateSQL(
                    prompt = prompt,
                    timeout = config.timeout
                )
                
                // 解析SQL
                val sqlQuery = sqlParser.parseSQL(
                    sqlText = llmResponse.sqlText,
                    originalQuery = naturalLanguageQuery,
                    confidence = llmResponse.confidence
                )
                
                // 验证SQL（如果启用）
                if (config.enableValidation) {
                    validateSQL(sqlQuery, schema)
                }
                
                // 缓存结果
                if (config.enableCache) {
                    val cacheKey = generateCacheKey(naturalLanguageQuery, schema)
                    queryCache[cacheKey] = CachedQuery(
                        sqlQuery = sqlQuery,
                        timestamp = Instant.now(),
                        ttl = config.cacheTtl
                    )
                }
                
                logger.info("Successfully converted query to SQL: {}", sqlQuery.sql)
                return@withContext sqlQuery
                
            } catch (e: Exception) {
                lastException = e
                logger.warn("Attempt {} failed for query: {}", attempt + 1, naturalLanguageQuery, e)
                
                if (attempt < config.maxRetries - 1) {
                    // 等待一段时间后重试
                    kotlinx.coroutines.delay(1000 * (attempt + 1))
                }
            }
        }
        
        // 所有重试都失败了
        val errorMessage = "Failed to convert natural language query to SQL after ${config.maxRetries} attempts"
        logger.error(errorMessage, lastException)
        throw NL2SQLException(errorMessage, lastException)
    }
    
    /**
     * 批量转换查询
     */
    suspend fun convertBatch(
        queries: List<String>,
        schema: DatabaseSchema,
        context: ConversationContext? = null
    ): List<SQLQuery> = withContext(Dispatchers.IO) {
        logger.info("Converting {} queries in batch", queries.size)
        
        val results = mutableListOf<SQLQuery>()
        
        for ((index, query) in queries.withIndex()) {
            try {
                logger.debug("Processing batch query {}/{}: {}", index + 1, queries.size, query)
                val sqlQuery = convertToSQL(query, schema, context)
                results.add(sqlQuery)
            } catch (e: Exception) {
                logger.error("Failed to convert batch query {}: {}", index + 1, query, e)
                // 创建错误查询对象
                results.add(SQLQuery(
                    sql = "",
                    originalQuery = query,
                    confidence = 0.0,
                    error = e.message,
                    timestamp = Instant.now()
                ))
            }
        }
        
        logger.info("Completed batch conversion: {}/{} successful", 
            results.count { it.error == null }, queries.size)
        
        return@withContext results
    }
    
    /**
     * 验证生成的SQL
     */
    private fun validateSQL(sqlQuery: SQLQuery, schema: DatabaseSchema) {
        logger.debug("Validating SQL: {}", sqlQuery.sql)
        
        // 基本语法检查
        if (sqlQuery.sql.isBlank()) {
            throw NL2SQLException("Generated SQL is empty")
        }
        
        // 检查是否包含危险操作
        val dangerousKeywords = listOf("DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "INSERT", "UPDATE")
        val upperSQL = sqlQuery.sql.uppercase()
        
        for (keyword in dangerousKeywords) {
            if (upperSQL.contains(keyword)) {
                logger.warn("SQL contains potentially dangerous keyword: {}", keyword)
                // 可以选择抛出异常或记录警告
            }
        }
        
        // 检查表名是否存在于schema中
        val tableNames = schema.tables.map { it.name.uppercase() }
        // 这里可以添加更复杂的表名提取和验证逻辑
        
        logger.debug("SQL validation passed for: {}", sqlQuery.sql)
    }
    
    /**
     * 生成缓存键
     */
    private fun generateCacheKey(query: String, schema: DatabaseSchema): String {
        return "${query.hashCode()}_${schema.name}_${schema.version}"
    }
    
    /**
     * 清理过期缓存
     */
    fun cleanupCache() {
        val expiredKeys = queryCache.filter { (_, cachedQuery) -> 
            cachedQuery.isExpired() 
        }.keys
        
        expiredKeys.forEach { queryCache.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            logger.debug("Cleaned up {} expired cache entries", expiredKeys.size)
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        val totalEntries = queryCache.size
        val expiredEntries = queryCache.values.count { it.isExpired() }
        
        return CacheStats(
            totalEntries = totalEntries,
            activeEntries = totalEntries - expiredEntries,
            expiredEntries = expiredEntries
        )
    }
}

/**
 * 缓存的查询
 */
data class CachedQuery(
    val sqlQuery: SQLQuery,
    val timestamp: Instant,
    val ttl: Long
) {
    fun isExpired(): Boolean {
        return Instant.now().isAfter(timestamp.plusSeconds(ttl))
    }
}

/**
 * 缓存统计信息
 */
data class CacheStats(
    val totalEntries: Int,
    val activeEntries: Int,
    val expiredEntries: Int
)

/**
 * NL2SQL转换异常
 */
class NL2SQLException(message: String, cause: Throwable? = null) : Exception(message, cause)