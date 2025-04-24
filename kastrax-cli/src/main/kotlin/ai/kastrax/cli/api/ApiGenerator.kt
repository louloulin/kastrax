

package ai.kastrax.cli.api

import ai.kastrax.cli.dsl.KastraxDslScanner
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

private val logger = KotlinLogging.logger {}

/**
 * API 生成器，用于生成 OpenAPI 规范和服务器路由。
 */
class ApiGenerator(private val dslScanner: KastraxDslScanner) {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)

    /**
     * 生成 API 定义。
     */
    fun generateApis(): List<ApiDefinition> {
        logger.info { "开始生成 API..." }

        // 扫描 DSL 获取 API 定义
        val apiDefinitions = dslScanner.scan()

        // 验证 API 定义
        val validatedDefinitions = validateApiDefinitions(apiDefinitions)

        // 生成 OpenAPI 规范
        generateOpenApiSpec(validatedDefinitions)

        // 生成服务器路由
        generateServerRoutes(validatedDefinitions)

        logger.info { "API 生成完成，共 ${validatedDefinitions.size} 个端点" }
        return validatedDefinitions
    }

    /**
     * 验证 API 定义。
     */
    private fun validateApiDefinitions(apiDefinitions: List<ApiDefinition>): List<ApiDefinition> {
        logger.info { "验证 API 定义..." }

        val validatedDefinitions = mutableListOf<ApiDefinition>()
        val pathMethodMap = mutableMapOf<Pair<String, HttpMethod>, ApiDefinition>()
        val duplicates = mutableListOf<ApiDefinition>()
        val invalidPaths = mutableListOf<ApiDefinition>()

        for (api in apiDefinitions) {
            // 验证路径格式
            if (!api.path.startsWith("/") || api.path.contains("//") || api.path.endsWith("/") && api.path != "/") {
                logger.warn { "无效的 API 路径: ${api.path}" }
                invalidPaths.add(api)
                continue
            }

            // 检查重复
            val key = api.path to api.method
            if (key in pathMethodMap) {
                logger.warn { "重复的 API 定义: ${api.path} ${api.method}" }
                duplicates.add(api)
                continue
            }

            pathMethodMap[key] = api
            validatedDefinitions.add(api)
        }

        logger.info { "验证完成: ${validatedDefinitions.size} 个有效端点, ${duplicates.size} 个重复, ${invalidPaths.size} 个无效路径" }

        // 生成验证报告
        generateValidationReport(validatedDefinitions, duplicates, invalidPaths)

        return validatedDefinitions
    }

    /**
     * 生成验证报告。
     */
    private fun generateValidationReport(
        validApis: List<ApiDefinition>,
        duplicates: List<ApiDefinition>,
        invalidPaths: List<ApiDefinition>
    ): File {
        logger.info { "生成 API 验证报告..." }

        val report = mapOf(
            "valid" to validApis.map { api ->
                mapOf(
                    "path" to api.path,
                    "method" to api.method,
                    "description" to api.description,
                    "sourceFile" to api.sourceFile
                )
            },
            "duplicates" to duplicates.map { api ->
                mapOf(
                    "path" to api.path,
                    "method" to api.method,
                    "description" to api.description,
                    "sourceFile" to api.sourceFile
                )
            },
            "invalidPaths" to invalidPaths.map { api ->
                mapOf(
                    "path" to api.path,
                    "method" to api.method,
                    "description" to api.description,
                    "sourceFile" to api.sourceFile
                )
            },
            "summary" to mapOf(
                "totalApis" to (validApis.size + duplicates.size + invalidPaths.size),
                "validApis" to validApis.size,
                "duplicates" to duplicates.size,
                "invalidPaths" to invalidPaths.size,
                "validPercentage" to (validApis.size * 100.0 / (validApis.size + duplicates.size + invalidPaths.size).coerceAtLeast(1))
            )
        )

        // 将验证报告写入文件
        val outputDir = File(".kastrax")
        outputDir.mkdirs()

        val outputFile = File(outputDir, "api-validation.json")
        objectMapper.writeValue(outputFile, report)

        logger.info { "API 验证报告已生成: ${outputFile.absolutePath}" }
        return outputFile
    }

    /**
     * 生成 OpenAPI 规范。
     */
    private fun generateOpenApiSpec(apiDefinitions: List<ApiDefinition>): File {
        logger.info { "生成 OpenAPI 规范..." }

        val openApiSpec = OpenApiSpec(
            info = OpenApiInfo(
                title = "KastraX API",
                version = "1.0.0",
                description = "KastraX API 自动生成的 OpenAPI 规范"
            ),
            paths = apiDefinitions.groupBy { it.path }
                .mapValues { (_, apis) ->
                    apis.associate { api ->
                        api.method.name.lowercase() to OpenApiOperation(
                            summary = api.description,
                            description = api.description,
                            parameters = api.parameters.map { param ->
                                OpenApiParameter(
                                    name = param.name,
                                    `in` = if (api.method == HttpMethod.GET) "query" else "body",
                                    required = param.required,
                                    schema = OpenApiSchema(type = param.type)
                                )
                            },
                            responses = mapOf(
                                "200" to OpenApiResponse(
                                    description = "成功响应"
                                ),
                                "400" to OpenApiResponse(
                                    description = "请求错误"
                                ),
                                "404" to OpenApiResponse(
                                    description = "资源不存在"
                                ),
                                "500" to OpenApiResponse(
                                    description = "服务器错误"
                                )
                            )
                        )
                    }
                }
        )

        // 将 OpenAPI 规范写入文件
        val outputDir = File(".kastrax")
        outputDir.mkdirs()

        val outputFile = File(outputDir, "openapi.json")
        objectMapper.writeValue(outputFile, openApiSpec)

        logger.info { "OpenAPI 规范已生成: ${outputFile.absolutePath}" }
        return outputFile
    }

    /**
     * 生成服务器路由。
     */
    private fun generateServerRoutes(apiDefinitions: List<ApiDefinition>): File {
        logger.info { "生成服务器路由..." }

        val serverRoutes = apiDefinitions.map { api ->
            ServerRoute(
                path = api.path,
                method = api.method,
                handler = "DefaultHandler",
                parameters = api.parameters
            )
        }

        // 将服务器路由写入文件
        val outputDir = File(".kastrax")
        outputDir.mkdirs()

        val outputFile = File(outputDir, "routes.json")
        objectMapper.writeValue(outputFile, serverRoutes)

        logger.info { "服务器路由已生成: ${outputFile.absolutePath}" }
        return outputFile
    }
}

