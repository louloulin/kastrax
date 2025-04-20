package ai.kastrax.evals

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EvaluatorTest {

    @Test
    fun `test evaluator builder`() {
        val evaluator = evaluator<String, String> {
            name = "TestEvaluator"
            description = "Test evaluator for testing"
            
            evaluate { input, output, options ->
                val score = if (input == output) 1.0 else 0.0
                EvaluationResult(score, mapOf("input" to input, "output" to output))
            }
        }
        
        assertEquals("TestEvaluator", evaluator.name)
        assertEquals("Test evaluator for testing", evaluator.description)
    }
    
    @Test
    fun `test evaluator evaluation`() = runBlocking {
        val evaluator = evaluator<String, String> {
            name = "TestEvaluator"
            description = "Test evaluator for testing"
            
            evaluate { input, output, options ->
                val score = if (input == output) 1.0 else 0.0
                EvaluationResult(score, mapOf("input" to input, "output" to output))
            }
        }
        
        // 测试匹配的情况
        val result1 = evaluator.evaluate("test", "test")
        assertEquals(1.0, result1.score)
        assertEquals("test", result1.details["input"])
        assertEquals("test", result1.details["output"])
        
        // 测试不匹配的情况
        val result2 = evaluator.evaluate("test", "different")
        assertEquals(0.0, result2.score)
        assertEquals("test", result2.details["input"])
        assertEquals("different", result2.details["output"])
    }
    
    @Test
    fun `test evaluation result factory methods`() {
        // 测试成功结果
        val successResult = EvaluationResult.success(mapOf("message" to "Success"))
        assertEquals(1.0, successResult.score)
        assertEquals("Success", successResult.details["message"])
        
        // 测试失败结果
        val failureResult = EvaluationResult.failure(mapOf("message" to "Failure"))
        assertEquals(0.0, failureResult.score)
        assertEquals("Failure", failureResult.details["message"])
    }
}
