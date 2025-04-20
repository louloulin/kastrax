package ai.kastrax.evals.examples

import ai.kastrax.evals.EvaluationReport
import ai.kastrax.evals.EvaluationRunner
import ai.kastrax.evals.evaluators.containsKeywordsEvaluator
import ai.kastrax.evals.evaluators.exactMatchEvaluator
import ai.kastrax.evals.evaluators.regexMatchEvaluator
import ai.kastrax.evals.evaluators.similarityEvaluator
import ai.kastrax.evals.reporting.ReportFormat
import ai.kastrax.evals.reporting.ReportGenerator
import kotlinx.coroutines.runBlocking

/**
 * 评估框架使用示例。
 */
object EvaluationExample {
    
    /**
     * 运行示例。
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建评估器
        val exactMatchEval = exactMatchEvaluator()
        val containsKeywordsEval = containsKeywordsEvaluator()
        val regexMatchEval = regexMatchEvaluator()
        val similarityEval = similarityEvaluator()
        
        // 创建评估运行器
        val runner = EvaluationRunner(
            listOf(exactMatchEval, containsKeywordsEval, regexMatchEval, similarityEval)
        )
        
        // 准备输入和输出
        val input = "What is the capital of France?"
        val output = "The capital of France is Paris."
        
        // 准备评估选项
        val exactMatchOptions = mapOf(
            "expected" to "The capital of France is Paris.",
            "ignoreCase" to true,
            "ignoreWhitespace" to true
        )
        
        val keywordsOptions = mapOf(
            "keywords" to listOf("capital", "France", "Paris"),
            "requireAll" to true
        )
        
        val regexOptions = mapOf(
            "pattern" to "capital of (\\w+) is (\\w+)"
        )
        
        val similarityOptions = mapOf(
            "expected" to "The capital of France is Paris.",
            "method" to "jaccard"
        )
        
        // 运行评估
        val exactMatchResult = runner.run(exactMatchEval, input, output, exactMatchOptions)
        val keywordsResult = runner.run(containsKeywordsEval, input, output, keywordsOptions)
        val regexResult = runner.run(regexMatchEval, input, output, regexOptions)
        val similarityResult = runner.run(similarityEval, input, output, similarityOptions)
        
        // 创建评估报告
        val report = EvaluationReport(
            listOf(exactMatchResult, keywordsResult, regexResult, similarityResult)
        )
        
        // 生成报告
        val reportGenerator = ReportGenerator()
        
        // 生成不同格式的报告
        val markdownReport = reportGenerator.generateReport(report, ReportFormat.MARKDOWN)
        val jsonReport = reportGenerator.generateReport(report, ReportFormat.JSON)
        val htmlReport = reportGenerator.generateReport(report, ReportFormat.HTML)
        val csvReport = reportGenerator.generateReport(report, ReportFormat.CSV)
        
        // 打印报告摘要
        println("评估报告摘要:")
        println("平均分数: ${report.averageScore()}")
        println("总耗时: ${report.totalDurationMs()} 毫秒")
        println("评估器数量: ${report.results.size}")
        println()
        
        // 打印各评估器的分数
        println("评估器分数:")
        report.results.forEach { result ->
            println("${result.evaluatorName}: ${result.result.score}")
        }
        
        // 保存报告到文件
        reportGenerator.saveReport(report, "evaluation_report.md", ReportFormat.MARKDOWN)
        reportGenerator.saveReport(report, "evaluation_report.json", ReportFormat.JSON)
        reportGenerator.saveReport(report, "evaluation_report.html", ReportFormat.HTML)
        reportGenerator.saveReport(report, "evaluation_report.csv", ReportFormat.CSV)
        
        println("\n报告已保存到当前目录")
    }
}
