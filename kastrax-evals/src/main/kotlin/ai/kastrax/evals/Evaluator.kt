package ai.kastrax.evals

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 评估结果，包含分数和可选的详细信息。
 *
 * @property score 评估分数，通常在 0.0 到 1.0 之间
 * @property details 评估的详细信息，可以包含评估过程中的中间结果、解释等
 */
data class EvaluationResult(
    val score: Double,
    val details: Map<String, Any?> = emptyMap()
) {
    companion object {
        /**
         * 创建一个成功的评估结果（满分）。
         *
         * @param details 可选的详细信息
         * @return 满分的评估结果
         */
        fun success(details: Map<String, Any?> = emptyMap()): EvaluationResult {
            return EvaluationResult(1.0, details)
        }

        /**
         * 创建一个失败的评估结果（零分）。
         *
         * @param details 可选的详细信息
         * @return 零分的评估结果
         */
        fun failure(details: Map<String, Any?> = emptyMap()): EvaluationResult {
            return EvaluationResult(0.0, details)
        }
    }
}

/**
 * 评估器接口，用于评估 AI 系统的输出。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
interface Evaluator<I, O> {
    /**
     * 评估器的名称。
     */
    val name: String

    /**
     * 评估器的描述。
     */
    val description: String

    /**
     * 评估 AI 系统的输出。
     *
     * @param input 输入数据
     * @param output AI 系统的输出
     * @param options 评估选项
     * @return 评估结果
     */
    suspend fun evaluate(
        input: I,
        output: O,
        options: Map<String, Any?> = emptyMap()
    ): EvaluationResult
}

/**
 * 评估器构建器，用于创建评估器。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
class EvaluatorBuilder<I, O> {
    var name: String = ""
    var description: String = ""
    private var evaluateFunction: (suspend (I, O, Map<String, Any?>) -> EvaluationResult)? = null

    /**
     * 设置评估函数。
     *
     * @param block 评估函数
     */
    fun evaluate(block: suspend (I, O, Map<String, Any?>) -> EvaluationResult) {
        evaluateFunction = block
    }

    /**
     * 构建评估器。
     *
     * @return 评估器实例
     */
    fun build(): Evaluator<I, O> {
        require(name.isNotEmpty()) { "Evaluator name must not be empty" }
        requireNotNull(evaluateFunction) { "Evaluate function must be provided" }

        val evalFn = evaluateFunction!!

        return object : Evaluator<I, O> {
            override val name: String = this@EvaluatorBuilder.name
            override val description: String = this@EvaluatorBuilder.description

            override suspend fun evaluate(
                input: I,
                output: O,
                options: Map<String, Any?>
            ): EvaluationResult {
                return evalFn(input, output, options)
            }
        }
    }
}

/**
 * 创建评估器的 DSL 函数。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @param init 初始化块
 * @return 评估器实例
 */
fun <I, O> evaluator(init: EvaluatorBuilder<I, O>.() -> Unit): Evaluator<I, O> {
    val builder = EvaluatorBuilder<I, O>()
    builder.init()
    return builder.build()
}
