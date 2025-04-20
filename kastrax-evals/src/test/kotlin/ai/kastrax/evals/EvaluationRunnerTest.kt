package ai.kastrax.evals

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EvaluationRunnerTest {

    @Test
    fun `test evaluation runner with single evaluator`() = runBlocking {
        // 创建测试评估器
        val evaluator = evaluator<String, String> {
            name = "TestEvaluator"
            description = "Test evaluator for testing"
            
            evaluate { input, output, options ->
                val score = if (input == output) 1.0 else 0.0
                EvaluationResult(score, mapOf("input" to input, "output" to output))
            }
        }
        
        // 创建评估运行器
        val runner = EvaluationRunner(listOf(evaluator))
        
        // 运行评估
        val result = runner.run(evaluator, "test", "test")
        
        // 验证结果
        assertEquals("TestEvaluator", result.evaluatorName)
        assertEquals(1.0, result.result.score)
        assertEquals("test", result.input)
        assertEquals("test", result.output)
        assertTrue(result.durationMs >= 0)
    }
    
    @Test
    fun `test evaluation runner with multiple evaluators`() = runBlocking {
        // 创建测试评估器
        val evaluator1 = evaluator<String, String> {
            name = "ExactMatchEvaluator"
            description = "Exact match evaluator"
            
            evaluate { input, output, options ->
                val score = if (input == output) 1.0 else 0.0
                EvaluationResult(score)
            }
        }
        
        val evaluator2 = evaluator<String, String> {
            name = "ContainsEvaluator"
            description = "Contains evaluator"
            
            evaluate { input, output, options ->
                val score = if (output.contains(input)) 1.0 else 0.0
                EvaluationResult(score)
            }
        }
        
        // 创建评估运行器
        val runner = EvaluationRunner(listOf(evaluator1, evaluator2))
        
        // 运行所有评估器
        val results = runner.runAll("test", "This is a test")
        
        // 验证结果
        assertEquals(2, results.size)
        
        // 第一个评估器应该返回 0.0（不完全匹配）
        val result1 = results.find { it.evaluatorName == "ExactMatchEvaluator" }
        assertNotNull(result1)
        assertEquals(0.0, result1!!.result.score)
        
        // 第二个评估器应该返回 1.0（包含输入）
        val result2 = results.find { it.evaluatorName == "ContainsEvaluator" }
        assertNotNull(result2)
        assertEquals(1.0, result2!!.result.score)
    }
    
    @Test
    fun `test evaluation report`() = runBlocking {
        // 创建测试评估器
        val evaluator1 = evaluator<String, String> {
            name = "Evaluator1"
            description = "First evaluator"
            
            evaluate { _, _, _ -> EvaluationResult(0.8) }
        }
        
        val evaluator2 = evaluator<String, String> {
            name = "Evaluator2"
            description = "Second evaluator"
            
            evaluate { _, _, _ -> EvaluationResult(0.6) }
        }
        
        // 创建评估运行器
        val runner = EvaluationRunner(listOf(evaluator1, evaluator2))
        
        // 运行所有评估器
        val results = runner.runAll("input", "output")
        
        // 创建评估报告
        val report = EvaluationReport(results)
        
        // 验证报告
        assertEquals(0.7, report.averageScore(), 0.001)
        assertTrue(report.totalDurationMs() >= 0)
        
        // 验证摘要
        val summary = report.summary()
        assertEquals(0.7, summary["averageScore"])
        assertEquals(2, summary["evaluatorCount"])
        assertTrue(summary["totalDurationMs"] is Long)
        
        // 验证 JSON 输出
        val json = report.toJson()
        assertTrue(json.contains("\"averageScore\""))
        assertTrue(json.contains("\"evaluatorCount\""))
        assertTrue(json.contains("\"totalDurationMs\""))
    }
}
