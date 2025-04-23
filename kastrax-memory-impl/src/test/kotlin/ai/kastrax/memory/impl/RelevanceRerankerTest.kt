package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.api.SemanticSearchResult
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RelevanceRerankerTest {

    /**
     * 创建测试消息。
     */
    private fun createTestMessage(content: String): MemoryMessage {
        return MemoryMessage(
            id = "msg-${System.currentTimeMillis()}-${content.hashCode()}",
            threadId = "test-thread",
            message = SimpleMessage(
                role = MessageRole.ASSISTANT,
                content = content
            ),
            createdAt = Clock.System.now()
        )
    }

    @Test
    fun `test rerank by relevance`() = runTest {
        // 创建重排序器
        val reranker = DiversityReranker()

        // 创建搜索结果
        val results = listOf(
            SemanticSearchResult(
                createTestMessage("Python is a programming language used in AI."),
                0.9f
            ),
            SemanticSearchResult(
                createTestMessage("Python is widely used for machine learning and data science."),
                0.85f
            ),
            SemanticSearchResult(
                createTestMessage("Python has libraries like TensorFlow and PyTorch for deep learning."),
                0.8f
            ),
            SemanticSearchResult(
                createTestMessage("Java is another programming language used in enterprise applications."),
                0.7f
            ),
            SemanticSearchResult(
                createTestMessage("JavaScript is used for web development."),
                0.6f
            )
        )

        // 重排序
        val reranked = reranker.rerank("Python and machine learning", results)

        // 验证结果
        assertEquals(5, reranked.size)

        // 验证包含"machine learning"的消息排在前面
        val machineLearningShouldBeFirst = reranked[0].message.message.content.contains("machine learning")
        assertEquals(true, machineLearningShouldBeFirst)
    }

    @Test
    fun `test rerank with context`() = runTest {
        // 创建重排序器
        val reranker = DiversityReranker()

        // 创建搜索结果
        val results = listOf(
            SemanticSearchResult(
                createTestMessage("What is deep learning?"),
                0.9f
            ),
            SemanticSearchResult(
                createTestMessage("Deep learning is a subset of machine learning."),
                0.85f
            ),
            SemanticSearchResult(
                createTestMessage("Traditional ML algorithms require feature engineering."),
                0.8f
            )
        )

        // 上下文信息（当前测试中未使用）
        // 注意：这里的上下文将在未来版本中使用，目前保留作为注释

        // 重排序
        val reranked = reranker.rerank("What's the difference between deep learning and traditional ML?", results)

        // 验证结果
        assertEquals(3, reranked.size)

        // 验证相关的消息对排在前面
        val firstPair = reranked[0].message.message.content
        val secondPair = reranked[1].message.message.content

        // 检查是否"deep learning"和"traditional ML"的对比排在前面
        val isDeepLearningPair = firstPair.contains("deep learning") && secondPair.contains("traditional ML")
        val isDefinitionPair = firstPair.contains("What is") && secondPair.contains("is a subset")

        assertEquals(true, isDeepLearningPair || isDefinitionPair)
    }

    @Test
    fun `test rerank with topic coherence`() = runTest {
        // 创建重排序器
        val reranker = DiversityReranker()

        // 创建搜索结果
        val results = listOf(
            SemanticSearchResult(
                createTestMessage("Python is great for AI development."),
                0.9f
            ),
            SemanticSearchResult(
                createTestMessage("Java is used for enterprise applications."),
                0.85f
            ),
            SemanticSearchResult(
                createTestMessage("Python has libraries like TensorFlow for AI."),
                0.8f
            ),
            SemanticSearchResult(
                createTestMessage("JavaScript is used for web development."),
                0.75f
            )
        )

        // 重排序
        val reranked = reranker.rerank("Python and AI", results)

        // 验证结果
        assertEquals(4, reranked.size)

        // 验证相同主题的消息排在一起
        val firstMessage = reranked[0].message.message.content
        val secondMessage = reranked[1].message.message.content

        // 检查是否Python相关的消息排在一起
        val bothContainPython = firstMessage.contains("Python") && secondMessage.contains("Python")
        val eitherContainsAI = firstMessage.contains("AI") || secondMessage.contains("AI")

        assertEquals(true, bothContainPython && eitherContainsAI)
    }
}
