package ai.kastrax.rag.document

import ai.kastrax.rag.embedding.RandomEmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SemanticDocumentSplitterTest {

    @Test
    fun `test semantic splitting with simple text`() = runBlocking {
        // 创建一个简单的文档
        val text = """
            人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。

            机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。机器学习算法通过分析大量数据来识别模式，并使用这些模式进行预测或决策。

            深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。深度学习在图像识别、语音识别和自然语言处理等领域取得了显著成功。

            自然语言处理（NLP）是人工智能的一个分支，专注于使计算机理解和生成人类语言。NLP技术被用于机器翻译、情感分析、文本摘要等应用。
        """.trimIndent()

        val document = Document(text)

        // 创建嵌入服务（使用随机嵌入服务进行测试）
        val embeddingService = RandomEmbeddingService(dimensions = 384, seed = 42)

        // 创建语义分块器
        val splitter = SemanticDocumentSplitter(
            embeddingService = embeddingService,
            chunkSize = 200,
            chunkOverlap = 50,
            similarityThreshold = 0.7
        )

        // 分割文档
        val chunks = splitter.split(document)

        // 验证结果
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.content.isNotBlank() })
        assertTrue(chunks.all { it.content.length <= 200 })

        // 验证元数据
        chunks.forEachIndexed { index, chunk ->
            assertEquals(index, chunk.metadata["chunk_index"])
            assertEquals(chunks.size, chunk.metadata["chunk_total"])
            assertEquals("semantic", chunk.metadata["chunk_type"])
        }
    }

    @Test
    fun `test semantic splitting with different similarity thresholds`() = runBlocking {
        // 创建一个包含不同主题的文档
        val text = """
            人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。

            机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。

            太阳系是由太阳及其周围的行星、卫星、小行星、彗星等天体组成的行星系统。

            地球是太阳系中的第三颗行星，也是目前已知唯一孕育和支持生命的天体。
        """.trimIndent()

        val document = Document(text)

        // 创建嵌入服务
        val embeddingService = RandomEmbeddingService(dimensions = 384, seed = 42)

        // 使用高相似度阈值的分块器
        val highThresholdSplitter = SemanticDocumentSplitter(
            embeddingService = embeddingService,
            chunkSize = 500,
            chunkOverlap = 50,
            similarityThreshold = 0.9  // 高相似度阈值
        )

        // 使用低相似度阈值的分块器
        val lowThresholdSplitter = SemanticDocumentSplitter(
            embeddingService = embeddingService,
            chunkSize = 500,
            chunkOverlap = 50,
            similarityThreshold = 0.1  // 低相似度阈值
        )

        // 分割文档
        val highThresholdChunks = highThresholdSplitter.split(document)
        val lowThresholdChunks = lowThresholdSplitter.split(document)

        // 验证结果：高阈值应该产生更多的块（因为更难合并）
        assertTrue(highThresholdChunks.size >= lowThresholdChunks.size)
    }

    @Test
    fun `test semantic splitting with long text`() = runBlocking {
        // 创建一个长文档
        val longText = StringBuilder()
        for (i in 1..10) {
            longText.append("这是第 $i 段文本，它包含一些关于人工智能的信息。人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。")
            longText.append("\n\n")
        }

        val document = Document(longText.toString())

        // 创建嵌入服务
        val embeddingService = RandomEmbeddingService(dimensions = 384, seed = 42)

        // 创建语义分块器
        val splitter = SemanticDocumentSplitter(
            embeddingService = embeddingService,
            chunkSize = 200,
            chunkOverlap = 50,
            similarityThreshold = 0.7
        )

        // 分割文档
        val chunks = splitter.split(document)

        // 验证结果
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.content.length <= 200 })
    }

    @Test
    fun `test semantic splitting with empty text`() = runBlocking {
        // 创建一个空文档
        val document = Document("")

        // 创建嵌入服务
        val embeddingService = RandomEmbeddingService(dimensions = 384, seed = 42)

        // 创建语义分块器
        val splitter = SemanticDocumentSplitter(
            embeddingService = embeddingService,
            chunkSize = 200,
            chunkOverlap = 50,
            similarityThreshold = 0.7
        )

        // 分割文档
        val chunks = splitter.split(document)

        // 验证结果：应该返回一个包含空字符串的列表
        assertEquals(1, chunks.size)
        assertEquals("", chunks[0].content)
    }

    @Test
    fun `test semantic splitting with custom metadata`() = runBlocking {
        // 创建一个带有自定义元数据的文档
        val text = "这是一个测试文档，用于测试语义分块器。"
        val metadata = mapOf(
            "source" to "test",
            "author" to "kastrax",
            "date" to "2023-01-01"
        )

        val document = Document(text, metadata)

        // 创建嵌入服务
        val embeddingService = RandomEmbeddingService(dimensions = 384, seed = 42)

        // 创建语义分块器
        val splitter = SemanticDocumentSplitter(
            embeddingService = embeddingService,
            chunkSize = 200,
            chunkOverlap = 50,
            similarityThreshold = 0.7
        )

        // 分割文档
        val chunks = splitter.split(document)

        // 验证结果：应该保留原始元数据
        assertEquals(1, chunks.size)
        assertEquals("test", chunks[0].metadata["source"])
        assertEquals("kastrax", chunks[0].metadata["author"])
        assertEquals("2023-01-01", chunks[0].metadata["date"])
    }

    @Test
    fun `test semantic splitting with different initial separators`() = runBlocking {
        // 创建一个文档
        val text = """
            第一段：这是第一段文本。这段文本包含多个句子。这些句子应该被分开。

            第二段：这是第二段文本。它也包含多个句子。这些句子也应该被分开。
        """.trimIndent()

        val document = Document(text)

        // 创建嵌入服务
        val embeddingService = RandomEmbeddingService(dimensions = 384, seed = 42)

        // 创建使用不同初始分隔符的分块器
        val paragraphSplitter = SemanticDocumentSplitter(
            embeddingService = embeddingService,
            chunkSize = 200,
            chunkOverlap = 50,
            similarityThreshold = 0.7,
            initialSeparators = listOf("\n\n")  // 只使用段落分隔符
        )

        val sentenceSplitter = SemanticDocumentSplitter(
            embeddingService = embeddingService,
            chunkSize = 200,
            chunkOverlap = 50,
            similarityThreshold = 0.7,
            initialSeparators = listOf("。", "：")  // 使用句号和冒号作为分隔符
        )

        // 分割文档
        val paragraphChunks = paragraphSplitter.split(document)
        val sentenceChunks = sentenceSplitter.split(document)

        // 验证结果：句子分块器应该产生更多的块
        assertTrue(sentenceChunks.size >= paragraphChunks.size)
    }

    @Test
    fun `test fallback to recursive character splitting`() = runBlocking {
        // 创建一个文档
        val text = "这是一个测试文档，用于测试当语义分块失败时的回退机制。"
        val document = Document(text)

        // 创建一个会抛出异常的嵌入服务
        val failingEmbeddingService = object : ai.kastrax.rag.embedding.EmbeddingService {
            override suspend fun embed(text: String): ai.kastrax.rag.embedding.Embedding {
                throw RuntimeException("模拟嵌入服务失败")
            }
        }

        // 创建语义分块器
        val splitter = SemanticDocumentSplitter(
            embeddingService = failingEmbeddingService,
            chunkSize = 200,
            chunkOverlap = 50,
            similarityThreshold = 0.7
        )

        // 分割文档（应该回退到递归字符分块）
        val chunks = splitter.split(document)

        // 验证结果：应该成功分块，但不包含 "chunk_type" = "semantic" 元数据
        assertTrue(chunks.isNotEmpty())
        assertNotEquals("semantic", chunks[0].metadata["chunk_type"])
    }
}
