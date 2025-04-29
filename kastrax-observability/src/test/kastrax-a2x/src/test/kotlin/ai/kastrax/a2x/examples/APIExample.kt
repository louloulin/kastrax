package ai.kastrax.a2x.examples

import ai.kastrax.a2x.a2x
import ai.kastrax.a2x.adapter.APIConfig
import ai.kastrax.a2x.adapter.APIEndpoint
import ai.kastrax.a2x.adapter.APIParameter
import ai.kastrax.a2x.model.Authentication
import ai.kastrax.a2x.model.AuthenticationType
import ai.kastrax.a2x.model.EntityType
import ai.kastrax.a2x.model.InvokeRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * API 示例
 */
fun main() = runBlocking {
    // 创建 A2X 实例
    val a2xInstance = a2x {
        // 注册 Web API
        api(APIConfig(
            id = "github-api",
            name = "GitHub API",
            description = "GitHub REST API",
            baseUrl = "https://api.github.com",
            defaultHeaders = mapOf(
                "Accept" to "application/vnd.github.v3+json"
            ),
            endpoints = listOf(
                APIEndpoint(
                    id = "get-user",
                    name = "获取用户信息",
                    description = "获取 GitHub 用户信息",
                    method = "GET",
                    path = "/users/{username}",
                    parameters = listOf(
                        APIParameter(
                            name = "username",
                            type = "string",
                            description = "GitHub 用户名",
                            required = true,
                            location = "path"
                        )
                    )
                ),
                APIEndpoint(
                    id = "get-repos",
                    name = "获取用户仓库",
                    description = "获取 GitHub 用户的仓库列表",
                    method = "GET",
                    path = "/users/{username}/repos",
                    parameters = listOf(
                        APIParameter(
                            name = "username",
                            type = "string",
                            description = "GitHub 用户名",
                            required = true,
                            location = "path"
                        ),
                        APIParameter(
                            name = "sort",
                            type = "string",
                            description = "排序方式（created, updated, pushed, full_name）",
                            required = false,
                            location = "query"
                        ),
                        APIParameter(
                            name = "per_page",
                            type = "integer",
                            description = "每页结果数量",
                            required = false,
                            location = "query"
                        ),
                        APIParameter(
                            name = "page",
                            type = "integer",
                            description = "页码",
                            required = false,
                            location = "query"
                        )
                    )
                ),
                APIEndpoint(
                    id = "create-repo",
                    name = "创建仓库",
                    description = "创建 GitHub 仓库",
                    method = "POST",
                    path = "/user/repos",
                    parameters = listOf(
                        APIParameter(
                            name = "name",
                            type = "string",
                            description = "仓库名称",
                            required = true,
                            location = "body"
                        ),
                        APIParameter(
                            name = "description",
                            type = "string",
                            description = "仓库描述",
                            required = false,
                            location = "body"
                        ),
                        APIParameter(
                            name = "private",
                            type = "boolean",
                            description = "是否为私有仓库",
                            required = false,
                            location = "body"
                        )
                    )
                )
            ),
            authentication = Authentication(
                type = AuthenticationType.OTHER,
                metadata = mapOf(
                    "auth_type" to "bearer",
                    "token" to (System.getenv("GITHUB_TOKEN") ?: "")
                )
            )
        ))

        // 配置服务器
        server {
            port = 8080
            enableCors = true
        }
    }

    // 获取 GitHub API 实体
    val githubApi = a2xInstance.getEntity("github-api")
    if (githubApi != null) {
        println("GitHub API 实体: ${githubApi.getEntityCard().name}")
        println("GitHub API 能力: ${githubApi.getCapabilities().map { it.name }}")

        try {
            // 创建获取用户信息请求
            val getUserRequest = InvokeRequest(
                id = "get-user-request",
                source = a2xInstance.createLocalEntityReference("test-client", EntityType.AGENT),
                target = a2xInstance.createLocalEntityReference("github-api", EntityType.SYSTEM),
                capabilityId = "get-user",
                parameters = mapOf(
                    "username" to JsonPrimitive("octocat")
                )
            )

            // 调用获取用户信息能力
            val getUserResponse = githubApi.invoke(getUserRequest)
            println("获取用户信息响应: ${getUserResponse.result}")

            // 创建获取用户仓库请求
            val getReposRequest = InvokeRequest(
                id = "get-repos-request",
                source = a2xInstance.createLocalEntityReference("test-client", EntityType.AGENT),
                target = a2xInstance.createLocalEntityReference("github-api", EntityType.SYSTEM),
                capabilityId = "get-repos",
                parameters = mapOf(
                    "username" to JsonPrimitive("octocat"),
                    "sort" to JsonPrimitive("updated"),
                    "per_page" to JsonPrimitive("5")
                )
            )

            // 调用获取用户仓库能力
            val getReposResponse = githubApi.invoke(getReposRequest)
            println("获取用户仓库响应: ${getReposResponse.result}")
        } catch (e: Exception) {
            println("调用 GitHub API 失败: ${e.message}")
            e.printStackTrace()
        }
    } else {
        println("未找到 GitHub API 实体")
    }

    // 停止服务器
    a2xInstance.stopServer()
}
