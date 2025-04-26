package examples

import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

/**
 * GitHub 集成示例
 */
fun main() = runBlocking {
    println("KastraX MCP GitHub 集成示例")
    println("========================")

    // 连接到 GitHub
    val github = GitHubBuilder()
        .withOAuthToken(System.getenv("GITHUB_TOKEN"))
        .build()
    
    // 创建 GitHub MCP 服务器
    val githubServer = mcpServer {
        name("github-server")
        version("1.0.0")
        
        // 添加搜索仓库工具
        tool {
            name = "searchRepositories"
            description = "搜索 GitHub 仓库"
            parameters {
                parameter {
                    name = "query"
                    type = "string"
                    description = "搜索查询"
                    required = true
                }
                parameter {
                    name = "limit"
                    type = "integer"
                    description = "结果数量限制"
                    required = false
                }
            }
            handler { params ->
                val query = params["query"] as String
                val limit = params["limit"] as? Int ?: 10
                
                // 搜索 GitHub 仓库
                val searchResult = github.searchRepositories()
                    .q(query)
                    .list()
                    .take(limit)
                    .map { repo ->
                        mapOf(
                            "name" to repo.name,
                            "fullName" to repo.fullName,
                            "description" to (repo.description ?: ""),
                            "url" to repo.htmlUrl.toString(),
                            "stars" to repo.stargazersCount,
                            "forks" to repo.forksCount,
                            "language" to (repo.language ?: "")
                        )
                    }
                
                // 返回结果
                searchResult.toString()
            }
        }
        
        // 添加获取仓库工具
        tool {
            name = "getRepository"
            description = "获取 GitHub 仓库信息"
            parameters {
                parameter {
                    name = "owner"
                    type = "string"
                    description = "仓库所有者"
                    required = true
                }
                parameter {
                    name = "repo"
                    type = "string"
                    description = "仓库名称"
                    required = true
                }
            }
            handler { params ->
                val owner = params["owner"] as String
                val repo = params["repo"] as String
                
                // 获取 GitHub 仓库
                val repository = github.getRepository("$owner/$repo")
                
                // 返回仓库信息
                mapOf(
                    "name" to repository.name,
                    "fullName" to repository.fullName,
                    "description" to (repository.description ?: ""),
                    "url" to repository.htmlUrl.toString(),
                    "stars" to repository.stargazersCount,
                    "forks" to repository.forksCount,
                    "language" to (repository.language ?: ""),
                    "createdAt" to repository.createdAt.toString(),
                    "updatedAt" to repository.updatedAt.toString(),
                    "topics" to repository.topics
                ).toString()
            }
        }
    }
    
    // 启动服务器
    println("\n启动服务器...")
    githubServer.startSSE(port = 8080)
    
    println("\nGitHub 服务器已启动，访问 http://localhost:8080/mcp/sse")
    println("按 Enter 键停止服务器...")
    readLine()
    
    // 停止服务器
    println("\n停止服务器...")
    githubServer.stop()
    
    println("\n示例完成!")
}
