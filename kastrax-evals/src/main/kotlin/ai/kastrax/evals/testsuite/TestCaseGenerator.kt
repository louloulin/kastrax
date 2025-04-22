package ai.kastrax.evals.testsuite

import ai.kastrax.evals.metrics.Metric
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val logger = KotlinLogging.logger {}

/**
 * 测试用例生成器接口，用于生成测试用例。
 *
 * @param I 输入类型
 * @param O 输出类型
 */
interface TestCaseGenerator<I, O> {
    /**
     * 生成测试用例。
     *
     * @param count 生成的测试用例数量
     * @param metrics 评估指标列表
     * @param options 生成选项
     * @return 测试用例列表
     */
    suspend fun generate(
        count: Int,
        metrics: List<Metric<I, O>>,
        options: Map<String, Any?> = emptyMap()
    ): List<TestCase<I, O>>
}

/**
 * 从 JSON 文件加载测试用例。
 *
 * @param file JSON 文件
 * @param metrics 评估指标列表
 * @return 测试用例列表
 */
fun <I, O> loadTestCasesFromJson(
    file: File,
    metrics: List<Metric<I, O>>,
    inputConverter: (String) -> I,
    outputConverter: (String) -> O
): List<TestCase<I, O>> {
    logger.info { "Loading test cases from JSON file: ${file.absolutePath}" }
    
    val json = Json { ignoreUnknownKeys = true }
    val jsonString = file.readText()
    val jsonArray = json.parseToJsonElement(jsonString).jsonArray
    
    return jsonArray.mapIndexed { index, element ->
        val jsonObject = element.jsonObject
        
        val id = jsonObject["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()
        val name = jsonObject["name"]?.jsonPrimitive?.content ?: "Test Case ${index + 1}"
        val description = jsonObject["description"]?.jsonPrimitive?.content ?: ""
        
        val tags = jsonObject["tags"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet() ?: emptySet()
        
        val inputString = jsonObject["input"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Test case input is required")
        
        val input = inputConverter(inputString)
        
        val expectedOutputString = jsonObject["expectedOutput"]?.jsonPrimitive?.content
        val expectedOutput = expectedOutputString?.let { outputConverter(it) }
        
        val options = jsonObject["options"]?.jsonObject?.mapValues { (_, value) ->
            when (value) {
                is JsonPrimitive -> {
                    when {
                        value.isString -> value.content
                        value.content.toIntOrNull() != null -> value.content.toInt()
                        value.content.toDoubleOrNull() != null -> value.content.toDouble()
                        value.content == "true" -> true
                        value.content == "false" -> false
                        else -> value.content
                    }
                }
                is JsonArray -> value.jsonArray.map { it.jsonPrimitive.content }
                is JsonObject -> value.jsonObject.mapValues { it.value.jsonPrimitive.content }
                else -> null
            }
        } ?: emptyMap()
        
        val threshold = jsonObject["threshold"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.7
        
        BasicTestCase(
            id = id,
            name = name,
            description = description,
            tags = tags,
            input = input,
            expectedOutput = expectedOutput,
            metrics = metrics,
            options = options,
            threshold = threshold
        )
    }
}

/**
 * 从 CSV 文件加载测试用例。
 *
 * @param file CSV 文件
 * @param metrics 评估指标列表
 * @param delimiter 分隔符
 * @param hasHeader 是否有标题行
 * @return 测试用例列表
 */
fun <I, O> loadTestCasesFromCsv(
    file: File,
    metrics: List<Metric<I, O>>,
    inputConverter: (String) -> I,
    outputConverter: (String) -> O,
    delimiter: String = ",",
    hasHeader: Boolean = true
): List<TestCase<I, O>> {
    logger.info { "Loading test cases from CSV file: ${file.absolutePath}" }
    
    val lines = file.readLines()
    val dataLines = if (hasHeader) lines.drop(1) else lines
    
    return dataLines.mapIndexed { index, line ->
        val fields = line.split(delimiter)
        
        if (fields.size < 2) {
            throw IllegalArgumentException("CSV file must have at least 2 columns: input and expected output")
        }
        
        val input = inputConverter(fields[0])
        val expectedOutput = if (fields.size > 1 && fields[1].isNotBlank()) {
            outputConverter(fields[1])
        } else {
            null
        }
        
        BasicTestCase(
            id = UUID.randomUUID().toString(),
            name = "Test Case ${index + 1}",
            description = "",
            tags = emptySet(),
            input = input,
            expectedOutput = expectedOutput,
            metrics = metrics,
            options = emptyMap(),
            threshold = 0.7
        )
    }
}

/**
 * LLM 测试用例生成器，使用 LLM 生成测试用例。
 *
 * @param I 输入类型
 * @param O 输出类型
 * @property llmClient LLM 客户端
 * @property systemPrompt 系统提示
 * @property inputConverter 输入转换器
 * @property outputConverter 输出转换器
 */
class LlmTestCaseGenerator<I, O>(
    private val llmClient: ai.kastrax.evals.metrics.llm.LlmClient,
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    private val inputConverter: (String) -> I,
    private val outputConverter: (String) -> O
) : TestCaseGenerator<I, O> {
    
    override suspend fun generate(
        count: Int,
        metrics: List<Metric<I, O>>,
        options: Map<String, Any?>
    ): List<TestCase<I, O>> {
        logger.info { "Generating $count test cases using LLM" }
        
        val domain = options["domain"] as? String ?: "general"
        val complexity = options["complexity"] as? String ?: "medium"
        val format = options["format"] as? String ?: "json"
        
        val prompt = buildPrompt(count, domain, complexity, format)
        
        try {
            val response = llmClient.generate(systemPrompt, prompt)
            
            return parseResponse(response, metrics)
        } catch (e: Exception) {
            logger.error(e) { "Error generating test cases using LLM" }
            return emptyList()
        }
    }
    
    /**
     * 构建提示。
     *
     * @param count 生成的测试用例数量
     * @param domain 领域
     * @param complexity 复杂度
     * @param format 格式
     * @return 提示
     */
    private fun buildPrompt(
        count: Int,
        domain: String,
        complexity: String,
        format: String
    ): String {
        return """
        请生成 $count 个测试用例，用于评估 AI 系统的性能。

        领域：$domain
        复杂度：$complexity
        格式：$format

        每个测试用例应包含以下信息：
        - 输入：用户的查询或问题
        - 期望输出：AI 系统应该生成的理想回答
        - 描述：测试用例的简短描述
        - 标签：与测试用例相关的标签（可选）

        请确保测试用例多样化，覆盖不同的场景和边缘情况。
        """.trimIndent()
    }
    
    /**
     * 解析 LLM 的响应。
     *
     * @param response LLM 的响应
     * @param metrics 评估指标列表
     * @return 测试用例列表
     */
    private fun parseResponse(
        response: String,
        metrics: List<Metric<I, O>>
    ): List<TestCase<I, O>> {
        // 尝试解析为 JSON 格式
        try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonElement = json.parseToJsonElement(response)
            
            if (jsonElement is JsonArray) {
                return jsonElement.mapIndexed { index, element ->
                    val jsonObject = element.jsonObject
                    
                    val input = jsonObject["input"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Test case input is required")
                    
                    val expectedOutput = jsonObject["expectedOutput"]?.jsonPrimitive?.content
                    
                    val description = jsonObject["description"]?.jsonPrimitive?.content ?: ""
                    
                    val tags = jsonObject["tags"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet() ?: emptySet()
                    
                    BasicTestCase(
                        id = UUID.randomUUID().toString(),
                        name = "Test Case ${index + 1}",
                        description = description,
                        tags = tags,
                        input = inputConverter(input),
                        expectedOutput = expectedOutput?.let { outputConverter(it) },
                        metrics = metrics,
                        options = emptyMap(),
                        threshold = 0.7
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse LLM response as JSON" }
        }
        
        // 如果 JSON 解析失败，尝试解析为文本格式
        val testCases = mutableListOf<TestCase<I, O>>()
        val lines = response.lines()
        
        var currentInput: String? = null
        var currentExpectedOutput: String? = null
        var currentDescription: String? = null
        var currentTags: Set<String> = emptySet()
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            if (trimmedLine.startsWith("输入：") || trimmedLine.startsWith("Input:")) {
                // 如果已经有输入，则创建一个测试用例
                if (currentInput != null) {
                    testCases.add(
                        BasicTestCase(
                            id = UUID.randomUUID().toString(),
                            name = "Test Case ${testCases.size + 1}",
                            description = currentDescription ?: "",
                            tags = currentTags,
                            input = inputConverter(currentInput),
                            expectedOutput = currentExpectedOutput?.let { outputConverter(it) },
                            metrics = metrics,
                            options = emptyMap(),
                            threshold = 0.7
                        )
                    )
                }
                
                currentInput = trimmedLine.substringAfter(":").trim()
                currentExpectedOutput = null
                currentDescription = null
                currentTags = emptySet()
            } else if (trimmedLine.startsWith("期望输出：") || trimmedLine.startsWith("Expected Output:")) {
                currentExpectedOutput = trimmedLine.substringAfter(":").trim()
            } else if (trimmedLine.startsWith("描述：") || trimmedLine.startsWith("Description:")) {
                currentDescription = trimmedLine.substringAfter(":").trim()
            } else if (trimmedLine.startsWith("标签：") || trimmedLine.startsWith("Tags:")) {
                currentTags = trimmedLine.substringAfter(":").trim().split(",").map { it.trim() }.toSet()
            }
        }
        
        // 添加最后一个测试用例
        if (currentInput != null) {
            testCases.add(
                BasicTestCase(
                    id = UUID.randomUUID().toString(),
                    name = "Test Case ${testCases.size + 1}",
                    description = currentDescription ?: "",
                    tags = currentTags,
                    input = inputConverter(currentInput),
                    expectedOutput = currentExpectedOutput?.let { outputConverter(it) },
                    metrics = metrics,
                    options = emptyMap(),
                    threshold = 0.7
                )
            )
        }
        
        return testCases
    }
    
    companion object {
        /**
         * 默认系统提示。
         */
        const val DEFAULT_SYSTEM_PROMPT = """
            你是一个专业的测试用例生成器。你的任务是生成高质量的测试用例，用于评估 AI 系统的性能。
            请确保测试用例多样化，覆盖不同的场景和边缘情况。
            每个测试用例应包含输入、期望输出、描述和标签。
        """
    }
}
