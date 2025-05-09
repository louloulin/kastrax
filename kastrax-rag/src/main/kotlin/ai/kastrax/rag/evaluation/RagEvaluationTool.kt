package ai.kastrax.rag.evaluation

import ai.kastrax.rag.RAG
import ai.kastrax.store.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

/**
 * LLM 客户端接口，定义了与 LLM 交互的方法。
 */
interface LlmClient {
    /**
     * 生成文本。
     *
     * @param prompt 提示文本
     * @param temperature 温度
     * @param maxTokens 最大令牌数
     * @return 生成的文本
     */
    suspend fun generateText(
        prompt: String,
        temperature: Double = 0.7,
        maxTokens: Int = 1000
    ): String
}

/**
 * RAG 评估工具，用于评估 RAG 系统的性能。
 *
 * @property rag RAG 实例
 * @property llmClient LLM 客户端
 */
class RagEvaluationTool(
    private val rag: RAG,
    private val llmClient: LlmClient
) {
    private val json = Json { prettyPrint = true }
    
    /**
     * 评估检索精度。
     *
     * @param query 查询文本
     * @param relevantDocIds 相关文档 ID 列表
     * @param limit 返回结果的最大数量
     * @return 精度分数
     */
    suspend fun evaluateRetrievalPrecision(
        query: String,
        relevantDocIds: List<String>,
        limit: Int = 5
    ): Double = withContext(Dispatchers.IO) {
        try {
            if (relevantDocIds.isEmpty()) {
                return@withContext 0.0
            }
            
            // 搜索文档
            val results = rag.search(query, limit)
            
            if (results.isEmpty()) {
                return@withContext 0.0
            }
            
            // 计算精度：检索到的相关文档数 / 检索到的文档总数
            val retrievedIds = results.map { it.document.id }
            val relevantRetrieved = retrievedIds.count { it in relevantDocIds }
            
            return@withContext relevantRetrieved.toDouble() / retrievedIds.size
        } catch (e: Exception) {
            logger.error(e) { "Error evaluating retrieval precision" }
            return@withContext 0.0
        }
    }
    
    /**
     * 评估上下文相关性。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @return 相关性分数
     */
    suspend fun evaluateContextRelevance(
        query: String,
        limit: Int = 5
    ): Double = withContext(Dispatchers.IO) {
        try {
            // 生成上下文
            val context = rag.generateContext(query, limit)
            
            if (context.isBlank()) {
                return@withContext 0.0
            }
            
            // 使用 LLM 评估相关性
            val prompt = """
                You are an AI assistant tasked with evaluating the relevance of a context to a query.
                
                Query: $query
                
                Context:
                $context
                
                On a scale of 0 to 10, how relevant is the context to the query?
                Provide only a number as your answer, where 0 means completely irrelevant and 10 means perfectly relevant.
            """.trimIndent()
            
            val response = llmClient.generateText(prompt, temperature = 0.1)
            
            // 解析分数
            val score = response.trim().toDoubleOrNull() ?: 0.0
            
            // 归一化分数到 0-1 范围
            return@withContext score / 10.0
        } catch (e: Exception) {
            logger.error(e) { "Error evaluating context relevance" }
            return@withContext 0.0
        }
    }
    
    /**
     * 评估答案质量。
     *
     * @param query 查询文本
     * @param expectedAnswer 期望的答案
     * @param limit 返回结果的最大数量
     * @return 质量分数
     */
    suspend fun evaluateAnswerQuality(
        query: String,
        expectedAnswer: String,
        limit: Int = 5
    ): Double = withContext(Dispatchers.IO) {
        try {
            // 生成上下文
            val context = rag.generateContext(query, limit)
            
            if (context.isBlank()) {
                return@withContext 0.0
            }
            
            // 使用 LLM 生成答案
            val answerPrompt = """
                You are an AI assistant tasked with answering a question based on the provided context.
                
                Context:
                $context
                
                Question: $query
                
                Provide a concise answer to the question based only on the information in the context.
            """.trimIndent()
            
            val generatedAnswer = llmClient.generateText(answerPrompt, temperature = 0.3)
            
            // 使用 LLM 评估答案质量
            val evaluationPrompt = """
                You are an AI assistant tasked with evaluating the quality of an answer compared to an expected answer.
                
                Question: $query
                
                Generated Answer:
                $generatedAnswer
                
                Expected Answer:
                $expectedAnswer
                
                On a scale of 0 to 10, how well does the generated answer match the expected answer in terms of correctness and completeness?
                Provide only a number as your answer, where 0 means completely incorrect/incomplete and 10 means perfectly correct/complete.
            """.trimIndent()
            
            val response = llmClient.generateText(evaluationPrompt, temperature = 0.1)
            
            // 解析分数
            val score = response.trim().toDoubleOrNull() ?: 0.0
            
            // 归一化分数到 0-1 范围
            return@withContext score / 10.0
        } catch (e: Exception) {
            logger.error(e) { "Error evaluating answer quality" }
            return@withContext 0.0
        }
    }
    
    /**
     * 评估幻觉。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @return 幻觉分数（越低越好）
     */
    suspend fun evaluateHallucination(
        query: String,
        limit: Int = 5
    ): Double = withContext(Dispatchers.IO) {
        try {
            // 检索上下文
            val result = rag.retrieveContext(query, limit)
            
            if (result.context.isBlank() || result.documents.isEmpty()) {
                return@withContext 1.0 // 最高幻觉分数
            }
            
            // 使用 LLM 生成答案
            val answerPrompt = """
                You are an AI assistant tasked with answering a question based on the provided context.
                
                Context:
                ${result.context}
                
                Question: $query
                
                Provide a concise answer to the question based only on the information in the context.
            """.trimIndent()
            
            val generatedAnswer = llmClient.generateText(answerPrompt, temperature = 0.3)
            
            // 创建评估数据
            val evaluationData = HallucinationEvaluationData(
                query = query,
                context = result.documents.map { it.content },
                answer = generatedAnswer
            )
            
            // 使用 LLM 评估幻觉
            val evaluationPrompt = """
                You are an AI assistant tasked with evaluating whether an answer contains hallucinations (information not present in the context).
                
                Evaluation Data:
                ${json.encodeToString(evaluationData)}
                
                Analyze the answer and determine if it contains information not present in the context.
                On a scale of 0 to 10, how much hallucination is present in the answer?
                Provide only a number as your answer, where 0 means no hallucination and 10 means severe hallucination.
            """.trimIndent()
            
            val response = llmClient.generateText(evaluationPrompt, temperature = 0.1)
            
            // 解析分数
            val score = response.trim().toDoubleOrNull() ?: 5.0
            
            // 归一化分数到 0-1 范围
            return@withContext score / 10.0
        } catch (e: Exception) {
            logger.error(e) { "Error evaluating hallucination" }
            return@withContext 0.5 // 中等幻觉分数
        }
    }
    
    /**
     * 运行完整评估。
     *
     * @param query 查询文本
     * @param expectedAnswer 期望的答案
     * @param relevantDocIds 相关文档 ID 列表
     * @param limit 返回结果的最大数量
     * @return 评估结果
     */
    suspend fun runFullEvaluation(
        query: String,
        expectedAnswer: String,
        relevantDocIds: List<String>,
        limit: Int = 5
    ): RagEvaluationResult = withContext(Dispatchers.IO) {
        try {
            // 并行运行所有评估
            val retrievalPrecision = evaluateRetrievalPrecision(query, relevantDocIds, limit)
            val contextRelevance = evaluateContextRelevance(query, limit)
            val answerQuality = evaluateAnswerQuality(query, expectedAnswer, limit)
            val hallucination = evaluateHallucination(query, limit)
            
            // 计算总分
            val totalScore = (retrievalPrecision + contextRelevance + answerQuality + (1.0 - hallucination)) / 4.0
            
            return@withContext RagEvaluationResult(
                query = query,
                retrievalPrecision = retrievalPrecision,
                contextRelevance = contextRelevance,
                answerQuality = answerQuality,
                hallucination = hallucination,
                totalScore = totalScore
            )
        } catch (e: Exception) {
            logger.error(e) { "Error running full evaluation" }
            return@withContext RagEvaluationResult(
                query = query,
                retrievalPrecision = 0.0,
                contextRelevance = 0.0,
                answerQuality = 0.0,
                hallucination = 1.0,
                totalScore = 0.0
            )
        }
    }
}

/**
 * 幻觉评估数据。
 *
 * @property query 查询文本
 * @property context 上下文
 * @property answer 答案
 */
@Serializable
data class HallucinationEvaluationData(
    val query: String,
    val context: List<String>,
    val answer: String
)

/**
 * RAG 评估结果。
 *
 * @property query 查询文本
 * @property retrievalPrecision 检索精度
 * @property contextRelevance 上下文相关性
 * @property answerQuality 答案质量
 * @property hallucination 幻觉分数
 * @property totalScore 总分
 */
data class RagEvaluationResult(
    val query: String,
    val retrievalPrecision: Double,
    val contextRelevance: Double,
    val answerQuality: Double,
    val hallucination: Double,
    val totalScore: Double
)
