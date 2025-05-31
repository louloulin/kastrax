package com.kastrax.ai2db.nl2sql.agent

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.memory.api.Memory
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.rag.RagResult
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.impl.SimpleMessage
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.core.tools.Tool
import com.kastrax.ai2db.connection.model.DatabaseType
import com.kastrax.ai2db.nl2sql.model.SQLGenerationResult
import com.kastrax.ai2db.nl2sql.service.SQLGenerationService
import com.kastrax.ai2db.schema.model.DatabaseSchema
import org.springframework.stereotype.Component
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * 基于Kastrax Agent框架的NL2SQL智能代理
 * 
 * 该Agent集成了Memory、RAG和Tool系统，提供智能的自然语言到SQL转换能力
 */
@Component
class NL2SQLAgent(
    private val memory: Memory,
    private val rag: RAG,
    private val tools: List<Tool>,
    private val sqlGenerationService: SQLGenerationService
) : Agent {

    override val versionManager: AgentVersionManager? = null
    
    private val logger = LoggerFactory.getLogger(NL2SQLAgent::class.java)
    
    override val name: String = "nl2sql-agent"
    
    /**
     * 生成SQL查询 - 使用消息列表
     */
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: AgentGenerateOptions
    ): AgentResponse {
        val input = messages.lastOrNull()?.content ?: ""
        return generate(input, options)
    }

    /**
     * 生成SQL查询 - 使用单个提示
     */
    override suspend fun generate(
        prompt: String,
        options: AgentGenerateOptions
    ): AgentResponse = withContext(Dispatchers.IO) {
        val input = prompt
        logger.info("NL2SQL Agent processing query: {}", input)
        
        try {
            val threadId = options.threadId ?: "default"
            
            // 1. 获取对话历史
        val conversationHistory = memory.getMessages(
            threadId = threadId,
            limit = 10
        )
        logger.debug("Retrieved {} messages from conversation history", conversationHistory.size)
        
        // 2. 使用RAG检索相关知识
            val ragContext = rag.retrieveContext(
                query = input,
                limit = 5,
                minScore = 0.0,
                options = RagProcessOptions(
                    useHybridSearch = true,
                    useSemanticRetrieval = true,
                    useReranking = true
                )
            )
            logger.debug("RAG retrieved {} relevant documents", ragContext.documents.size)
            
            // 3. 获取数据库模式信息（通过Tool）
            val schemaInfo = getSchemaInfo(options.metadata)
            
            // 4. 构建增强提示
        val enhancedPrompt = buildEnhancedPrompt(
            query = input,
            conversationHistory = conversationHistory.map { memoryMsg -> memoryMsg.message },
            ragContext = RagResult(
                documents = ragContext.documents,
                query = input,
                metadata = emptyMap()
            ),
            schemaInfo = schemaInfo
        )
            
            // 5. 生成SQL
            val sqlResult = sqlGenerationService.generateSQL(
                prompt = enhancedPrompt,
                schema = schemaInfo
            )
            
            // 6. 验证生成的SQL（通过Tool）
            val validationResult = validateSQL(sqlResult, schemaInfo)
            
            // 7. 保存到Memory
            memory.saveMessage(
                SimpleMessage(MessageRole.USER, input),
                threadId
            )
            memory.saveMessage(
                SimpleMessage(
                    role = MessageRole.ASSISTANT,
                    content = sqlResult.sql
                ),
                threadId
            )
            
            logger.info("Successfully generated SQL with confidence: {}", sqlResult.confidence)
            
            return@withContext AgentResponse(
                text = sqlResult.sql,
                result = mapOf(
                    "confidence" to sqlResult.confidence,
                    "explanation" to sqlResult.explanation,
                    "validation" to validationResult,
                    "timestamp" to Instant.now().toString(),
                    "agent_name" to name
                )
            )
            
        } catch (e: Exception) {
            logger.error("Error in NL2SQL Agent generation", e)
            
            // 保存错误信息到Memory
            memory.saveMessage(
                SimpleMessage(MessageRole.USER, input),
                options.threadId ?: "default"
            )
            memory.saveMessage(
                SimpleMessage(
                    role = MessageRole.ASSISTANT,
                    content = "Error: ${e.message}"
                ),
                options.threadId ?: "default"
            )
            
            throw e
        }
    }
    
    /**
     * Stream a response from the agent.
     */
    override suspend fun stream(
        prompt: String,
        options: AgentStreamOptions
    ): AgentResponse {
        // For now, just delegate to generate method
        return generate(prompt, AgentGenerateOptions())
    }

    /**
     * Reset the agent's state.
     */
    override suspend fun reset() {
        // Reset implementation if needed
        logger.info("NL2SQL Agent state reset")
    }

    /**
     * Get the agent's current state.
     */
    override suspend fun getState(): ai.kastrax.core.agent.AgentState? {
        // Return current state if needed
        return null
    }

    /**
     * Update the agent's state.
     */
    override suspend fun updateState(status: ai.kastrax.core.agent.AgentStatus): ai.kastrax.core.agent.AgentState? {
        // Update state implementation if needed
        return null
    }

    /**
     * Create a new version of the agent.
     */
    override suspend fun createVersion(
        instructions: String,
        name: String?,
        description: String?,
        metadata: Map<String, String>,
        activateImmediately: Boolean
    ): AgentVersion? {
        // Create version implementation if needed
        return null
    }

    /**
     * Get versions of the agent.
     */
    override suspend fun getVersions(
        limit: Int,
        offset: Int
    ): List<AgentVersion>? {
        // Get versions implementation if needed
        return null
    }

    /**
     * Get the active version of the agent.
     */
    override suspend fun getActiveVersion(): AgentVersion? {
        // Get active version implementation if needed
        return null
    }

    /**
     * Activate a version of the agent.
     */
    override suspend fun activateVersion(versionId: String): AgentVersion? {
        // Activate version implementation if needed
        return null
    }

    /**
     * Rollback to a version of the agent.
     */
    override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
        // Rollback to version implementation if needed
        return null
    }

    /**
     * Create a new session.
     */
    override suspend fun createSession(
        title: String?,
        resourceId: String?,
        metadata: Map<String, String>
    ): SessionInfo? {
        // Create session implementation if needed
        return null
    }

    /**
     * Get session information.
     */
    override suspend fun getSession(sessionId: String): SessionInfo? {
        // Get session implementation if needed
        return null
    }

    /**
     * Get session messages.
     */
    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        // Get session messages implementation if needed
        return null
    }

    /**
     * 转换自然语言到SQL的便捷方法
     */
    suspend fun convertToSQL(
        query: String,
        databaseId: String,
        threadId: String? = null
    ): SQLConversionResult {
        return try {
            val options = AgentGenerateOptions(
                threadId = threadId,
                metadata = mapOf("database" to databaseId)
            )
            
            val result = generate(query, options)
            
            SQLConversionResult(
                isSuccess = true,
                sql = result.text,
                confidence = (result.result as? Map<String, Any>)?.get("confidence") as? Double ?: 0.0,
                explanation = (result.result as? Map<String, Any>)?.get("explanation") as? String ?: "",
                error = null
            )
        } catch (e: Exception) {
            SQLConversionResult(
                isSuccess = false,
                sql = null,
                confidence = 0.0,
                explanation = "",
                error = e.message
            )
        }
    }
    
    /**
     * 构建增强提示
     */
    private fun buildEnhancedPrompt(
        query: String,
        conversationHistory: List<ai.kastrax.memory.api.Message>,
        ragContext: RagResult,
        schemaInfo: DatabaseSchema?
    ): String {
        val promptBuilder = StringBuilder()
        
        // 系统提示
        promptBuilder.append("""你是一个专业的SQL生成助手。请根据用户的自然语言查询生成准确的SQL语句。

""")
        
        // 数据库模式信息
        if (schemaInfo != null) {
            promptBuilder.append("数据库模式信息：\n")
            schemaInfo.tables.forEach { table ->
                promptBuilder.append("表名: ${table.name}\n")
                table.columns.forEach { column ->
                    promptBuilder.append("  - ${column.name}: ${column.type}")
                    if (column.isPrimaryKey) promptBuilder.append(" (主键)")
                    if (column.isForeignKey) promptBuilder.append(" (外键)")
                    promptBuilder.append("\n")
                }
                promptBuilder.append("\n")
            }
        }
        
        // RAG检索的相关知识
        if (ragContext.documents.isNotEmpty()) {
            promptBuilder.append("相关SQL示例和知识：\n")
            ragContext.documents.take(3).forEach { doc ->
                promptBuilder.append("- ${doc.content}\n")
            }
            promptBuilder.append("\n")
        }
        
        // 对话历史
        if (conversationHistory.isNotEmpty()) {
            promptBuilder.append("对话历史：\n")
            conversationHistory.takeLast(5).forEach { message ->
                promptBuilder.append("${message.role}: ${message.content}\n")
            }
            promptBuilder.append("\n")
        }
        
        // 用户查询
        promptBuilder.append("用户查询: $query\n\n")
        promptBuilder.append("请生成对应的SQL语句，并提供简要说明：")
        
        return promptBuilder.toString()
    }
    
    /**
     * 获取数据库模式信息
     */
    private suspend fun getSchemaInfo(metadata: Map<String, Any>?): DatabaseSchema? {
        return try {
            val schemaTool = tools.find { it.id == "database-schema" }
            if (schemaTool != null && metadata?.containsKey("database") == true) {
                // 通过Tool获取模式信息
                val result = schemaTool.execute(
                    input = mapOf("database" to metadata["database"]),
                    context = null
                )
                // 解析结果为DatabaseSchema
                parseSchemaFromToolResult(result.output)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to get schema info", e)
            null
        }
    }
    
    /**
     * 验证生成的SQL
     */
    private suspend fun validateSQL(
        sqlResult: SQLGenerationResult,
        schema: DatabaseSchema?
    ): Map<String, Any> {
        return try {
            val validationTool = tools.find { it.id == "sql-validation" }
            if (validationTool != null) {
                val result = validationTool.execute(
                    input = mapOf(
                        "sql" to sqlResult.sql,
                        "schema" to (schema?.name ?: "")
                    ),
                    context = null
                )
                result.output as? Map<String, Any> ?: mapOf("valid" to true)
            } else {
                mapOf("valid" to true, "message" to "No validation tool available")
            }
        } catch (e: Exception) {
            logger.warn("SQL validation failed", e)
            mapOf("valid" to false, "error" to e.message)
        }
    }
    
    /**
     * 解析Tool结果为DatabaseSchema
     */
    private fun parseSchemaFromToolResult(output: Any): DatabaseSchema? {
        return try {
            when (output) {
                is Map<*, *> -> {
                    val schemaMap = output as? Map<String, Any>
                    schemaMap?.get("schema") as? DatabaseSchema
                }
                is DatabaseSchema -> output
                else -> null
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse schema from tool result", e)
            null
        }
    }
}

/**
 * SQL转换结果
 */
data class SQLConversionResult(
    val isSuccess: Boolean,
    val sql: String?,
    val confidence: Double,
    val explanation: String,
    val error: String?
)