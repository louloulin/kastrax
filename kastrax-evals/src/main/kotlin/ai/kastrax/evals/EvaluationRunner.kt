package ai.kastrax.evals

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * 评估运行的结果。
 *
 * @property evaluatorName 评估器名称
 * @property result 评估结果
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property durationMs 持续时间（毫秒）
 */
data class EvaluationRunResult<I, O>(
    val evaluatorName: String,
    val result: EvaluationResult,
    val input: I,
    val output: O,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long
)

/**
 * 评估运行器，用于运行一组评估器。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @property evaluators 评估器列表
 */
class EvaluationRunner<I, O>(
    private val evaluators: List<Evaluator<I, O>>
) {
    /**
     * 运行所有评估器。
     *
     * @param input 输入数据
     * @param output AI 系统的输出
     * @param options 评估选项
     * @return 评估运行结果列表
     */
    suspend fun runAll(
        input: I,
        output: O,
        options: Map<String, Any?> = emptyMap()
    ): List<EvaluationRunResult<I, O>> = coroutineScope {
        evaluators.map { evaluator ->
            async {
                run(evaluator, input, output, options)
            }
        }.awaitAll()
    }

    /**
     * 运行单个评估器。
     *
     * @param evaluator 评估器
     * @param input 输入数据
     * @param output AI 系统的输出
     * @param options 评估选项
     * @return 评估运行结果
     */
    suspend fun run(
        evaluator: Evaluator<I, O>,
        input: I,
        output: O,
        options: Map<String, Any?> = emptyMap()
    ): EvaluationRunResult<I, O> {
        logger.info { "Running evaluator: ${evaluator.name}" }
        
        val startTime = Instant.now()
        
        val result = try {
            evaluator.evaluate(input, output, options)
        } catch (e: Exception) {
            logger.error(e) { "Error running evaluator: ${evaluator.name}" }
            EvaluationResult(0.0, mapOf("error" to e.message))
        }
        
        val endTime = Instant.now()
        val durationMs = endTime.toEpochMilli() - startTime.toEpochMilli()
        
        logger.info { "Evaluator ${evaluator.name} completed in ${durationMs}ms with score: ${result.score}" }
        
        return EvaluationRunResult(
            evaluatorName = evaluator.name,
            result = result,
            input = input,
            output = output,
            startTime = startTime,
            endTime = endTime,
            durationMs = durationMs
        )
    }
}

/**
 * 评估报告，包含多个评估运行的结果。
 *
 * @property results 评估运行结果列表
 */
data class EvaluationReport<I, O>(
    val results: List<EvaluationRunResult<I, O>>
) {
    /**
     * 获取平均分数。
     *
     * @return 平均分数
     */
    fun averageScore(): Double {
        if (results.isEmpty()) return 0.0
        return results.map { it.result.score }.average()
    }

    /**
     * 获取总持续时间（毫秒）。
     *
     * @return 总持续时间
     */
    fun totalDurationMs(): Long {
        return results.sumOf { it.durationMs }
    }

    /**
     * 获取评估报告的摘要。
     *
     * @return 评估报告摘要
     */
    fun summary(): Map<String, Any> {
        return mapOf(
            "averageScore" to averageScore(),
            "totalDurationMs" to totalDurationMs(),
            "evaluatorCount" to results.size,
            "evaluatorScores" to results.associate { it.evaluatorName to it.result.score }
        )
    }

    /**
     * 将评估报告转换为 JSON 字符串。
     *
     * @return JSON 字符串
     */
    fun toJson(): String {
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        mapper.registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
        return mapper.writeValueAsString(summary())
    }
}
