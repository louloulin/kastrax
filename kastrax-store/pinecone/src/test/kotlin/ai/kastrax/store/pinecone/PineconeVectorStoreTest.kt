package ai.kastrax.store.pinecone

import ai.kastrax.store.SimilarityMetric
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.util.*

class PineconeVectorStoreTest {

    private lateinit var mockVectorStore: PineconeVectorStore
    private lateinit var mockClient: HttpClient

    @BeforeEach
    fun setUp() {
        // 创建模拟客户端
        mockClient = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                })
            }
            engine {
                addHandler { request ->
                    when (request.url.toString()) {
                        "https://controller.test-env.pinecone.io/databases" -> {
                            if (request.method == HttpMethod.Get) {
                                // 列出索引
                                respond(
                                    content = """[]""",
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                                )
                            } else {
                                // 创建索引
                                respond(
                                    content = """{"name":"test_index"}""",
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                                )
                            }
                        }
                        "https://controller.test-env.pinecone.io/databases/test_index" -> {
                            if (request.method == HttpMethod.Get) {
                                // 获取索引信息
                                respond(
                                    content = """{"database":{"name":"test_index","dimension":3,"metric":"cosine"}, "status":{"host":"test-index-123.svc.test-env.pinecone.io"}}""",
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                                )
                            } else if (request.method == HttpMethod.Delete) {
                                // 删除索引
                                respond(
                                    content = """{}""",
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                                )
                            } else {
                                respond(
                                    content = """{"error":"Method not supported"}""",
                                    status = HttpStatusCode.BadRequest,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                                )
                            }
                        }
                        "https://test-index-123.svc.test-env.pinecone.io/vectors/upsert" -> {
                            // 添加向量
                            respond(
                                content = """{"upsertedCount":3}""",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        "https://test-index-123.svc.test-env.pinecone.io/query" -> {
                            // 查询向量
                            respond(
                                content = """{"matches":[{"id":"1","score":0.9,"metadata":{"name":"vector1"}},{"id":"2","score":0.8,"metadata":{"name":"vector2"}}]}""",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        "https://test-index-123.svc.test-env.pinecone.io/vectors/delete" -> {
                            // 删除向量
                            respond(
                                content = """{}""",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        "https://test-index-123.svc.test-env.pinecone.io/vectors/update" -> {
                            // 更新向量
                            respond(
                                content = """{}""",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        "https://test-index-123.svc.test-env.pinecone.io/describe_index_stats" -> {
                            // 获取索引统计信息
                            respond(
                                content = """{"dimension":3,"totalVectorCount":100}""",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        else -> {
                            respond(
                                content = """{"error":"Unknown URL: ${request.url}"}""",
                                status = HttpStatusCode.NotFound,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                    }
                }
            }
        }

        // 创建模拟向量存储
        mockVectorStore = PineconeVectorStore("test-api-key", "test-env", "test-project")

        // 使用反射设置模拟客户端
        val clientField = PineconeVectorStore::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        clientField.set(mockVectorStore, mockClient)
    }

    @Test
    fun `test create index`() = runBlocking {
        // 创建索引
        val result = mockVectorStore.createIndex("test_index", 3, SimilarityMetric.COSINE)
        assertTrue(result)
    }

    @Test
    fun `test list indexes`() = runBlocking {
        // 列出索引
        val indexes = mockVectorStore.listIndexes()
        assertEquals(0, indexes.size)
    }

    @Test
    fun `test describe index`() = runBlocking {
        // 获取索引信息
        val stats = mockVectorStore.describeIndex("test_index")
        assertEquals(3, stats.dimension)
        assertEquals(100, stats.count)
        assertEquals(SimilarityMetric.COSINE, stats.metric)
    }

    @Test
    fun `test upsert and query`() = runBlocking {
        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2"),
            mapOf("name" to "vector3")
        )
        val ids = mockVectorStore.upsert("test_index", vectors, metadata)
        assertEquals(3, ids.size)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = mockVectorStore.query("test_index", queryVector, 2)
        assertEquals(2, results.size)
        assertEquals("1", results[0].id)
        assertEquals(0.9, results[0].score)
        assertEquals("vector1", results[0].metadata?.get("name"))
    }

    @Test
    fun `test delete vectors`() = runBlocking {
        // 删除向量
        val deleteResult = mockVectorStore.deleteVectors("test_index", listOf("1", "2"))
        assertTrue(deleteResult)
    }

    @Test
    fun `test delete index`() = runBlocking {
        // 删除索引
        val deleteResult = mockVectorStore.deleteIndex("test_index")
        assertTrue(deleteResult)
    }

    @Test
    fun `test update vector`() = runBlocking {
        // 更新向量
        val updateResult = mockVectorStore.updateVector(
            "test_index",
            "1",
            floatArrayOf(0f, 1f, 0f),
            mapOf("name" to "updated_vector")
        )
        assertTrue(updateResult)
    }

    @Test
    fun `test batch upsert`() = runBlocking {
        // 准备大量向量
        val vectorCount = 150
        val vectors = List(vectorCount) { i ->
            when (i % 3) {
                0 -> floatArrayOf(1f, 0f, 0f)
                1 -> floatArrayOf(0f, 1f, 0f)
                else -> floatArrayOf(0f, 0f, 1f)
            }
        }
        val metadata = List(vectorCount) { i ->
            mapOf("index" to i)
        }

        // 批量添加向量
        val ids = mockVectorStore.batchUpsert("test_index", vectors, metadata, batchSize = 50)
        assertEquals(vectorCount, ids.size)
    }

    /**
     * 以下测试需要真实的 Pinecone API 密钥，仅在提供环境变量时运行。
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "PINECONE_API_KEY", matches = ".+")
    fun `test with real Pinecone API`() = runBlocking {
        val apiKey = System.getenv("PINECONE_API_KEY") ?: return@runBlocking
        val environment = System.getenv("PINECONE_ENVIRONMENT") ?: "gcp-starter"
        val projectId = System.getenv("PINECONE_PROJECT_ID") ?: ""

        // 创建真实的向量存储
        val realVectorStore = PineconeVectorStore(apiKey, environment, projectId)

        try {
            // 创建索引
            val indexName = "test-index-${UUID.randomUUID().toString().substring(0, 8)}"
            realVectorStore.createIndex(indexName, 3, SimilarityMetric.COSINE)

            // 等待索引准备就绪
            var ready = false
            var attempts = 0
            while (!ready && attempts < 10) {
                try {
                    realVectorStore.describeIndex(indexName)
                    ready = true
                } catch (e: Exception) {
                    attempts++
                    Thread.sleep(5000)
                }
            }

            if (!ready) {
                fail<String>("Index not ready after 50 seconds")
            }

            // 添加向量
            val vectors = listOf(
                floatArrayOf(1f, 0f, 0f),
                floatArrayOf(0f, 1f, 0f),
                floatArrayOf(0f, 0f, 1f)
            )
            val metadata = listOf(
                mapOf("name" to "vector1"),
                mapOf("name" to "vector2"),
                mapOf("name" to "vector3")
            )
            val ids = realVectorStore.upsert(indexName, vectors, metadata)
            assertEquals(3, ids.size)

            // 查询向量
            val queryVector = floatArrayOf(1f, 0f, 0f)
            val results = realVectorStore.query(indexName, queryVector, 2)
            assertEquals(2, results.size)

            // 删除索引
            realVectorStore.deleteIndex(indexName)
        } finally {
            realVectorStore.close()
        }
    }
}
