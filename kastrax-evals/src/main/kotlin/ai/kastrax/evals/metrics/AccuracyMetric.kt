package ai.kastrax.evals.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * 准确性指标，用于评估输出的准确性。
 *
 * 支持多种准确性评估方法：
 * - 精确匹配：输出必须与期望输出完全匹配
 * - 部分匹配：输出包含期望输出的一部分
 * - 相似度匹配：输出与期望输出的相似度达到一定阈值
 *
 * @param method 准确性评估方法
 * @param threshold 相似度阈值，仅在使用相似度匹配时有效
 * @param ignoreCase 是否忽略大小写
 * @param ignoreWhitespace 是否忽略空白字符
 */
class AccuracyMetric(
    private val method: AccuracyMethod = AccuracyMethod.EXACT_MATCH,
    private val threshold: Double = 0.8,
    private val ignoreCase: Boolean = true,
    private val ignoreWhitespace: Boolean = true
) : Metric<String, String> {
    
    override val name: String = "Accuracy"
    override val description: String = "Evaluates the accuracy of the output compared to the expected output"
    override val category: MetricCategory = MetricCategory.ACCURACY
    
    override suspend fun calculate(
        input: String,
        output: String,
        options: Map<String, Any?>
    ): MetricResult {
        // 获取期望输出
        val expectedOutput = options["expected"] as? String
            ?: throw IllegalArgumentException("Expected output must be provided for accuracy evaluation")
        
        // 预处理文本
        val processedOutput = preprocessText(output)
        val processedExpected = preprocessText(expectedOutput)
        
        // 根据方法计算准确性
        val score = when (method) {
            AccuracyMethod.EXACT_MATCH -> calculateExactMatch(processedOutput, processedExpected)
            AccuracyMethod.PARTIAL_MATCH -> calculatePartialMatch(processedOutput, processedExpected)
            AccuracyMethod.SIMILARITY -> calculateSimilarity(processedOutput, processedExpected)
        }
        
        return MetricResult(
            score = score,
            details = mapOf(
                "method" to method.name,
                "threshold" to threshold,
                "ignoreCase" to ignoreCase,
                "ignoreWhitespace" to ignoreWhitespace,
                "expectedOutput" to expectedOutput,
                "actualOutput" to output
            )
        )
    }
    
    /**
     * 预处理文本。
     *
     * @param text 原始文本
     * @return 预处理后的文本
     */
    private fun preprocessText(text: String): String {
        var processed = text
        
        if (ignoreWhitespace) {
            processed = processed.trim().replace(Regex("\\s+"), " ")
        }
        
        if (ignoreCase) {
            processed = processed.lowercase()
        }
        
        return processed
    }
    
    /**
     * 计算精确匹配的准确性。
     *
     * @param output 输出文本
     * @param expected 期望文本
     * @return 准确性分数，1.0 表示完全匹配，0.0 表示不匹配
     */
    private fun calculateExactMatch(output: String, expected: String): Double {
        return if (output == expected) 1.0 else 0.0
    }
    
    /**
     * 计算部分匹配的准确性。
     *
     * @param output 输出文本
     * @param expected 期望文本
     * @return 准确性分数，范围为 0.0 到 1.0
     */
    private fun calculatePartialMatch(output: String, expected: String): Double {
        // 如果期望输出为空，则返回 1.0
        if (expected.isEmpty()) {
            return 1.0
        }
        
        // 如果输出包含期望输出，则返回 1.0
        if (output.contains(expected)) {
            return 1.0
        }
        
        // 计算最长公共子序列的长度
        val lcsLength = longestCommonSubsequence(output, expected)
        
        // 计算部分匹配分数
        return lcsLength.toDouble() / expected.length
    }
    
    /**
     * 计算相似度匹配的准确性。
     *
     * @param output 输出文本
     * @param expected 期望文本
     * @return 准确性分数，范围为 0.0 到 1.0
     */
    private fun calculateSimilarity(output: String, expected: String): Double {
        // 计算 Jaccard 相似度
        val similarity = jaccardSimilarity(output, expected)
        
        // 如果相似度大于等于阈值，则返回 1.0，否则返回相似度
        return if (similarity >= threshold) 1.0 else similarity
    }
    
    /**
     * 计算最长公共子序列的长度。
     *
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @return 最长公共子序列的长度
     */
    private fun longestCommonSubsequence(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1] + 1
                } else {
                    max(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }
        
        return dp[m][n]
    }
    
    /**
     * 计算 Jaccard 相似度。
     *
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @return Jaccard 相似度，范围为 0.0 到 1.0
     */
    private fun jaccardSimilarity(a: String, b: String): Double {
        // 将字符串分割为单词
        val wordsA = a.split(Regex("\\s+")).toSet()
        val wordsB = b.split(Regex("\\s+")).toSet()
        
        // 计算交集和并集
        val intersection = wordsA.intersect(wordsB)
        val union = wordsA.union(wordsB)
        
        // 如果并集为空，则返回 1.0
        if (union.isEmpty()) {
            return 1.0
        }
        
        // 计算 Jaccard 相似度
        return intersection.size.toDouble() / union.size
    }
}

/**
 * 准确性评估方法。
 */
enum class AccuracyMethod {
    /**
     * 精确匹配，输出必须与期望输出完全匹配。
     */
    EXACT_MATCH,
    
    /**
     * 部分匹配，输出包含期望输出的一部分。
     */
    PARTIAL_MATCH,
    
    /**
     * 相似度匹配，输出与期望输出的相似度达到一定阈值。
     */
    SIMILARITY
}

/**
 * 创建准确性指标。
 *
 * @param method 准确性评估方法
 * @param threshold 相似度阈值，仅在使用相似度匹配时有效
 * @param ignoreCase 是否忽略大小写
 * @param ignoreWhitespace 是否忽略空白字符
 * @return 准确性指标
 */
fun accuracyMetric(
    method: AccuracyMethod = AccuracyMethod.EXACT_MATCH,
    threshold: Double = 0.8,
    ignoreCase: Boolean = true,
    ignoreWhitespace: Boolean = true
): AccuracyMetric {
    return AccuracyMetric(method, threshold, ignoreCase, ignoreWhitespace)
}
