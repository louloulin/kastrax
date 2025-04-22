package ai.kastrax.evals.metrics.llm

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CorrectnessMetricTest {
    
    @Test
    fun testCorrectnessEvaluation() = runBlocking {
        // 创建模拟 LLM 客户端
        val mockResponses = mapOf(
            """
            请评估以下回答的正确性。
            
            问题：什么是人工智能？
            
            回答：人工智能是计算机科学的一个分支，致力于创建能够模拟人类智能的系统。
            
            参考答案：人工智能是计算机科学的一个分支，研究如何使计算机能够模拟人类智能行为。
            
            请根据回答的正确性给出 0 到 10 的评分，其中 0 表示完全错误，10 表示完全正确。
            请首先分析回答的正确性，然后给出评分。
            
            评分：
            """.trimIndent() to """
            分析：回答准确地描述了人工智能的基本定义，指出它是计算机科学的一个分支，目标是创建能够模拟人类智能的系统。这与参考答案非常接近，参考答案提到人工智能研究如何使计算机能够模拟人类智能行为。两者在核心概念上是一致的，只是表述略有不同。

            评分：9
            """.trimIndent(),
            
            """
            请评估以下回答的正确性。
            
            问题：什么是人工智能？
            
            回答：人工智能是一种能够自主思考和学习的机器人。
            
            参考答案：人工智能是计算机科学的一个分支，研究如何使计算机能够模拟人类智能行为。
            
            请根据回答的正确性给出 0 到 10 的评分，其中 0 表示完全错误，10 表示完全正确。
            请首先分析回答的正确性，然后给出评分。
            
            评分：
            """.trimIndent() to """
            分析：回答存在一些误导性。首先，人工智能不仅限于机器人，它是一个更广泛的计算机科学领域。其次，虽然某些人工智能系统确实展示了学习能力，但将其描述为"自主思考"过于拟人化，并不准确。参考答案更准确地将人工智能定义为计算机科学的一个分支，研究如何使计算机模拟人类智能行为。

            评分：4
            """.trimIndent()
        )
        
        val mockLlmClient = MockLlmClient(mockResponses)
        val metric = correctnessMetric(mockLlmClient)
        
        // 测试正确答案
        val result1 = metric.calculate(
            input = "什么是人工智能？",
            output = "人工智能是计算机科学的一个分支，致力于创建能够模拟人类智能的系统。",
            options = mapOf(
                "reference" to "人工智能是计算机科学的一个分支，研究如何使计算机能够模拟人类智能行为。"
            )
        )
        assertEquals(0.9, result1.score)
        
        // 测试部分正确答案
        val result2 = metric.calculate(
            input = "什么是人工智能？",
            output = "人工智能是一种能够自主思考和学习的机器人。",
            options = mapOf(
                "reference" to "人工智能是计算机科学的一个分支，研究如何使计算机能够模拟人类智能行为。"
            )
        )
        assertEquals(0.4, result2.score)
    }
    
    @Test
    fun testScoreExtractor() {
        val extractor = DefaultScoreExtractor()
        
        // 测试不同格式的分数
        assertEquals(0.8, extractor.extractScore("评分：8"))
        assertEquals(0.7, extractor.extractScore("分数：7"))
        assertEquals(0.6, extractor.extractScore("得分：6"))
        assertEquals(0.5, extractor.extractScore("我给这个回答 5 分"))
        assertEquals(0.9, extractor.extractScore("根据分析，我给出 9/10 的评分"))
        
        // 测试带小数的分数
        assertEquals(0.75, extractor.extractScore("评分：7.5"))
        
        // 测试无法提取分数的情况
        assertEquals(0.5, extractor.extractScore("这个回答不错"))
    }
}