/**
 * API 定义。
 */
data class ApiDefinition(
    val path: String,
    val method: HttpMethod,
    val description: String,
    val parameters: List<ApiParameter>,
    val sourceFile: String
)

/**
 * API 参数。
 */
data class ApiParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean
)

/**
 * HTTP 方法。
 */
enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH
}

/**
 * 服务器路由。
 */
data class ServerRoute(
    val path: String,
    val method: HttpMethod,
    val handler: String,
    val parameters: List<ApiParameter>
)

/**
 * OpenAPI 规范。
 */
data class OpenApiSpec(
    val openapi: String = "3.0.0",
    val info: OpenApiInfo,
    val paths: Map<String, Map<String, OpenApiOperation>>
)

/**
 * OpenAPI 信息。
 */
data class OpenApiInfo(
    val title: String,
    val version: String,
    val description: String
)

/**
 * OpenAPI 操作。
 */
data class OpenApiOperation(
    val summary: String,
    val description: String,
    val parameters: List<OpenApiParameter>,
    val responses: Map<String, OpenApiResponse>
)

/**
 * OpenAPI 参数。
 */
data class OpenApiParameter(
    val name: String,
    val `in`: String,
    val required: Boolean,
    val schema: OpenApiSchema
)

/**
 * OpenAPI 模式。
 */
data class OpenApiSchema(
    val type: String
)

/**
 * OpenAPI 响应。
 */
data class OpenApiResponse(
    val description: String
)
