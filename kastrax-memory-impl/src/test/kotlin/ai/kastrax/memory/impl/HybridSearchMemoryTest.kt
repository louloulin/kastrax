package ai.kastrax.memory.impl

import ai.kastrax.memory.api.EmbeddingGenerator
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.api.SemanticRecallConfig
import ai.kastrax.memory.api.VectorStorage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HybridSearchMemoryTest {

    // 模拟基础内存
    private val baseMemory = createTestMemory {
        storage(MemoryFactory.createInMemoryStorage())
        lastMessages(10)
    }

    // 模拟嵌入生成器
    private val embeddingGenerator = MockEmbeddingGenerator()

    // 模拟向量存储
    private val vectorStorage = InMemoryVectorStorage()

    // 创建混合搜索内存
    private val hybridMemory = HybridSearchMemoryImpl(
        baseMemory = baseMemory,
        embeddingGenerator = embeddingGenerator,
        vectorStorage = vectorStorage,
        keywordWeight = 0.3f,
        semanticWeight = 0.7f
    )

    @Test
    fun `test hybrid search with Python and Java messages`() = runTest {
        // 创建线程
        val threadId = hybridMemory.createThread("Test Thread")

        // 保存消息
        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = "Python is a programming language used in AI."
            ),
            threadId
        )

        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.ASSISTANT,
                content = "Yes, Python is widely used in AI development due to its simplicity " +
                    "and rich ecosystem of libraries."
            ),
            threadId
        )

        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = "What is machine learning?"
            ),
            threadId
        )

        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.ASSISTANT,
                content = "That's correct. Machine learning is a branch of AI focused on " +
                    "building systems that learn from data."
            ),
            threadId
        )

        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = "Java is also a programming language."
            ),
            threadId
        )

        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.ASSISTANT,
                content = "Yes, Java is popular for enterprise applications due to its " +
                    "platform independence and strong ecosystem."
            ),
            threadId
        )

        // 执行混合搜索
        val results = hybridMemory.semanticSearch(
            query = "Tell me about Python and AI",
            threadId = threadId,
            config = SemanticRecallConfig(topK = 3)
        )

        // 验证结果
        assertEquals(3, results.size)

        // 检查结果是否包含Python相关消息
        val pythonMessages = results.filter { result ->
            result.message.message.content.contains("Python")
        }

        assertTrue(pythonMessages.isNotEmpty())
        assertTrue(pythonMessages.all { it.score > 0.5f })
    }

    @Test
    fun `test hybrid search with deep learning messages`() = runTest {
        // 创建线程
        val threadId = hybridMemory.createThread("Deep Learning Thread")

        // 保存消息
        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = "What is deep learning?"
            ),
            threadId
        )

        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.ASSISTANT,
                content = "Deep learning is a subset of machine learning that uses neural networks with multiple layers."
            ),
            threadId
        )

        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = "How is deep learning different from traditional ML?"
            ),
            threadId
        )

        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.ASSISTANT,
                content = "Unlike traditional ML, deep learning automatically learns features " +
                    "from data using multiple layers."
            ),
            threadId
        )

        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = "What are some applications of deep learning?"
            ),
            threadId
        )

        hybridMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.ASSISTANT,
                content = "Deep learning is used in computer vision, natural language processing, " +
                    "speech recognition, and more."
            ),
            threadId
        )

        // 执行混合搜索
        val results = hybridMemory.semanticSearch(
            query = "neural networks and deep learning",
            threadId = threadId,
            config = SemanticRecallConfig(topK = 2)
        )

        // 验证结果
        assertEquals(2, results.size)

        // 检查结果是否包含神经网络相关消息
        val neuralNetworkMessages = results.filter { result ->
            result.message.message.content.contains("neural networks")
        }

        assertTrue(neuralNetworkMessages.isNotEmpty())
    }
}
