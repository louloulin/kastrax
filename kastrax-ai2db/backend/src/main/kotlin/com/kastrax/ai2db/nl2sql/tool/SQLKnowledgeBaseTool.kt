package com.kastrax.ai2db.nl2sql.tool

import ai.kastrax.core.tool.Tool
import ai.kastrax.core.tool.ToolExecuteResult
import ai.kastrax.core.tool.ToolContext
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

/**
 * SQL知识库工具
 * 
 * 基于RAG系统检索SQL相关的知识和示例
 */
@Component
class SQLKnowledgeBaseTool(
    private val rag: RAG
) : Tool {
    
    private val logger = LoggerFactory.getLogger(SQLKnowledgeBaseTool::class.java)
    
    override val id: String = "sql-knowledge-base"
    override val name: String = "SQL知识库工具"
    override val description: String = "从知识库中检索SQL相关的知识、示例和最佳实践"
    override val version: String = "1.0.0"
    
    override suspend fun execute(
        input: Map<String, Any>,
        context: ToolContext?
    ): ToolExecuteResult {
        logger.info("Executing SQLKnowledgeBaseTool with input: {}", input)
        
        return try {
            val query = input["query"] as? String
                ?: throw IllegalArgumentException("Missing 'query' parameter")
            
            val maxResults = input["maxResults"] as? Int ?: 5
            val useSemanticSearch = input["useSemanticSearch"] as? Boolean ?: true
            val useReranking = input["useReranking"] as? Boolean ?: true
            val databaseType = input["databaseType"] as? String
            
            // 构建增强查询
            val enhancedQuery = buildEnhancedQuery(query, databaseType)
            
            // 使用RAG检索相关文档
            val ragResult = rag.retrieveContext(
                query = enhancedQuery,
                options = RagProcessOptions(
                    maxResults = maxResults,
                    useHybridSearch = true,
                    useSemanticRetrieval = useSemanticSearch,
                    useReranking = useReranking,
                    metadata = mapOf(
                        "domain" to "sql",
                        "databaseType" to (databaseType ?: "general")
                    )
                )
            )
            
            // 处理检索结果
            val knowledgeItems = ragResult.documents.map { doc ->
                mapOf(
                    "content" to doc.content,
                    "score" to doc.score,
                    "metadata" to doc.metadata,
                    "source" to (doc.metadata["source"] ?: "unknown")
                )
            }
            
            // 分类知识项
            val categorizedKnowledge = categorizeKnowledge(knowledgeItems)
            
            ToolExecuteResult(
                success = true,
                output = mapOf(
                    "knowledgeItems" to knowledgeItems,
                    "categorized" to categorizedKnowledge,
                    "totalResults" to knowledgeItems.size,
                    "query" to enhancedQuery,
                    "summary" to generateSummary(knowledgeItems)
                ),
                metadata = mapOf(
                    "originalQuery" to query,
                    "enhancedQuery" to enhancedQuery,
                    "retrievalTime" to System.currentTimeMillis(),
                    "useSemanticSearch" to useSemanticSearch,
                    "useReranking" to useReranking
                )
            )
        } catch (e: Exception) {
            logger.error("Error executing SQLKnowledgeBaseTool", e)
            ToolExecuteResult(
                success = false,
                output = mapOf("error" to e.message),
                metadata = mapOf("errorType" to e.javaClass.simpleName)
            )
        }
    }
    
    /**
     * 构建增强查询
     */
    private fun buildEnhancedQuery(query: String, databaseType: String?): String {
        val queryBuilder = StringBuilder()
        
        // 添加SQL相关的上下文
        queryBuilder.append("SQL查询: ")
        queryBuilder.append(query)
        
        // 如果指定了数据库类型，添加到查询中
        if (!databaseType.isNullOrBlank()) {
            queryBuilder.append(" 数据库类型: $databaseType")
        }
        
        // 添加相关的关键词以提高检索精度
        val keywords = extractKeywords(query)
        if (keywords.isNotEmpty()) {
            queryBuilder.append(" 关键词: ${keywords.joinToString(", ")}")
        }
        
        return queryBuilder.toString()
    }
    
    /**
     * 提取查询中的关键词
     */
    private fun extractKeywords(query: String): List<String> {
        val keywords = mutableListOf<String>()
        val lowerQuery = query.lowercase()
        
        // SQL操作关键词
        val sqlOperations = listOf(
            "select", "insert", "update", "delete", "create", "drop", "alter",
            "join", "inner join", "left join", "right join", "full join",
            "group by", "order by", "having", "where", "union", "subquery"
        )
        
        sqlOperations.forEach { operation ->
            if (lowerQuery.contains(operation)) {
                keywords.add(operation)
            }
        }
        
        // 数据类型关键词
        val dataTypes = listOf(
            "varchar", "int", "integer", "decimal", "date", "datetime", "timestamp",
            "text", "blob", "json", "boolean", "float", "double"
        )
        
        dataTypes.forEach { type ->
            if (lowerQuery.contains(type)) {
                keywords.add(type)
            }
        }
        
        // 业务关键词（简单的名词提取）
        val businessKeywords = query.split("\\s+")
            .filter { it.length > 3 && !it.matches(".*[0-9].*".toRegex()) }
            .take(5)
        
        keywords.addAll(businessKeywords)
        
        return keywords.distinct()
    }
    
    /**
     * 对知识项进行分类
     */
    private fun categorizeKnowledge(knowledgeItems: List<Map<String, Any>>): Map<String, List<Map<String, Any>>> {
        val categories = mutableMapOf<String, MutableList<Map<String, Any>>>()
        
        knowledgeItems.forEach { item ->
            val content = item["content"] as? String ?: ""
            val lowerContent = content.lowercase()
            
            val category = when {
                lowerContent.contains("example") || lowerContent.contains("示例") -> "examples"
                lowerContent.contains("best practice") || lowerContent.contains("最佳实践") -> "best_practices"
                lowerContent.contains("syntax") || lowerContent.contains("语法") -> "syntax"
                lowerContent.contains("performance") || lowerContent.contains("性能") -> "performance"
                lowerContent.contains("error") || lowerContent.contains("错误") -> "troubleshooting"
                else -> "general"
            }
            
            categories.getOrPut(category) { mutableListOf() }.add(item)
        }
        
        return categories
    }
    
    /**
     * 生成知识摘要
     */
    private fun generateSummary(knowledgeItems: List<Map<String, Any>>): String {
        if (knowledgeItems.isEmpty()) {
            return "未找到相关的SQL知识。"
        }
        
        val summaryBuilder = StringBuilder()
        summaryBuilder.append("找到 ${knowledgeItems.size} 条相关的SQL知识：\n")
        
        // 取前3个最相关的项目生成摘要
        knowledgeItems.take(3).forEachIndexed { index, item ->
            val content = item["content"] as? String ?: ""
            val score = item["score"] as? Double ?: 0.0
            
            summaryBuilder.append("${index + 1}. ")
            summaryBuilder.append(content.take(100))
            if (content.length > 100) {
                summaryBuilder.append("...")
            }
            summaryBuilder.append(" (相关度: ${String.format("%.2f", score)})\n")
        }
        
        if (knowledgeItems.size > 3) {
            summaryBuilder.append("还有 ${knowledgeItems.size - 3} 条其他相关知识。")
        }
        
        return summaryBuilder.toString()
    }
}