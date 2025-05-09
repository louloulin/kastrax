package ai.kastrax.store.retriever

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.retriever.Retriever
import ai.kastrax.rag.vectorstore.RagDocument
import ai.kastrax.store.VectorStore
import ai.kastrax.store.adapter.RagVectorStoreAdapter
import ai.kastrax.store.hybrid.HybridSearch
import ai.kastrax.store.hybrid.HybridSearchOptions
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 混合检索器，使用混合搜索来检索文档。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property options 混合搜索选项
 * @property maxKeywords 最大关键词数量
 * @property minKeywordLength 最小关键词长度
 * @property defaultLimit 默认返回结果的最大数量
 */
class HybridRetriever(
    private val vectorStore: VectorStore,
    private val embeddingService: EmbeddingService,
    private val options: HybridSearchOptions = HybridSearchOptions(),
    private val maxKeywords: Int = 5,
    private val minKeywordLength: Int = 3,
    private val defaultLimit: Int = 5
) : Retriever {

    /**
     * 检索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @return 文档列表
     */
    override suspend fun retrieve(query: String, limit: Int): List<RagDocument> {
        val actualLimit = if (limit <= 0) defaultLimit else limit

        // 从查询中提取关键词
        val keywords = HybridSearch.extractKeywords(
            query = query,
            minLength = minKeywordLength,
            maxKeywords = maxKeywords
        )

        logger.debug { "Extracted keywords from query: $keywords" }

        // 执行混合搜索
        val results = HybridSearch.search(
            query = query,
            vectorStore = vectorStore,
            embeddingService = embeddingService,
            keywords = keywords,
            limit = actualLimit,
            options = options
        )

        logger.debug { "Hybrid search returned ${results.size} results" }

        // 返回文档
        return results.map { it.document }
    }
}

/**
 * 混合检索器工厂类。
 */
object HybridRetrieverFactory {

    /**
     * 创建混合检索器。
     *
     * @param vectorStore 向量存储
     * @param embeddingService 嵌入服务
     * @param options 混合搜索选项
     * @param maxKeywords 最大关键词数量
     * @param minKeywordLength 最小关键词长度
     * @param defaultLimit 默认返回结果的最大数量
     * @return 混合检索器
     */
    fun create(
        vectorStore: VectorStore,
        embeddingService: EmbeddingService,
        options: HybridSearchOptions = HybridSearchOptions(),
        maxKeywords: Int = 5,
        minKeywordLength: Int = 3,
        defaultLimit: Int = 5
    ): HybridRetriever {
        return HybridRetriever(
            vectorStore = vectorStore,
            embeddingService = embeddingService,
            options = options,
            maxKeywords = maxKeywords,
            minKeywordLength = minKeywordLength,
            defaultLimit = defaultLimit
        )
    }

    /**
     * 创建混合检索器。
     *
     * @param ragVectorStore RAG 向量存储
     * @param embeddingService 嵌入服务
     * @param options 混合搜索选项
     * @param maxKeywords 最大关键词数量
     * @param minKeywordLength 最小关键词长度
     * @param defaultLimit 默认返回结果的最大数量
     * @return 混合检索器
     */
    fun create(
        ragVectorStore: RagVectorStoreAdapter,
        embeddingService: EmbeddingService,
        options: HybridSearchOptions = HybridSearchOptions(),
        maxKeywords: Int = 5,
        minKeywordLength: Int = 3,
        defaultLimit: Int = 5
    ): Retriever {
        // 创建一个适配器，将 Retriever 接口适配为 HybridRetriever
        return object : Retriever {
            override suspend fun retrieve(query: String, limit: Int): List<RagDocument> {
                val actualLimit = if (limit <= 0) defaultLimit else limit

                // 从查询中提取关键词
                val keywords = HybridSearch.extractKeywords(
                    query = query,
                    minLength = minKeywordLength,
                    maxKeywords = maxKeywords
                )

                logger.debug { "Extracted keywords from query: $keywords" }

                // 执行混合搜索
                val results = HybridSearch.search(
                    query = query,
                    vectorStore = ragVectorStore,
                    embeddingService = embeddingService,
                    keywords = keywords,
                    limit = actualLimit,
                    options = options
                )

                logger.debug { "Hybrid search returned ${results.size} results" }

                // 返回文档
                return results.map { it.document }
            }
        }
    }
}
