package com.kastrax.ai2db.nl2sql.config

import ai.kastrax.memory.api.Memory
import ai.kastrax.rag.RAG
import ai.kastrax.core.tool.Tool
import com.kastrax.ai2db.nl2sql.agent.NL2SQLAgent
import com.kastrax.ai2db.nl2sql.service.SQLGenerationService
import com.kastrax.ai2db.nl2sql.tool.DatabaseSchemaTool
import com.kastrax.ai2db.nl2sql.tool.SQLKnowledgeBaseTool
import com.kastrax.ai2db.nl2sql.tool.SQLValidationTool
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * NL2SQL模块配置
 */
@Configuration
@EnableConfigurationProperties(NL2SQLProperties::class)
class NL2SQLConfiguration {
    
    /**
     * 配置NL2SQL Agent
     */
    @Bean
    @Primary
    fun nl2sqlAgent(
        memory: Memory,
        rag: RAG,
        tools: List<Tool>,
        sqlGenerationService: SQLGenerationService
    ): NL2SQLAgent {
        return NL2SQLAgent(
            memory = memory,
            rag = rag,
            tools = tools,
            sqlGenerationService = sqlGenerationService
        )
    }
    
    /**
     * 配置NL2SQL工具列表
     */
    @Bean
    fun nl2sqlTools(
        databaseSchemaTool: DatabaseSchemaTool,
        sqlKnowledgeBaseTool: SQLKnowledgeBaseTool,
        sqlValidationTool: SQLValidationTool
    ): List<Tool> {
        return listOf(
            databaseSchemaTool,
            sqlKnowledgeBaseTool,
            sqlValidationTool
        )
    }
}

/**
 * NL2SQL配置属性
 */
@ConfigurationProperties(prefix = "kastrax.nl2sql")
data class NL2SQLProperties(
    val memory: NL2SQLMemoryConfig = NL2SQLMemoryConfig(),
    val rag: NL2SQLRagConfig = NL2SQLRagConfig(),
    val agent: NL2SQLAgentConfig = NL2SQLAgentConfig(),
    val validation: NL2SQLValidationConfig = NL2SQLValidationConfig()
)

/**
 * NL2SQL Memory配置
 */
data class NL2SQLMemoryConfig(
    val enabled: Boolean = true,
    val maxConversationHistory: Int = 10,
    val ttlHours: Long = 24,
    val persistenceEnabled: Boolean = true,
    val compressionEnabled: Boolean = false,
    val indexingEnabled: Boolean = true
)

/**
 * NL2SQL RAG配置
 */
data class NL2SQLRagConfig(
    val enabled: Boolean = true,
    val maxResults: Int = 5,
    val useSemanticSearch: Boolean = true,
    val useHybridSearch: Boolean = true,
    val useReranking: Boolean = true,
    val similarityThreshold: Double = 0.7,
    val knowledgeBase: KnowledgeBaseConfig = KnowledgeBaseConfig()
)

/**
 * 知识库配置
 */
data class KnowledgeBaseConfig(
    val sqlExamplesPath: String = "classpath:knowledge/sql-examples.json",
    val bestPracticesPath: String = "classpath:knowledge/sql-best-practices.json",
    val commonPatternsPath: String = "classpath:knowledge/sql-patterns.json",
    val autoUpdateEnabled: Boolean = true,
    val updateIntervalHours: Long = 24
)

/**
 * NL2SQL Agent配置
 */
data class NL2SQLAgentConfig(
    val enabled: Boolean = true,
    val version: String = "1.0.0",
    val maxRetries: Int = 3,
    val timeoutSeconds: Long = 30,
    val confidenceThreshold: Double = 0.6,
    val enableExplanation: Boolean = true,
    val enableOptimization: Boolean = false
)

/**
 * SQL验证配置
 */
data class NL2SQLValidationConfig(
    val enabled: Boolean = true,
    val strictMode: Boolean = true,
    val checkSyntax: Boolean = true,
    val checkSecurity: Boolean = true,
    val checkPerformance: Boolean = false,
    val allowedOperations: Set<String> = setOf("SELECT", "INSERT", "UPDATE"),
    val blockedKeywords: Set<String> = setOf("DROP", "TRUNCATE", "SHUTDOWN", "KILL")
)