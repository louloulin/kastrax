package ai.kastrax.evals.evaluators

import ai.kastrax.evals.Evaluator
import ai.kastrax.evals.EvaluationResult
import ai.kastrax.evals.evaluator
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 创建精确匹配评估器，用于评估输出是否与预期输出完全匹配。
 *
 * @return 精确匹配评估器
 */
fun exactMatchEvaluator(): Evaluator<String, String> = evaluator {
    name = "ExactMatch"
    description = "评估输出是否与预期输出完全匹配"

    evaluate { input, output, options ->
        val expected = options["expected"] as? String ?: input
        val ignoreCase = options["ignoreCase"] as? Boolean ?: false
        val ignoreWhitespace = options["ignoreWhitespace"] as? Boolean ?: false

        var processedExpected = expected
        var processedOutput = output

        if (ignoreWhitespace) {
            processedExpected = processedExpected.trim().replace("\\s+".toRegex(), " ")
            processedOutput = processedOutput.trim().replace("\\s+".toRegex(), " ")
        }

        val isMatch = if (ignoreCase) {
            processedExpected.equals(processedOutput, ignoreCase = true)
        } else {
            processedExpected == processedOutput
        }

        val score = if (isMatch) 1.0 else 0.0
        val details = mapOf(
            "expected" to processedExpected,
            "actual" to processedOutput,
            "isMatch" to isMatch
        )

        EvaluationResult(score, details)
    }
}

/**
 * 创建包含关键词评估器，用于评估输出是否包含所有指定的关键词。
 *
 * @return 包含关键词评估器
 */
fun containsKeywordsEvaluator(): Evaluator<String, String> = evaluator {
    name = "ContainsKeywords"
    description = "评估输出是否包含所有指定的关键词"

    evaluate { input, output, options ->
        @Suppress("UNCHECKED_CAST")
        val keywords = options["keywords"] as? List<String> ?: emptyList()
        val ignoreCase = options["ignoreCase"] as? Boolean ?: true
        val requireAll = options["requireAll"] as? Boolean ?: true

        if (keywords.isEmpty()) {
            return@evaluate EvaluationResult.failure(
                mapOf("error" to "No keywords provided")
            )
        }

        val processedOutput = if (ignoreCase) output.lowercase() else output
        val processedKeywords = if (ignoreCase) {
            keywords.map { it.lowercase() }
        } else {
            keywords
        }

        val matchedKeywords = processedKeywords.filter { keyword ->
            processedOutput.contains(keyword)
        }

        val score = if (requireAll) {
            if (matchedKeywords.size == keywords.size) 1.0 else 0.0
        } else {
            matchedKeywords.size.toDouble() / keywords.size
        }

        val details = mapOf(
            "keywords" to keywords,
            "matchedKeywords" to matchedKeywords,
            "matchedCount" to matchedKeywords.size,
            "totalKeywords" to keywords.size
        )

        EvaluationResult(score, details)
    }
}

/**
 * 创建正则表达式匹配评估器，用于评估输出是否匹配指定的正则表达式。
 *
 * @return 正则表达式匹配评估器
 */
fun regexMatchEvaluator(): Evaluator<String, String> = evaluator {
    name = "RegexMatch"
    description = "评估输出是否匹配指定的正则表达式"

    evaluate { input, output, options ->
        val pattern = options["pattern"] as? String
            ?: throw IllegalArgumentException("Pattern must be provided")
        
        val regex = try {
            Regex(pattern)
        } catch (e: Exception) {
            logger.error(e) { "Invalid regex pattern: $pattern" }
            return@evaluate EvaluationResult.failure(
                mapOf("error" to "Invalid regex pattern: ${e.message}")
            )
        }

        val isMatch = regex.containsMatchIn(output)
        val matches = regex.findAll(output).map { it.value }.toList()

        val score = if (isMatch) 1.0 else 0.0
        val details = mapOf(
            "pattern" to pattern,
            "isMatch" to isMatch,
            "matches" to matches
        )

        EvaluationResult(score, details)
    }
}

/**
 * 创建相似度评估器，用于评估输出与预期输出的相似度。
 *
 * @return 相似度评估器
 */
fun similarityEvaluator(): Evaluator<String, String> = evaluator {
    name = "Similarity"
    description = "评估输出与预期输出的相似度"

    evaluate { input, output, options ->
        val expected = options["expected"] as? String ?: input
        val method = options["method"] as? String ?: "jaccard"

        val similarity = when (method.lowercase()) {
            "jaccard" -> jaccardSimilarity(expected, output)
            "levenshtein" -> levenshteinSimilarity(expected, output)
            else -> throw IllegalArgumentException("Unsupported similarity method: $method")
        }

        val details = mapOf(
            "expected" to expected,
            "actual" to output,
            "method" to method,
            "similarity" to similarity
        )

        EvaluationResult(similarity, details)
    }
}

/**
 * 计算两个字符串的 Jaccard 相似度。
 *
 * @param s1 第一个字符串
 * @param s2 第二个字符串
 * @return Jaccard 相似度，范围为 [0, 1]
 */
private fun jaccardSimilarity(s1: String, s2: String): Double {
    val words1 = s1.split("\\s+".toRegex()).toSet()
    val words2 = s2.split("\\s+".toRegex()).toSet()

    val intersection = words1.intersect(words2)
    val union = words1.union(words2)

    return if (union.isEmpty()) 0.0 else intersection.size.toDouble() / union.size
}

/**
 * 计算两个字符串的 Levenshtein 相似度。
 *
 * @param s1 第一个字符串
 * @param s2 第二个字符串
 * @return Levenshtein 相似度，范围为 [0, 1]
 */
private fun levenshteinSimilarity(s1: String, s2: String): Double {
    val distance = levenshteinDistance(s1, s2)
    val maxLength = maxOf(s1.length, s2.length)
    return if (maxLength == 0) 1.0 else 1.0 - (distance.toDouble() / maxLength)
}

/**
 * 计算两个字符串的 Levenshtein 距离。
 *
 * @param s1 第一个字符串
 * @param s2 第二个字符串
 * @return Levenshtein 距离
 */
private fun levenshteinDistance(s1: String, s2: String): Int {
    val m = s1.length
    val n = s2.length
    val dp = Array(m + 1) { IntArray(n + 1) }

    for (i in 0..m) {
        dp[i][0] = i
    }

    for (j in 0..n) {
        dp[0][j] = j
    }

    for (i in 1..m) {
        for (j in 1..n) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,      // 删除
                dp[i][j - 1] + 1,      // 插入
                dp[i - 1][j - 1] + cost // 替换
            )
        }
    }

    return dp[m][n]
}
