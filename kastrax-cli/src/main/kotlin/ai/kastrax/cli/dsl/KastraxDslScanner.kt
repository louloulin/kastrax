package ai.kastrax.cli.dsl

import java.io.File
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import ai.kastrax.cli.api.ApiDefinition
import ai.kastrax.cli.api.ApiParameter
import ai.kastrax.cli.api.HttpMethod

private val logger = KotlinLogging.logger {}

/**
 * KastraX DSL 扫描器，用于解析 KastraX DSL 并提取 API 定义。
 */
class KastraxDslScanner(private val sourceDir: File) {

    /**
     * 扫描源代码目录并提取 API 定义。
     */
    fun scan(): List<ApiDefinition> {
        logger.info { "开始扫描 KastraX DSL: ${sourceDir.absolutePath}" }

        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 遍历所有 Kotlin 文件
        Files.walk(sourceDir.toPath())
            .filter { it.isRegularFile() && it.extension == "kt" }
            .forEach { file ->
                try {
                    val content = file.readText()
                    apiDefinitions.addAll(extractApiDefinitionsFromFile(file, content))
                } catch (e: Exception) {
                    logger.error(e) { "解析文件时发生错误: $file" }
                }
            }

        logger.info { "扫描完成，发现 ${apiDefinitions.size} 个 API 定义" }
        return apiDefinitions
    }

    /**
     * 从单个文件中提取 API 定义。
     */
    private fun extractApiDefinitionsFromFile(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 提取代理定义
        apiDefinitions.addAll(extractAgentApiDefinitions(file, content))

        // 提取工具定义
        apiDefinitions.addAll(extractToolApiDefinitions(file, content))

        // 提取 ZodTool 定义
        apiDefinitions.addAll(extractZodToolApiDefinitions(file, content))

        // 提取高级工具定义
        apiDefinitions.addAll(extractAdvancedToolApiDefinitions(file, content))

        // 提取专业工具定义
        apiDefinitions.addAll(extractSpecializedToolApiDefinitions(file, content))

        // 提取工作流定义
        apiDefinitions.addAll(extractWorkflowApiDefinitions(file, content))

        // 提取代理网络定义
        apiDefinitions.addAll(extractNetworkApiDefinitions(file, content))

        return apiDefinitions
    }

    /**
     * 从代理定义中提取 API。
     */
    private fun extractAgentApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 agent { ... } 块
        val agentPattern = """agent\s*\(\s*["']([^"']+)["']\s*\)\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = agentPattern.findAll(content)

        for (match in matches) {
            val agentId = match.groupValues[1]
            val agentBody = match.groupValues[2]

            logger.debug { "找到代理定义: $agentId" }

            // 为代理生成标准 API 端点
            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/agents/$agentId",
                    method = HttpMethod.GET,
                    description = "获取代理 $agentId 的详细信息",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/agents/$agentId/generate",
                    method = HttpMethod.POST,
                    description = "使用代理 $agentId 生成响应",
                    parameters = listOf(
                        ApiParameter("messages", "array", "消息列表", true),
                        ApiParameter("threadId", "string", "会话 ID", false),
                        ApiParameter("resourceId", "string", "资源 ID", false)
                    ),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/agents/$agentId/stream",
                    method = HttpMethod.POST,
                    description = "使用代理 $agentId 流式生成响应",
                    parameters = listOf(
                        ApiParameter("messages", "array", "消息列表", true),
                        ApiParameter("threadId", "string", "会话 ID", false),
                        ApiParameter("resourceId", "string", "资源 ID", false)
                    ),
                    sourceFile = file.toString()
                )
            )
        }

        return apiDefinitions
    }

    /**
     * 从工具定义中提取 API。
     */
    private fun extractToolApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 tool { ... } 块
        val toolPattern = """tool\s*\(\s*["']([^"']+)["']\s*\)\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = toolPattern.findAll(content)

