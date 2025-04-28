package ai.kastrax.a2a.server

import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2a.model.*
import ai.kastrax.a2a.task.TaskManager
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * A2A 服务器配置
 */
data class A2AServerConfig(
    /**
     * 服务器主机
     */
    val host: String = "0.0.0.0",

    /**
     * 服务器端口
     */
    val port: Int = 8080,

    /**
     * 是否启用 CORS
     */
    val enableCors: Boolean = true,

    /**
     * 是否启用 HTTPS
     */
    val enableHttps: Boolean = false,

    /**
     * HTTPS 证书路径
     */
    val certPath: String? = null,

    /**
     * HTTPS 私钥路径
     */
    val keyPath: String? = null
)

/**
 * 配置 A2A 服务器
 */
fun Application.configureA2AServer(agents: Map<String, A2AAgent>, taskManager: TaskManager? = null) {
    // 配置内容协商
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // 配置 CORS
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
    }

    // 配置路由
    routing {
        // Agent Card 端点
        get("/.well-known/agent.json") {
            val hostAgent = agents.values.firstOrNull() ?: throw NotFoundException("No agent available")
            call.respond(hostAgent.getAgentCard())
        }

        route("/a2a/v1") {
            // 代理发现端点
            get("/agents") {
                call.respond(agents.values.map { it.getAgentCard() })
            }

            // 代理能力查询
            get("/agents/{agentId}/capabilities") {
                val agentId = call.parameters["agentId"] ?: throw IllegalArgumentException("Agent ID is required")
                val agent = agents[agentId] ?: throw NotFoundException("Agent not found")
                call.respond(agent.getCapabilities())
            }

            // 代理能力调用
            post("/agents/{agentId}/invoke") {
                val agentId = call.parameters["agentId"] ?: throw IllegalArgumentException("Agent ID is required")
                val agent = agents[agentId] ?: throw NotFoundException("Agent not found")

                val request = call.receive<InvokeRequest>()
                val response = agent.invoke(request)
                call.respond(response)
            }

            // 代理状态查询
            get("/agents/{agentId}/status") {
                val agentId = call.parameters["agentId"] ?: throw IllegalArgumentException("Agent ID is required")
                val agent = agents[agentId] ?: throw NotFoundException("Agent not found")

                val request = QueryRequest(
                    id = UUID.randomUUID().toString(),
                    queryType = "status"
                )

                val response = agent.query(request)
                call.respond(response)
            }

            // 代理健康检查
            get("/agents/{agentId}/health") {
                val agentId = call.parameters["agentId"] ?: throw IllegalArgumentException("Agent ID is required")
                val agent = agents[agentId] ?: throw NotFoundException("Agent not found")

                val request = QueryRequest(
                    id = UUID.randomUUID().toString(),
                    queryType = "health"
                )

                val response = agent.query(request)
                call.respond(response)
            }

            // 处理通用 A2A 消息
            post("/agents/{agentId}/message") {
                val agentId = call.parameters["agentId"] ?: throw IllegalArgumentException("Agent ID is required")
                val agent = agents[agentId] ?: throw NotFoundException("Agent not found")

                val message = call.receive<A2AMessage>()
                val response = agent.processMessage(message)
                call.respond(response)
            }
        }

        // 配置任务服务器端点
        if (taskManager != null) {
            // 配置任务路由
            routing {
                // 注释掉任务路由配置，因为我们还没有实现 SSE 相关功能
                // configureTaskRoutes(agents, taskManager)
            }
        }
    }
}

/**
 * 找不到资源异常
 */
class NotFoundException(override val message: String) : RuntimeException(message)
