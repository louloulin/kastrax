package ai.kastrax.evals.reporting

import ai.kastrax.evals.EvaluationReport
import ai.kastrax.evals.EvaluationResult
import ai.kastrax.evals.EvaluationRunResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Instant

class ReportGeneratorTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `test generate JSON report`() {
        val reportGenerator = ReportGenerator()
        val report = createTestReport()
        
        val jsonReport = reportGenerator.generateReport(report, ReportFormat.JSON)
        
        // 验证 JSON 报告
        assertTrue(jsonReport.contains("\"summary\""))
        assertTrue(jsonReport.contains("\"results\""))
        assertTrue(jsonReport.contains("\"evaluatorName\""))
        assertTrue(jsonReport.contains("\"score\""))
    }
    
    @Test
    fun `test generate Markdown report`() {
        val reportGenerator = ReportGenerator()
        val report = createTestReport()
        
        val markdownReport = reportGenerator.generateReport(report, ReportFormat.MARKDOWN)
        
        // 验证 Markdown 报告
        assertTrue(markdownReport.contains("# 评估报告"))
        assertTrue(markdownReport.contains("## 摘要"))
        assertTrue(markdownReport.contains("## 详细结果"))
        assertTrue(markdownReport.contains("| 评估器 | 分数 | 耗时 (毫秒) |"))
    }
    
    @Test
    fun `test generate HTML report`() {
        val reportGenerator = ReportGenerator()
        val report = createTestReport()
        
        val htmlReport = reportGenerator.generateReport(report, ReportFormat.HTML)
        
        // 验证 HTML 报告
        assertTrue(htmlReport.contains("<!DOCTYPE html>"))
        assertTrue(htmlReport.contains("<title>评估报告</title>"))
        assertTrue(htmlReport.contains("<table>"))
        assertTrue(htmlReport.contains("<th>评估器</th>"))
    }
    
    @Test
    fun `test generate CSV report`() {
        val reportGenerator = ReportGenerator()
        val report = createTestReport()
        
        val csvReport = reportGenerator.generateReport(report, ReportFormat.CSV)
        
        // 验证 CSV 报告
        assertTrue(csvReport.contains("评估器,分数,开始时间,结束时间,耗时(毫秒)"))
        assertTrue(csvReport.contains("TestEvaluator1,0.8,"))
        assertTrue(csvReport.contains("TestEvaluator2,0.6,"))
    }
    
    @Test
    fun `test save report to file`() {
        val reportGenerator = ReportGenerator()
        val report = createTestReport()
        
        // 保存 Markdown 报告
        val markdownFile = tempDir.resolve("report.md").toString()
        reportGenerator.saveReport(report, markdownFile, ReportFormat.MARKDOWN)
        
        // 验证文件存在且内容正确
        val markdownContent = File(markdownFile).readText()
        assertTrue(markdownContent.contains("# 评估报告"))
        
        // 保存 JSON 报告
        val jsonFile = tempDir.resolve("report.json").toString()
        reportGenerator.saveReport(report, jsonFile, ReportFormat.JSON)
        
        // 验证文件存在且内容正确
        val jsonContent = File(jsonFile).readText()
        assertTrue(jsonContent.contains("\"summary\""))
    }
    
    /**
     * 创建测试报告。
     */
    private fun createTestReport(): EvaluationReport<String, String> {
        val now = Instant.now()
        
        val result1 = EvaluationRunResult(
            evaluatorName = "TestEvaluator1",
            result = EvaluationResult(0.8, mapOf("detail" to "Test detail 1")),
            input = "test input",
            output = "test output",
            startTime = now.minusMillis(1000),
            endTime = now.minusMillis(500),
            durationMs = 500
        )
        
        val result2 = EvaluationRunResult(
            evaluatorName = "TestEvaluator2",
            result = EvaluationResult(0.6, mapOf("detail" to "Test detail 2")),
            input = "test input",
            output = "test output",
            startTime = now.minusMillis(500),
            endTime = now,
            durationMs = 500
        )
        
        return EvaluationReport(listOf(result1, result2))
    }
}
