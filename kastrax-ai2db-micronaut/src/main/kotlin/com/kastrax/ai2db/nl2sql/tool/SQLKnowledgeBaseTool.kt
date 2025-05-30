package com.kastrax.ai2db.nl2sql.tool

import ai.kastrax.core.tool.ZodTool
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.zod.*
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * SQL查询输入
 */
data class SQLQueryInput(
    val query: String,
    val schema: String? = null,
    val context: String? = null
)

/**
 * SQL查询输出
 */
data class SQLQueryOutput(
    val examples: List<SQLExample>,
    val patterns: List<SQLPattern>,
    val suggestions: List<String>
)

/**
 * SQL示例
 */
data class SQLExample(
    val description: String,
    val sql: String,
    val explanation: String,
    val confidence: Double
)

/**
 * SQL模式
 */
data class SQLPattern(
    val name: String,
    val pattern: String,
    val usage: String,
    val examples: List<String>
)

/**
 * SQL知识库工具
 * 
 * 基于RAG系统检索相关的SQL示例、模式和最佳实践
 */
@Singleton
class SQLKnowledgeBaseTool(
    private val rag: RAG
) : ZodTool<SQLQueryInput, SQLQueryOutput> {
    
    private val logger = LoggerFactory.getLogger(SQLKnowledgeBaseTool::class.java)
    
    override val id = "sql-knowledge-base"
    override val name = "SQL知识库查询"
    override val description = "从SQL知识库中检索相关的SQL示例和最佳实践"
    
    override val inputSchema = objectInput {
        "query" to stringField("自然语言查询")
        "schema" to stringField("数据库模式信息", required = false)
        "context" to stringField("查询上下文", required = false)
    }
    
    override val outputSchema = objectOutput {
        "examples" to arrayField("相关SQL示例") {
            objectField {
                "description" to stringField("示例描述")
                "sql" to stringField("SQL语句")
                "explanation" to stringField("解释说明")
                "confidence" to numberField("置信度")
            }
        }
        "patterns" to arrayField("SQL模式") {
            objectField {
                "name" to stringField("模式名称")
                "pattern" to stringField("模式定义")
                "usage" to stringField("使用场景")
                "examples" to arrayField("示例") { stringField() }
            }
        }
        "suggestions" to arrayField("优化建议") { stringField() }
    }
    
    override suspend fun execute(input: SQLQueryInput): SQLQueryOutput = withContext(Dispatchers.IO) {
        logger.info("Searching SQL knowledge base for query: {}", input.query)
        
        try {
            // 构建增强查询
            val enhancedQuery = buildEnhancedQuery(input)
            
            // 使用RAG检索相关文档
            val ragResult = rag.retrieveContext(
                query = enhancedQuery,
                options = RagProcessOptions(
                    useHybridSearch = true,
                    useSemanticRetrieval = true,
                    useReranking = true,
                    hybridOptions = ai.kastrax.rag.HybridOptions(
                        vectorWeight = 0.7,
                        keywordWeight = 0.3
                    ),
                    rerankingOptions = ai.kastrax.rag.RerankingOptions(
                        useDiversity = true,
                        diversityWeight = 0.3
                    )
                )
            )
            
            logger.debug("RAG retrieved {} documents", ragResult.documents.size)
            
            // 提取SQL示例
            val examples = extractSQLExamples(ragResult)
            
            // 提取SQL模式
            val patterns = extractSQLPatterns(ragResult)
            
            // 生成优化建议
            val suggestions = generateSuggestions(ragResult, input)
            
            logger.info("Found {} examples, {} patterns, {} suggestions", 
                examples.size, patterns.size, suggestions.size)
            
            return@withContext SQLQueryOutput(
                examples = examples,
                patterns = patterns,
                suggestions = suggestions
            )
            
        } catch (e: Exception) {
            logger.error("Error searching SQL knowledge base", e)
            
            // 返回空结果而不是抛出异常
            return@withContext SQLQueryOutput(
                examples = emptyList(),
                patterns = emptyList(),
                suggestions = listOf("知识库检索失败，请检查查询条件")
            )
        }
    }
    
    /**
     * 构建增强查询
     */
    private fun buildEnhancedQuery(input: SQLQueryInput): String {
        val queryBuilder = StringBuilder(input.query)
        
        // 添加模式信息
        if (!input.schema.isNullOrBlank()) {
            queryBuilder.append(" schema: ${input.schema}")
        }
        
        // 添加上下文信息
        if (!input.context.isNullOrBlank()) {
            queryBuilder.append(" context: ${input.context}")
        }
        
        return queryBuilder.toString()
    }
    
    /**
     * 从RAG结果中提取SQL示例
     */
    private fun extractSQLExamples(ragResult: ai.kastrax.rag.RagResult): List<SQLExample> {
        val examples = mutableListOf<SQLExample>()
        
        ragResult.documents.forEach { document ->
            try {
                // 解析文档内容，提取SQL示例
                val content = document.content
                val sqlBlocks = extractSQLBlocks(content)
                
                sqlBlocks.forEach { sqlBlock ->
                    examples.add(SQLExample(
                        description = extractDescription(sqlBlock, content),
                        sql = sqlBlock.sql,
                        explanation = sqlBlock.explanation ?: "无说明",
                        confidence = document.score
                    ))
                }
                
            } catch (e: Exception) {
                logger.warn("Failed to extract SQL example from document", e)
            }
        }
        
        // 按置信度排序，取前10个
        return examples.sortedByDescending { it.confidence }.take(10)
    }
    
    /**
     * 从RAG结果中提取SQL模式
     */
    private fun extractSQLPatterns(ragResult: ai.kastrax.rag.RagResult): List<SQLPattern> {
        val patterns = mutableListOf<SQLPattern>()
        
        ragResult.documents.forEach { document ->
            try {
                val content = document.content
                
                // 检测常见SQL模式
                detectCommonPatterns(content).forEach { pattern ->
                    patterns.add(pattern)
                }
                
            } catch (e: Exception) {
                logger.warn("Failed to extract SQL pattern from document", e)
            }
        }
        
        // 去重并返回
        return patterns.distinctBy { it.name }
    }
    
    /**
     * 生成优化建议
     */
    private fun generateSuggestions(
        ragResult: ai.kastrax.rag.RagResult,
        input: SQLQueryInput
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        // 基于检索结果生成建议
        if (ragResult.documents.isNotEmpty()) {
            suggestions.add("建议参考相关示例进行SQL编写")
            
            // 分析查询类型并给出建议
            val queryLower = input.query.lowercase()
            when {
                queryLower.contains("join") -> {
                    suggestions.add("注意JOIN操作的性能，考虑使用适当的索引")
                }
                queryLower.contains("group by") -> {
                    suggestions.add("GROUP BY查询建议添加适当的HAVING条件")
                }
                queryLower.contains("order by") -> {
                    suggestions.add("ORDER BY操作建议在相关字段上创建索引")
                }
                queryLower.contains("count") || queryLower.contains("sum") -> {
                    suggestions.add("聚合查询建议考虑数据量大小，必要时使用分页")
                }
            }
        } else {
            suggestions.add("未找到相关示例，建议检查查询描述是否准确")
        }
        
        return suggestions
    }
    
    /**
     * 提取SQL代码块
     */
    private fun extractSQLBlocks(content: String): List<SQLBlock> {
        val blocks = mutableListOf<SQLBlock>()
        
        // 简单的SQL代码块提取逻辑
        val sqlPattern = Regex("```sql\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val matches = sqlPattern.findAll(content)
        
        matches.forEach { match ->
            val sql = match.groupValues[1].trim()
            if (sql.isNotBlank()) {
                blocks.add(SQLBlock(
                    sql = sql,
                    explanation = extractExplanationForSQL(content, match.range)
                ))
            }
        }
        
        return blocks
    }
    
    /**
     * 提取描述
     */
    private fun extractDescription(sqlBlock: SQLBlock, content: String): String {
        // 简单的描述提取逻辑
        val lines = content.split("\n")
        val sqlLine = lines.indexOfFirst { it.contains(sqlBlock.sql.take(20)) }
        
        if (sqlLine > 0) {
            // 查找SQL前面的描述行
            for (i in (sqlLine - 1) downTo 0) {
                val line = lines[i].trim()
                if (line.isNotBlank() && !line.startsWith("```")) {
                    return line
                }
            }
        }
        
        return "SQL示例"
    }
    
    /**
     * 提取SQL的解释说明
     */
    private fun extractExplanationForSQL(content: String, sqlRange: IntRange): String? {
        // 查找SQL代码块后的解释
        val afterSQL = content.substring(sqlRange.last + 1)
        val lines = afterSQL.split("\n").take(3)
        
        val explanation = lines.firstOrNull { line ->
            val trimmed = line.trim()
            trimmed.isNotBlank() && !trimmed.startsWith("```")
        }
        
        return explanation?.trim()
    }
    
    /**
     * 检测常见SQL模式
     */
    private fun detectCommonPatterns(content: String): List<SQLPattern> {
        val patterns = mutableListOf<SQLPattern>()
        val contentLower = content.lowercase()
        
        // 检测JOIN模式
        if (contentLower.contains("join")) {
            patterns.add(SQLPattern(
                name = "JOIN查询",
                pattern = "SELECT ... FROM table1 JOIN table2 ON condition",
                usage = "用于关联多个表的数据查询",
                examples = listOf("INNER JOIN", "LEFT JOIN", "RIGHT JOIN")
            ))
        }
        
        // 检测聚合模式
        if (contentLower.contains("group by")) {
            patterns.add(SQLPattern(
                name = "聚合查询",
                pattern = "SELECT column, AGG_FUNC(column) FROM table GROUP BY column",
                usage = "用于数据分组和聚合计算",
                examples = listOf("COUNT", "SUM", "AVG", "MAX", "MIN")
            ))
        }
        
        // 检测子查询模式
        if (contentLower.contains("select") && contentLower.count("select") > 1) {
            patterns.add(SQLPattern(
                name = "子查询",
                pattern = "SELECT ... FROM (SELECT ... FROM table) AS subquery",
                usage = "用于复杂的嵌套查询",
                examples = listOf("EXISTS", "IN", "NOT IN")
            ))
        }
        
        return patterns
    }
}

/**
 * SQL代码块
 */
data class SQLBlock(
    val sql: String,
    val explanation: String?
)