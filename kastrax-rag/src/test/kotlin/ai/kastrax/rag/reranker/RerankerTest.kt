package ai.kastrax.rag.reranker

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.vectorstore.RagDocument
import ai.kastrax.rag.vectorstore.SearchResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RerankerTest {

    @Test
    fun `test identity reranker`() = runBlocking {
        // 创建测试数据
        val results = listOf(
            SearchResult(RagDocument("1", "文档1", mapOf("score" to "0.9")), 0.9),
            SearchResult(RagDocument("2", "文档2", mapOf("score" to "0.8")), 0.8),
            SearchResult(RagDocument("3", "文档3", mapOf("score" to "0.7")), 0.7)
        )

        // 创建重排序器
        val reranker = IdentityReranker()

        // 执行重排序
        val reranked = reranker.rerank("测试查询", results)

        // 验证结果
        assertEquals(results.size, reranked.size)
        assertEquals(results[0], reranked[0])
        assertEquals(results[1], reranked[1])
        assertEquals(results[2], reranked[2])
    }

    @Test
    fun `test keyword match reranker`() = runBlocking {
        // 创建测试数据
        val results = listOf(
            SearchResult(RagDocument("1", "这是一个关于人工智能的文档", mapOf()), 0.8),
            SearchResult(RagDocument("2", "这个文档包含人工智能和机器学习的内容", mapOf()), 0.7),
            SearchResult(RagDocument("3", "这是一个关于数据库的文档", mapOf()), 0.9)
        )

        // 创建重排序器
        val reranker = KeywordMatchReranker(keywordWeight = 0.7, originalScoreWeight = 0.3)

        // 执行重排序
        val reranked = reranker.rerank("人工智能 机器学习", results)

        // 验证结果
        assertEquals(results.size, reranked.size)

        // 包含两个关键词的文档应该排在最前面
        assertEquals("这个文档包含人工智能和机器学习的内容", reranked[0].document.content)

        // 包含一个关键词的文档应该排在第二位
        assertEquals("这是一个关于人工智能的文档", reranked[1].document.content)

        // 不包含关键词的文档应该排在最后
        assertEquals("这是一个关于数据库的文档", reranked[2].document.content)
    }

    @Test
    fun `test metadata reranker`() = runBlocking {
        // 创建测试数据
        val results = listOf(
            SearchResult(RagDocument("1", "文档1", mapOf("date" to "20220101", "relevance" to "0.5")), 0.7),
            SearchResult(RagDocument("2", "文档2", mapOf("date" to "20230101", "relevance" to "0.8")), 0.8),
            SearchResult(RagDocument("3", "文档3", mapOf("date" to "20210101", "relevance" to "0.9")), 0.9)
        )

        // 创建按日期降序排序的重排序器
        val dateReranker = MetadataReranker(
            metadataKey = "date",
            ascending = false,
            metadataWeight = 0.8,
            originalScoreWeight = 0.2
        )

        // 执行重排序
        val rerankedByDate = dateReranker.rerank("测试查询", results)

        // 打印重排序结果以便调试
        println("Reranked by date: ${rerankedByDate.map { it.document.content }}")
        println("Original dates: ${results.map { it.document.metadata["date"] }}")

        // 验证结果
        assertEquals(results.size, rerankedByDate.size)

        // 验证最新的文档排在前面
        val contents = rerankedByDate.map { it.document.content }
        val dates = rerankedByDate.map { it.document.metadata["date"]?.toInt() ?: 0 }

        // 验证日期是降序排序的
        assertTrue(dates[0] >= dates[1] && dates[1] >= dates[2], "日期应该是降序排序的")

        // 验证最新的文档在前面
        assertEquals(20230101, dates[0], "最新的文档应该在前面")

        // 创建按相关性降序排序的重排序器
        val relevanceReranker = MetadataReranker(
            metadataKey = "relevance",
            ascending = false,
            metadataWeight = 0.9,
            originalScoreWeight = 0.1
        )

        // 执行重排序
        val rerankedByRelevance = relevanceReranker.rerank("测试查询", results)

        // 打印重排序结果以便调试
        println("Reranked by relevance: ${rerankedByRelevance.map { it.document.content }}")
        println("Original relevance: ${results.map { it.document.metadata["relevance"] }}")

        // 验证结果
        assertEquals(results.size, rerankedByRelevance.size)

        // 验证相关性最高的文档排在前面
        val relevanceValues = rerankedByRelevance.map { it.document.metadata["relevance"]?.toDouble() ?: 0.0 }

        // 验证相关性是降序排序的
        assertTrue(relevanceValues[0] >= relevanceValues[1] && relevanceValues[1] >= relevanceValues[2], "相关性应该是降序排序的")

        // 验证相关性最高的文档在前面
        assertEquals(0.9, relevanceValues[0], "相关性最高的文档应该在前面")
    }

    @Test
    fun `test composite reranker`() = runBlocking {
        // 创建测试数据
        val results = listOf(
            SearchResult(RagDocument("1", "这是一个关于人工智能的旧文档", mapOf("date" to "20210101")), 0.8),
            SearchResult(RagDocument("2", "这个文档包含人工智能和机器学习的新内容", mapOf("date" to "20230101")), 0.7),
            SearchResult(RagDocument("3", "这是一个关于数据库的新文档", mapOf("date" to "20230201")), 0.9)
        )

        // 创建组合重排序器：先按关键词匹配，再按日期排序
        val compositeReranker = CompositeReranker(
            KeywordMatchReranker(keywordWeight = 0.7, originalScoreWeight = 0.3),
            MetadataReranker(metadataKey = "date", ascending = false)
        )

        // 执行重排序
        val reranked = compositeReranker.rerank("人工智能 机器学习", results)

        // 验证结果
        assertEquals(results.size, reranked.size)

        // 包含两个关键词的最新文档应该排在最前面
        assertEquals("这个文档包含人工智能和机器学习的新内容", reranked[0].document.content)

        // 包含一个关键词的文档应该排在第二位
        assertEquals("这是一个关于人工智能的旧文档", reranked[1].document.content)

        // 不包含关键词的文档应该排在最后
        assertEquals("这是一个关于数据库的新文档", reranked[2].document.content)
    }
}
