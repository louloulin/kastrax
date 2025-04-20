package ai.kastrax.evals.evaluators

import ai.kastrax.core.agent.Agent
import ai.kastrax.evals.Evaluator
import ai.kastrax.evals.EvaluationResult
import ai.kastrax.evals.evaluator
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 创建 AI 评估器，使用 AI 代理评估输出的质量。
 *
 * @param agent AI 代理
 * @return AI 评估器
 */
fun aiEvaluator(agent: Agent): Evaluator<String, String> = evaluator {
    name = "AIEvaluator"
    description = "使用 AI 代理评估输出的质量"

    evaluate { input, output, options ->
        val criteria = options["criteria"] as? String ?: "质量、相关性和准确性"
        val scoreOnly = options["scoreOnly"] as? Boolean ?: false
        val rubric = options["rubric"] as? String

        val prompt = buildString {
            append("你是一个专业的评估专家。请评估以下输出的质量。\n\n")
            append("输入：\n$input\n\n")
            append("输出：\n$output\n\n")
            
            if (rubric != null) {
                append("评分标准：\n$rubric\n\n")
            } else {
                append("请根据以下标准评估输出：$criteria\n\n")
            }
            
            if (scoreOnly) {
                append("请只给出一个 0 到 1 之间的分数，表示输出的质量。")
            } else {
                append("请给出详细的评估，并在最后给出一个 0 到 1 之间的分数，格式为：\"分数：X.X\"。")
            }
        }

        val response = agent.generate(prompt)
        val responseText = response.text

        // 尝试从响应中提取分数
        val score = try {
            if (scoreOnly) {
                responseText.trim().toDoubleOrNull()
            } else {
                val scoreRegex = "分数：(\\d+(\\.\\d+)?)".toRegex()
                val matchResult = scoreRegex.find(responseText)
                matchResult?.groupValues?.get(1)?.toDoubleOrNull()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error extracting score from AI response" }
            null
        }

        // 如果无法提取分数，使用默认值 0.5
        val finalScore = score?.coerceIn(0.0, 1.0) ?: 0.5

        val details = mapOf(
            "criteria" to criteria,
            "evaluation" to responseText,
            "extractedScore" to score
        )

        EvaluationResult(finalScore, details)
    }
}

/**
 * 创建 AI 对比评估器，使用 AI 代理比较两个输出的质量。
 *
 * @param agent AI 代理
 * @return AI 对比评估器
 */
fun aiComparisonEvaluator(agent: Agent): Evaluator<String, Pair<String, String>> = evaluator {
    name = "AIComparisonEvaluator"
    description = "使用 AI 代理比较两个输出的质量"

    evaluate { input, outputs, options ->
        val (output1, output2) = outputs
        val criteria = options["criteria"] as? String ?: "质量、相关性和准确性"
        val preferenceOnly = options["preferenceOnly"] as? Boolean ?: false

        val prompt = buildString {
            append("你是一个专业的评估专家。请比较以下两个输出的质量。\n\n")
            append("输入：\n$input\n\n")
            append("输出 A：\n$output1\n\n")
            append("输出 B：\n$output2\n\n")
            append("请根据以下标准评估这两个输出：$criteria\n\n")
            
            if (preferenceOnly) {
                append("请只回答 A 或 B，表示你认为哪个输出更好。")
            } else {
                append("请给出详细的比较，并在最后明确指出你认为哪个输出更好（A 或 B）。")
            }
        }

        val response = agent.generate(prompt)
        val responseText = response.text

        // 尝试从响应中提取偏好
        val preference = try {
            if (preferenceOnly) {
                responseText.trim().uppercase()
            } else {
                val preferenceRegex = "(?:选择|偏好|更好|更优)[：:]*\\s*([AB])".toRegex()
                val matchResult = preferenceRegex.find(responseText)
                matchResult?.groupValues?.get(1)?.uppercase()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error extracting preference from AI response" }
            null
        }

        // 根据偏好计算分数
        val score = when (preference) {
            "A" -> 1.0  // 输出 1 更好
            "B" -> 0.0  // 输出 2 更好
            else -> 0.5 // 无法确定
        }

        val details = mapOf(
            "criteria" to criteria,
            "comparison" to responseText,
            "preference" to preference
        )

        EvaluationResult(score, details)
    }
}

/**
 * 创建 AI 分类评估器，使用 AI 代理对输出进行分类。
 *
 * @param agent AI 代理
 * @return AI 分类评估器
 */
fun aiClassificationEvaluator(agent: Agent): Evaluator<String, String> = evaluator {
    name = "AIClassificationEvaluator"
    description = "使用 AI 代理对输出进行分类"

    evaluate { input, output, options ->
        @Suppress("UNCHECKED_CAST")
        val categories = options["categories"] as? List<String>
            ?: throw IllegalArgumentException("Categories must be provided")
        
        val categoriesStr = categories.joinToString(", ")
        val labelOnly = options["labelOnly"] as? Boolean ?: false

        val prompt = buildString {
            append("你是一个专业的分类专家。请将以下输出分类到指定的类别中。\n\n")
            append("输入：\n$input\n\n")
            append("输出：\n$output\n\n")
            append("可选类别：$categoriesStr\n\n")
            
            if (labelOnly) {
                append("请只回答一个类别名称，表示你认为输出属于哪个类别。")
            } else {
                append("请给出详细的分析，并在最后明确指出你认为输出属于哪个类别。")
            }
        }

        val response = agent.generate(prompt)
        val responseText = response.text

        // 尝试从响应中提取类别
        val category = try {
            if (labelOnly) {
                responseText.trim()
            } else {
                // 尝试找到最后提到的类别
                categories.lastOrNull { category ->
                    responseText.contains(category, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error extracting category from AI response" }
            null
        }

        // 检查提取的类别是否在预定义的类别列表中
        val validCategory = category?.let { 
            categories.find { it.equals(category, ignoreCase = true) } 
        }

        // 如果找到有效类别，则评估成功；否则失败
        val score = if (validCategory != null) 1.0 else 0.0

        val details = mapOf(
            "categories" to categories,
            "analysis" to responseText,
            "extractedCategory" to category,
            "validCategory" to validCategory
        )

        EvaluationResult(score, details)
    }
}
