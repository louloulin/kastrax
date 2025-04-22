package ai.kastrax.rag.retrieval

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.query.QueryTransformer
import ai.kastrax.rag.vectorstore.RagDocument
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.SearchResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryEnhancedRetrieverTest {
    
    @Test
    fun testSingleQueryRetrieval() = runBlocking {
        // 创建模拟查询转换器
        val queryTransformer = object : QueryTransformer {
            override suspend fun transform(query: String): String {
                return "transformed $query"
            }
        }
        
        // 创建模拟检索器
        val mockRetriever = object : Retriever {
            override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<SearchResult> {
                return if (query.startsWith("transformed")) {
                    // 如果是转换后的查询，返回高分结果
                    listOf(
                        SearchResult(RagDocument("1", "Document 1", mapOf()), 0.9),
                        SearchResult(RagDocument("2", "Document 2", mapOf()), 0.8)
                    )
                } else {
                    // 如果是原始查询，返回低分结果
                    listOf(
                        SearchResult(RagDocument("3", "Document 3", mapOf()), 0.7),
                        SearchResult(RagDocument("4", "Document 4", mapOf()), 0.6)
                    )
                }
            }
        }
        
        // 创建查询增强检索器
        val retriever = QueryEnhancedRetriever(
            baseRetriever = mockRetriever,
            queryTransformer = queryTransformer,
            config = QueryEnhancedRetrieverConfig(useMultiQuery = false)
        )
        
        // 测试检索
        val results = retriever.retrieve("test query", 2, 0.0)
        
        // 验证结果
        assertEquals(2, results.size)
        assertEquals("1", results[0].document.id)
        assertEquals("2", results[1].document.id)
        assertEquals(0.9, results[0].score)
        assertEquals(0.8, results[1].score)
    }
    
    @Test
    fun testMultiQueryRetrieval() = runBlocking {
        // 创建模拟查询转换器
        val queryTransformer = object : QueryTransformer {
            override suspend fun transform(query: String): String {
                return query
            }
            
            override suspend fun transformToMultiple(query: String): List<String> {
                return listOf(
                    query,
                    "variant 1 of $query",
                    "variant 2 of $query"
                )
            }
        }
        
        // 创建模拟检索器
        val mockRetriever = object : Retriever {
            override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<SearchResult> {
                return when {
                    query.contains("variant 1") -> {
                        listOf(
                            SearchResult(RagDocument("1", "Document 1", mapOf()), 0.9),
                            SearchResult(RagDocument("2", "Document 2", mapOf()), 0.8)
                        )
                    }
                    query.contains("variant 2") -> {
                        listOf(
                            SearchResult(RagDocument("3", "Document 3", mapOf()), 0.85),
                            SearchResult(RagDocument("4", "Document 4", mapOf()), 0.75)
                        )
                    }
                    else -> {
                        listOf(
                            SearchResult(RagDocument("5", "Document 5", mapOf()), 0.7),
                            SearchResult(RagDocument("6", "Document 6", mapOf()), 0.6)
                        )
                    }
                }
            }
        }
        
        // 创建查询增强检索器，使用交错合并策略
        val retriever = QueryEnhancedRetriever(
            baseRetriever = mockRetriever,
            queryTransformer = queryTransformer,
            config = QueryEnhancedRetrieverConfig(
                useMultiQuery = true,
                mergeStrategy = MergeStrategy.INTERLEAVE
            )
        )
        
        // 测试检索
        val results = retriever.retrieve("test query", 4, 0.0)
        
        // 验证结果
        assertEquals(4, results.size)
        
        // 验证交错合并的结果
        val docIds = results.map { it.document.id }
        assertTrue(docIds.contains("1"))
        assertTrue(docIds.contains("3"))
        assertTrue(docIds.contains("5"))
    }
    
    @Test
    fun testMergeByScore() = runBlocking {
        // 创建模拟查询转换器
        val queryTransformer = object : QueryTransformer {
            override suspend fun transform(query: String): String {
                return query
            }
            
            override suspend fun transformToMultiple(query: String): List<String> {
                return listOf(
                    query,
                    "variant 1 of $query",
                    "variant 2 of $query"
                )
            }
        }
        
        // 创建模拟检索器
        val mockRetriever = object : Retriever {
            override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<SearchResult> {
                return when {
                    query.contains("variant 1") -> {
                        listOf(
                            SearchResult(RagDocument("1", "Document 1", mapOf()), 0.9),
                            SearchResult(RagDocument("2", "Document 2", mapOf()), 0.8)
                        )
                    }
                    query.contains("variant 2") -> {
                        listOf(
                            SearchResult(RagDocument("3", "Document 3", mapOf()), 0.85),
                            SearchResult(RagDocument("4", "Document 4", mapOf()), 0.75)
                        )
                    }
                    else -> {
                        listOf(
                            SearchResult(RagDocument("5", "Document 5", mapOf()), 0.7),
                            SearchResult(RagDocument("6", "Document 6", mapOf()), 0.6)
                        )
                    }
                }
            }
        }
        
        // 创建查询增强检索器，使用按分数合并策略
        val retriever = QueryEnhancedRetriever(
            baseRetriever = mockRetriever,
            queryTransformer = queryTransformer,
            config = QueryEnhancedRetrieverConfig(
                useMultiQuery = true,
                mergeStrategy = MergeStrategy.BY_SCORE
            )
        )
        
        // 测试检索
        val results = retriever.retrieve("test query", 4, 0.0)
        
        // 验证结果
        assertEquals(4, results.size)
        
        // 验证按分数排序的结果
        assertEquals("1", results[0].document.id)
        assertEquals("3", results[1].document.id)
        assertEquals("2", results[2].document.id)
        assertEquals("4", results[3].document.id)
        
        // 验证分数降序排列
        assertTrue(results[0].score >= results[1].score)
        assertTrue(results[1].score >= results[2].score)
        assertTrue(results[2].score >= results[3].score)
    }
    
    @Test
    fun testMergeByDiversity() = runBlocking {
        // 创建模拟查询转换器
        val queryTransformer = object : QueryTransformer {
            override suspend fun transform(query: String): String {
                return query
            }
            
            override suspend fun transformToMultiple(query: String): List<String> {
                return listOf(
                    query,
                    "variant 1 of $query",
                    "variant 2 of $query"
                )
            }
        }
        
        // 创建模拟检索器
        val mockRetriever = object : Retriever {
            override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<SearchResult> {
                return when {
                    query.contains("variant 1") -> {
                        listOf(
                            SearchResult(RagDocument("1", "Document 1", mapOf()), 0.9),
                            SearchResult(RagDocument("2", "Document 2", mapOf()), 0.8)
                        )
                    }
                    query.contains("variant 2") -> {
                        listOf(
                            SearchResult(RagDocument("3", "Document 3", mapOf()), 0.85),
                            SearchResult(RagDocument("4", "Document 4", mapOf()), 0.75)
                        )
                    }
                    else -> {
                        listOf(
                            SearchResult(RagDocument("5", "Document 5", mapOf()), 0.7),
                            SearchResult(RagDocument("6", "Document 6", mapOf()), 0.6)
                        )
                    }
                }
            }
        }
        
        // 创建查询增强检索器，使用多样性合并策略
        val retriever = QueryEnhancedRetriever(
            baseRetriever = mockRetriever,
            queryTransformer = queryTransformer,
            config = QueryEnhancedRetrieverConfig(
                useMultiQuery = true,
                mergeStrategy = MergeStrategy.DIVERSITY
            )
        )
        
        // 测试检索
        val results = retriever.retrieve("test query", 3, 0.0)
        
        // 验证结果
        assertEquals(3, results.size)
        
        // 验证多样性合并的结果（每个查询的最佳结果应该被包含）
        val docIds = results.map { it.document.id }
        assertTrue(docIds.contains("1"))
        assertTrue(docIds.contains("3"))
        assertTrue(docIds.contains("5"))
    }
}
