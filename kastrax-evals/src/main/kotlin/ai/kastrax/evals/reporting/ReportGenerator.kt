package ai.kastrax.evals.reporting

import ai.kastrax.evals.EvaluationReport
import ai.kastrax.evals.EvaluationRunResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

/**
 * 报告格式。
 */
enum class ReportFormat {
    JSON,
    MARKDOWN,
    HTML,
    CSV
}

/**
 * 报告生成器，用于生成评估报告。
 */
class ReportGenerator {
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    /**
     * 生成报告。
     *
     * @param report 评估报告
     * @param format 报告格式
     * @return 报告内容
     */
    fun <I, O> generateReport(
        report: EvaluationReport<I, O>,
        format: ReportFormat = ReportFormat.MARKDOWN
    ): String {
        return when (format) {
            ReportFormat.JSON -> generateJsonReport(report)
            ReportFormat.MARKDOWN -> generateMarkdownReport(report)
            ReportFormat.HTML -> generateHtmlReport(report)
            ReportFormat.CSV -> generateCsvReport(report)
        }
    }

    /**
     * 将报告保存到文件。
     *
     * @param report 评估报告
     * @param filePath 文件路径
     * @param format 报告格式
     */
    fun <I, O> saveReport(
        report: EvaluationReport<I, O>,
        filePath: String,
        format: ReportFormat = ReportFormat.MARKDOWN
    ) {
        val content = generateReport(report, format)
        File(filePath).writeText(content)
        logger.info { "Report saved to $filePath" }
    }

    /**
     * 生成 JSON 报告。
     *
     * @param report 评估报告
     * @return JSON 报告内容
     */
    private fun <I, O> generateJsonReport(report: EvaluationReport<I, O>): String {
        val reportData = mapOf(
            "summary" to report.summary(),
            "results" to report.results.map { result ->
                mapOf(
                    "evaluatorName" to result.evaluatorName,
                    "score" to result.result.score,
                    "details" to result.result.details,
                    "startTime" to dateFormatter.format(result.startTime),
                    "endTime" to dateFormatter.format(result.endTime),
                    "durationMs" to result.durationMs
                )
            }
        )
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reportData)
    }

    /**
     * 生成 Markdown 报告。
     *
     * @param report 评估报告
     * @return Markdown 报告内容
     */
    private fun <I, O> generateMarkdownReport(report: EvaluationReport<I, O>): String {
        val summary = report.summary()
        val now = dateFormatter.format(Instant.now())

        return buildString {
            append("# 评估报告\n\n")
            append("生成时间：$now\n\n")
            
            append("## 摘要\n\n")
            append("- 平均分数：${summary["averageScore"]}\n")
            append("- 评估器数量：${summary["evaluatorCount"]}\n")
            append("- 总耗时：${summary["totalDurationMs"]} 毫秒\n\n")
            
            append("## 详细结果\n\n")
            append("| 评估器 | 分数 | 耗时 (毫秒) |\n")
            append("|-------|------|------------|\n")
            
            report.results.forEach { result ->
                append("| ${result.evaluatorName} | ${result.result.score} | ${result.durationMs} |\n")
            }
            
            append("\n## 评估器详情\n\n")
            
            report.results.forEach { result ->
                append("### ${result.evaluatorName}\n\n")
                append("- 分数：${result.result.score}\n")
                append("- 开始时间：${dateFormatter.format(result.startTime)}\n")
                append("- 结束时间：${dateFormatter.format(result.endTime)}\n")
                append("- 耗时：${result.durationMs} 毫秒\n\n")
                
                append("#### 详细信息\n\n")
                append("```json\n")
                append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.result.details))
                append("\n```\n\n")
            }
        }
    }

    /**
     * 生成 HTML 报告。
     *
     * @param report 评估报告
     * @return HTML 报告内容
     */
    private fun <I, O> generateHtmlReport(report: EvaluationReport<I, O>): String {
        val summary = report.summary()
        val now = dateFormatter.format(Instant.now())

        return buildString {
            append("<!DOCTYPE html>\n")
            append("<html>\n")
            append("<head>\n")
            append("  <title>评估报告</title>\n")
            append("  <style>\n")
            append("    body { font-family: Arial, sans-serif; margin: 20px; }\n")
            append("    h1, h2, h3 { color: #333; }\n")
            append("    table { border-collapse: collapse; width: 100%; }\n")
            append("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n")
            append("    th { background-color: #f2f2f2; }\n")
            append("    tr:nth-child(even) { background-color: #f9f9f9; }\n")
            append("    .summary { background-color: #e9f7ef; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n")
            append("    .details { background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin-top: 10px; }\n")
            append("    pre { background-color: #f8f8f8; padding: 10px; border-radius: 5px; overflow-x: auto; }\n")
            append("  </style>\n")
            append("</head>\n")
            append("<body>\n")
            
            append("  <h1>评估报告</h1>\n")
            append("  <p>生成时间：$now</p>\n\n")
            
            append("  <div class=\"summary\">\n")
            append("    <h2>摘要</h2>\n")
            append("    <p>平均分数：${summary["averageScore"]}</p>\n")
            append("    <p>评估器数量：${summary["evaluatorCount"]}</p>\n")
            append("    <p>总耗时：${summary["totalDurationMs"]} 毫秒</p>\n")
            append("  </div>\n\n")
            
            append("  <h2>详细结果</h2>\n")
            append("  <table>\n")
            append("    <tr>\n")
            append("      <th>评估器</th>\n")
            append("      <th>分数</th>\n")
            append("      <th>耗时 (毫秒)</th>\n")
            append("    </tr>\n")
            
            report.results.forEach { result ->
                append("    <tr>\n")
                append("      <td>${result.evaluatorName}</td>\n")
                append("      <td>${result.result.score}</td>\n")
                append("      <td>${result.durationMs}</td>\n")
                append("    </tr>\n")
            }
            
            append("  </table>\n\n")
            
            append("  <h2>评估器详情</h2>\n\n")
            
            report.results.forEach { result ->
                append("  <div class=\"details\">\n")
                append("    <h3>${result.evaluatorName}</h3>\n")
                append("    <p>分数：${result.result.score}</p>\n")
                append("    <p>开始时间：${dateFormatter.format(result.startTime)}</p>\n")
                append("    <p>结束时间：${dateFormatter.format(result.endTime)}</p>\n")
                append("    <p>耗时：${result.durationMs} 毫秒</p>\n\n")
                
                append("    <h4>详细信息</h4>\n")
                append("    <pre>\n")
                append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.result.details))
                append("    </pre>\n")
                append("  </div>\n\n")
            }
            
            append("</body>\n")
            append("</html>\n")
        }
    }

    /**
     * 生成 CSV 报告。
     *
     * @param report 评估报告
     * @return CSV 报告内容
     */
    private fun <I, O> generateCsvReport(report: EvaluationReport<I, O>): String {
        return buildString {
            append("评估器,分数,开始时间,结束时间,耗时(毫秒)\n")
            
            report.results.forEach { result ->
                append("${result.evaluatorName},")
                append("${result.result.score},")
                append("${dateFormatter.format(result.startTime)},")
                append("${dateFormatter.format(result.endTime)},")
                append("${result.durationMs}\n")
            }
        }
    }
}