        for (match in matches) {
            val toolId = match.groupValues[1]
            val toolBody = match.groupValues[2]

            logger.debug { "找到工具定义: $toolId" }

            // 为工具生成标准 API 端点
            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/$toolId",
                    method = HttpMethod.GET,
                    description = "获取工具 $toolId 的详细信息",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/$toolId/execute",
                    method = HttpMethod.POST,
                    description = "执行工具 $toolId",
                    parameters = listOf(
                        ApiParameter("data", "object", "工具输入数据", true)
                    ),
                    sourceFile = file.toString()
                )
            )
        }

        return apiDefinitions
    }

    /**
     * 从 ZodTool 定义中提取 API。
     */
    private fun extractZodToolApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 zodTool { ... } 块
        val zodToolPattern = """zodTool\s*<[^>]*>\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = zodToolPattern.findAll(content)

        for (match in matches) {
            val toolBody = match.groupValues[1]

            // 提取工具 ID
            val idPattern = """id\s*=\s*["']([^"']+)["']""".toRegex()
            val idMatch = idPattern.find(toolBody)

            if (idMatch != null) {
                val toolId = idMatch.groupValues[1]

                logger.debug { "找到 ZodTool 定义: $toolId" }

                // 提取工具描述
                val descriptionPattern = """description\s*=\s*["']([^"']+)["']""".toRegex()
                val descriptionMatch = descriptionPattern.find(toolBody)
                val description = descriptionMatch?.groupValues?.get(1) ?: "ZodTool $toolId"

                // 为 ZodTool 生成标准 API 端点
                apiDefinitions.add(
                    ApiDefinition(
                        path = "/api/zodtools/$toolId",
                        method = HttpMethod.GET,
                        description = "获取 ZodTool $toolId 的详细信息",
                        parameters = emptyList(),
                        sourceFile = file.toString()
                    )
                )

                apiDefinitions.add(
                    ApiDefinition(
                        path = "/api/zodtools/$toolId/schema",
                        method = HttpMethod.GET,
                        description = "获取 ZodTool $toolId 的输入输出模式",
                        parameters = emptyList(),
                        sourceFile = file.toString()
                    )
                )

                apiDefinitions.add(
                    ApiDefinition(
                        path = "/api/zodtools/$toolId/execute",
                        method = HttpMethod.POST,
                        description = "执行 ZodTool $toolId",
                        parameters = listOf(
                            ApiParameter("input", "object", "工具输入数据", true),
                            ApiParameter("threadId", "string", "会话 ID", false),
                            ApiParameter("resourceId", "string", "资源 ID", false)
                        ),
                        sourceFile = file.toString()
                    )
                )

                apiDefinitions.add(
                    ApiDefinition(
                        path = "/api/zodtools/$toolId/validate",
                        method = HttpMethod.POST,
                        description = "验证 ZodTool $toolId 的输入数据",
                        parameters = listOf(
                            ApiParameter("input", "object", "要验证的输入数据", true)
                        ),
                        sourceFile = file.toString()
                    )
                )
            }
        }

        // 使用正则表达式匹配 zodToolAsLegacy { ... } 块
        val zodToolAsLegacyPattern = """zodToolAsLegacy\s*<[^>]*>\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val legacyMatches = zodToolAsLegacyPattern.findAll(content)

        for (match in legacyMatches) {
            val toolBody = match.groupValues[1]

            // 提取工具 ID
            val idPattern = """id\s*=\s*["']([^"']+)["']""".toRegex()
            val idMatch = idPattern.find(toolBody)

            if (idMatch != null) {
                val toolId = idMatch.groupValues[1]

                logger.debug { "找到 ZodToolAsLegacy 定义: $toolId" }

                // 为 ZodToolAsLegacy 生成标准 API 端点（与普通工具相同）
                apiDefinitions.add(
                    ApiDefinition(
                        path = "/api/tools/$toolId",
                        method = HttpMethod.GET,
                        description = "获取工具 $toolId 的详细信息",
                        parameters = emptyList(),
                        sourceFile = file.toString()
                    )
                )

                apiDefinitions.add(
                    ApiDefinition(
                        path = "/api/tools/$toolId/execute",
                        method = HttpMethod.POST,
                        description = "执行工具 $toolId",
                        parameters = listOf(
                            ApiParameter("data", "object", "工具输入数据", true)
                        ),
                        sourceFile = file.toString()
                    )
                )
            }
        }

        return apiDefinitions
    }

    /**
     * 从高级工具定义中提取 API。
     */
    private fun extractAdvancedToolApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 提取复合工具定义
        apiDefinitions.addAll(extractCompositeToolApiDefinitions(file, content))

        // 提取链式工具定义
        apiDefinitions.addAll(extractChainToolApiDefinitions(file, content))

        // 提取并行工具定义
        apiDefinitions.addAll(extractParallelToolApiDefinitions(file, content))

        // 提取条件工具定义
        apiDefinitions.addAll(extractConditionalToolApiDefinitions(file, content))

        return apiDefinitions
    }

    /**
     * 从复合工具定义中提取 API。
     */
    private fun extractCompositeToolApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 compositeTool { ... } 块
        val compositeToolPattern = """compositeTool\s*\(\s*["']([^"']+)["']\s*\)\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = compositeToolPattern.findAll(content)

        for (match in matches) {
            val toolId = match.groupValues[1]
            val toolBody = match.groupValues[2]

            logger.debug { "找到复合工具定义: $toolId" }

            // 为复合工具生成标准 API 端点
            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/composite/$toolId",
                    method = HttpMethod.GET,
                    description = "获取复合工具 $toolId 的详细信息",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/composite/$toolId/execute",
                    method = HttpMethod.POST,
                    description = "执行复合工具 $toolId",
                    parameters = listOf(
                        ApiParameter("data", "object", "工具输入数据", true)
                    ),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/composite/$toolId/components",
                    method = HttpMethod.GET,
                    description = "获取复合工具 $toolId 的组件工具",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )
        }

        return apiDefinitions
    }

    /**
     * 从链式工具定义中提取 API。
     */
    private fun extractChainToolApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 chainTool { ... } 块
        val chainToolPattern = """chainTool\s*\(\s*["']([^"']+)["']\s*\)\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = chainToolPattern.findAll(content)

        for (match in matches) {
            val toolId = match.groupValues[1]
            val toolBody = match.groupValues[2]

            logger.debug { "找到链式工具定义: $toolId" }

            // 为链式工具生成标准 API 端点
            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/chain/$toolId",
                    method = HttpMethod.GET,
                    description = "获取链式工具 $toolId 的详细信息",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/chain/$toolId/execute",
                    method = HttpMethod.POST,
                    description = "执行链式工具 $toolId",
                    parameters = listOf(
                        ApiParameter("data", "object", "工具输入数据", true)
                    ),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/chain/$toolId/steps",
                    method = HttpMethod.GET,
                    description = "获取链式工具 $toolId 的步骤",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )
        }

        return apiDefinitions
    }

    /**
     * 从并行工具定义中提取 API。
     */
    private fun extractParallelToolApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 parallelTool { ... } 块
        val parallelToolPattern = """parallelTool\s*\(\s*["']([^"']+)["']\s*\)\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = parallelToolPattern.findAll(content)

        for (match in matches) {
            val toolId = match.groupValues[1]
            val toolBody = match.groupValues[2]

            logger.debug { "找到并行工具定义: $toolId" }

            // 为并行工具生成标准 API 端点
            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/parallel/$toolId",
                    method = HttpMethod.GET,
                    description = "获取并行工具 $toolId 的详细信息",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/parallel/$toolId/execute",
                    method = HttpMethod.POST,
                    description = "执行并行工具 $toolId",
                    parameters = listOf(
                        ApiParameter("data", "object", "工具输入数据", true)
                    ),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/parallel/$toolId/tasks",
                    method = HttpMethod.GET,
                    description = "获取并行工具 $toolId 的任务",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )
        }

        return apiDefinitions
    }

    /**
     * 从条件工具定义中提取 API。
     */
    private fun extractConditionalToolApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 conditionalTool { ... } 块
        val conditionalToolPattern = """conditionalTool\s*\(\s*["']([^"']+)["']\s*\)\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = conditionalToolPattern.findAll(content)

        for (match in matches) {
            val toolId = match.groupValues[1]
            val toolBody = match.groupValues[2]

            logger.debug { "找到条件工具定义: $toolId" }

            // 为条件工具生成标准 API 端点
            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/conditional/$toolId",
                    method = HttpMethod.GET,
                    description = "获取条件工具 $toolId 的详细信息",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/conditional/$toolId/execute",
                    method = HttpMethod.POST,
                    description = "执行条件工具 $toolId",
                    parameters = listOf(
                        ApiParameter("data", "object", "工具输入数据", true)
                    ),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/conditional/$toolId/conditions",
                    method = HttpMethod.GET,
                    description = "获取条件工具 $toolId 的条件",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )
        }

        return apiDefinitions
    }

    /**
     * 从专业工具定义中提取 API。
     */
    private fun extractSpecializedToolApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 提取数据处理工具定义
        apiDefinitions.addAll(extractDataProcessingToolApiDefinitions(file, content))

        // 提取 API 集成工具定义
        apiDefinitions.addAll(extractApiIntegrationToolApiDefinitions(file, content))

        // 提取文件操作工具定义
        apiDefinitions.addAll(extractFileOperationToolApiDefinitions(file, content))

        return apiDefinitions
    }

    /**
     * 从数据处理工具定义中提取 API。
     */
    private fun extractDataProcessingToolApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 dataProcessingTool { ... } 块
        val dataProcessingToolPattern = """dataProcessingTool\s*\(\s*["']([^"']+)["']\s*\)\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = dataProcessingToolPattern.findAll(content)

        for (match in matches) {
            val toolId = match.groupValues[1]
            val toolBody = match.groupValues[2]

            logger.debug { "找到数据处理工具定义: $toolId" }

            // 为数据处理工具生成标准 API 端点
            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/data/$toolId",
                    method = HttpMethod.GET,
                    description = "获取数据处理工具 $toolId 的详细信息",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/data/$toolId/execute",
                    method = HttpMethod.POST,
                    description = "执行数据处理工具 $toolId",
                    parameters = listOf(
                        ApiParameter("data", "object", "输入数据", true),
                        ApiParameter("options", "object", "处理选项", false)
                    ),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/data/$toolId/schema",
                    method = HttpMethod.GET,
                    description = "获取数据处理工具 $toolId 的数据模式",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/data/$toolId/validate",
                    method = HttpMethod.POST,
                    description = "验证数据处理工具 $toolId 的输入数据",
                    parameters = listOf(
                        ApiParameter("data", "object", "要验证的数据", true)
                    ),
                    sourceFile = file.toString()
                )
            )
        }

        return apiDefinitions
    }

    /**
     * 从 API 集成工具定义中提取 API。
     */
    private fun extractApiIntegrationToolApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 apiIntegrationTool { ... } 块
        val apiIntegrationToolPattern = """apiIntegrationTool\s*\(\s*["']([^"']+)["']\s*\)\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = apiIntegrationToolPattern.findAll(content)

        for (match in matches) {
            val toolId = match.groupValues[1]
            val toolBody = match.groupValues[2]

            logger.debug { "找到 API 集成工具定义: $toolId" }

            // 为 API 集成工具生成标准 API 端点
            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/integration/$toolId",
                    method = HttpMethod.GET,
                    description = "获取 API 集成工具 $toolId 的详细信息",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/integration/$toolId/execute",
                    method = HttpMethod.POST,
                    description = "执行 API 集成工具 $toolId",
                    parameters = listOf(
                        ApiParameter("params", "object", "请求参数", true),
                        ApiParameter("headers", "object", "请求头", false),
                        ApiParameter("auth", "object", "认证信息", false)
                    ),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/integration/$toolId/endpoints",
                    method = HttpMethod.GET,
                    description = "获取 API 集成工具 $toolId 的可用端点",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/integration/$toolId/status",
                    method = HttpMethod.GET,
                    description = "获取 API 集成工具 $toolId 的连接状态",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )
        }

        return apiDefinitions
    }

    /**
     * 从文件操作工具定义中提取 API。
     */
    private fun extractFileOperationToolApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 fileOperationTool { ... } 块
        val fileOperationToolPattern = """fileOperationTool\s*\(\s*["']([^"']+)["']\s*\)\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = fileOperationToolPattern.findAll(content)

        for (match in matches) {
            val toolId = match.groupValues[1]
            val toolBody = match.groupValues[2]

            logger.debug { "找到文件操作工具定义: $toolId" }

            // 为文件操作工具生成标准 API 端点
            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/file/$toolId",
                    method = HttpMethod.GET,
                    description = "获取文件操作工具 $toolId 的详细信息",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/file/$toolId/execute",
                    method = HttpMethod.POST,
                    description = "执行文件操作工具 $toolId",
                    parameters = listOf(
                        ApiParameter("operation", "string", "操作类型", true),
                        ApiParameter("path", "string", "文件路径", true),
                        ApiParameter("content", "string", "文件内容", false),
                        ApiParameter("options", "object", "操作选项", false)
                    ),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/file/$toolId/list",
                    method = HttpMethod.GET,
                    description = "列出文件操作工具 $toolId 可访问的文件",
                    parameters = listOf(
                        ApiParameter("path", "string", "目录路径", true),
                        ApiParameter("recursive", "boolean", "是否递归", false)
                    ),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/tools/file/$toolId/info",
                    method = HttpMethod.GET,
                    description = "获取文件信息",
                    parameters = listOf(
                        ApiParameter("path", "string", "文件路径", true)
                    ),
                    sourceFile = file.toString()
                )
            )
        }

        return apiDefinitions
    }

    /**
     * 从工作流定义中提取 API。
     */
    private fun extractWorkflowApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 workflow { ... } 块
        val workflowPattern = """workflow\s*\(\s*["']([^"']+)["']\s*\)\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = workflowPattern.findAll(content)

        for (match in matches) {
            val workflowId = match.groupValues[1]
            val workflowBody = match.groupValues[2]

            logger.debug { "找到工作流定义: $workflowId" }

            // 为工作流生成标准 API 端点
            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/workflows/$workflowId",
                    method = HttpMethod.GET,
                    description = "获取工作流 $workflowId 的详细信息",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/workflows/$workflowId/execute",
                    method = HttpMethod.POST,
                    description = "执行工作流 $workflowId",
                    parameters = listOf(
                        ApiParameter("input", "object", "工作流输入数据", true)
                    ),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/workflows/$workflowId/status",
                    method = HttpMethod.GET,
                    description = "获取工作流 $workflowId 的执行状态",
                    parameters = listOf(
                        ApiParameter("runId", "string", "运行 ID", true)
                    ),
                    sourceFile = file.toString()
                )
            )
        }

        return apiDefinitions
    }

    /**
     * 从代理网络定义中提取 API。
     */
    private fun extractNetworkApiDefinitions(file: Path, content: String): List<ApiDefinition> {
        val apiDefinitions = mutableListOf<ApiDefinition>()

        // 使用正则表达式匹配 AgentNetwork.builder() 块
        val networkPattern = """AgentNetwork\.builder\(\)([^{}]*(?:\{[^{}]*\}[^{}]*)*)\.build\(\)""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = networkPattern.findAll(content)

        // 使用正则表达式匹配网络变量定义
        val networkVarPattern = """val\s+([a-zA-Z0-9_]+)\s*=\s*AgentNetwork\.builder\(\)""".toRegex()

        for (match in matches) {
            val networkBody = match.value
            val networkVarMatch = networkVarPattern.find(content.substring(0, match.range.first + match.range.last))
            val networkId = networkVarMatch?.groupValues?.get(1) ?: "network"

            logger.debug { "找到代理网络定义: $networkId" }

            // 为代理网络生成标准 API 端点
            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/networks/$networkId",
                    method = HttpMethod.GET,
                    description = "获取代理网络 $networkId 的详细信息",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/networks/$networkId/agents",
                    method = HttpMethod.GET,
                    description = "获取代理网络 $networkId 中的所有代理",
                    parameters = emptyList(),
                    sourceFile = file.toString()
                )
            )

            apiDefinitions.add(
                ApiDefinition(
                    path = "/api/networks/$networkId/route",
                    method = HttpMethod.POST,
                    description = "将请求路由到代理网络 $networkId 中的适当代理",
                    parameters = listOf(
                        ApiParameter("input", "string", "输入文本", true),
                        ApiParameter("options", "object", "生成选项", false)
                    ),
                    sourceFile = file.toString()
                )
            )

            // 提取网络中的代理
            val agentPattern = """\.addAgent\(([^)]+)\)""".toRegex()
            val agentMatches = agentPattern.findAll(networkBody)

            for (agentMatch in agentMatches) {
                val agentRef = agentMatch.groupValues[1].trim()

                // 为网络中的每个代理生成特定端点
                apiDefinitions.add(
                    ApiDefinition(
                        path = "/api/networks/$networkId/agents/$agentRef",
                        method = HttpMethod.GET,
                        description = "获取代理网络 $networkId 中的代理 $agentRef 的详细信息",
                        parameters = emptyList(),
                        sourceFile = file.toString()
                    )
                )

                apiDefinitions.add(
                    ApiDefinition(
                        path = "/api/networks/$networkId/agents/$agentRef/generate",
                        method = HttpMethod.POST,
                        description = "使用代理网络 $networkId 中的代理 $agentRef 生成响应",
                        parameters = listOf(
                            ApiParameter("messages", "array", "消息列表", true),
                            ApiParameter("threadId", "string", "会话 ID", false),
                            ApiParameter("resourceId", "string", "资源 ID", false)
                        ),
                        sourceFile = file.toString()
                    )
                )
            }
        }

        // 匹配 NetworkRouter 类
        val routerPattern = """class\s+([a-zA-Z0-9_]+)\s*(?::\s*NetworkRouter)?""".toRegex()
        val routerMatches = routerPattern.findAll(content)

        for (routerMatch in routerMatches) {
            val routerId = routerMatch.groupValues[1]

            if (routerId.contains("Router", ignoreCase = true) || routerId.contains("Network", ignoreCase = true)) {
                logger.debug { "找到网络路由器定义: $routerId" }

                // 为网络路由器生成标准 API 端点
                apiDefinitions.add(
                    ApiDefinition(
                        path = "/api/routers/$routerId",
                        method = HttpMethod.GET,
                        description = "获取网络路由器 $routerId 的详细信息",
                        parameters = emptyList(),
                        sourceFile = file.toString()
                    )
                )

                apiDefinitions.add(
                    ApiDefinition(
                        path = "/api/routers/$routerId/route",
                        method = HttpMethod.POST,
                        description = "使用网络路由器 $routerId 路由请求",
                        parameters = listOf(
                            ApiParameter("input", "string", "输入文本", true),
                            ApiParameter("options", "object", "生成选项", false)
                        ),
                        sourceFile = file.toString()
                    )
                )
            }
        }

        return apiDefinitions
    }
}
