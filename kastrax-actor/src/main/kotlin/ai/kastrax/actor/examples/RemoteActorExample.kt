package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.KastraxActor
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentResponse as CoreAgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.agent.SessionInfo
import kotlinx.coroutines.flow.flowOf
import ai.kastrax.actor.remote.RemoteActorConfig
import ai.kastrax.actor.remote.configureRemoteActorSystem
import ai.kastrax.actor.remote.connectToRemoteSystem
import ai.kastrax.actor.remote.registerRemoteAgent
import ai.kastrax.actor.remote.remoteAddress
import ai.kastrax.core.agent.AgentGenerateOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * 远程模拟 Agent 实现，用于示例
 */
class RemoteMockAgent : Agent {
    override val name: String = "MockAgent"
    override val versionManager: AgentVersionManager? = null

    override suspend fun generate(messages: List<LlmMessage>, options: ai.kastrax.core.agent.AgentGenerateOptions): CoreAgentResponse {
        val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
        return generate(lastMessage, options)
    }

    override suspend fun generate(prompt: String, options: ai.kastrax.core.agent.AgentGenerateOptions): CoreAgentResponse {
        return CoreAgentResponse(
            text = "这是对“$prompt”的模拟响应",
            toolCalls = emptyList()
        )
    }

    override suspend fun stream(prompt: String, options: AgentStreamOptions): CoreAgentResponse {
        return CoreAgentResponse(
            text = "这是对“$prompt”的模拟流式响应",
            toolCalls = emptyList(),
            textStream = flowOf("这", "是", "对", "“$prompt”", "的", "模拟", "流式", "响应")
        )
    }

    override suspend fun reset() {
        // 不做任何操作
    }

    override suspend fun getState(): AgentState? {
        return null
    }

    override suspend fun updateState(status: AgentStatus): AgentState? {
        return null
    }

    override suspend fun createSession(title: String?, resourceId: String?, metadata: Map<String, String>): SessionInfo? {
        return null
    }

    override suspend fun getSession(sessionId: String): SessionInfo? {
        return null
    }

    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        return emptyList()
    }

    override suspend fun createVersion(
        instructions: String,
        name: String?,
        description: String?,
        metadata: Map<String, String>,
        activateImmediately: Boolean
    ): AgentVersion? {
        return null
    }

    override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
        return emptyList()
    }

    override suspend fun getActiveVersion(): AgentVersion? {
        return null
    }

    override suspend fun activateVersion(versionId: String): AgentVersion? {
        return null
    }

    override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
        return null
    }
}

/**
 * 远程 Actor 示例
 */
object RemoteActorExample {
    /**
     * 启动服务器
     */
    fun startServer() {
        // 配置远程 Actor 系统 - 使用新的 API
        val system = configureRemoteActorSystem(8090, "0.0.0.0")

        // 注册 Agent
        val mockAgent = RemoteMockAgent()
        system.registerRemoteAgent(mockAgent, "remoteAssistant")

        println("远程服务器已启动，监听端口: 8090")
        println("按 Ctrl+C 停止服务器")

        // 保持系统运行
        runBlocking {
            while (true) {
                delay(1000)
            }
        }
    }

    /**
     * 启动服务器（使用配置对象）
     */
    fun startServerWithConfig() {
        // 配置远程 Actor 系统 - 使用配置对象
        val config = RemoteActorConfig(
            port = 8090,
            advertisedHostname = "localhost"
        )
        val system = ai.kastrax.actor.remote.configureRemoteActorSystemWithConfig("kastrax-remote-server", config)

        // 注册 Agent
        val mockAgent = RemoteMockAgent()
        system.registerRemoteAgent(mockAgent, "remoteAssistant")

        println("远程服务器已启动，监听端口: ${config.port}")
        println("按 Ctrl+C 停止服务器")

        // 保持系统运行
        runBlocking {
            while (true) {
                delay(1000)
            }
        }
    }

    /**
     * 连接到服务器
     */
    fun connectToServer() = runBlocking {
        // 连接到远程系统 - 使用新的 API
        val remoteAgent = connectToRemoteSystem("localhost", 8090)

        // 发送消息给远程 Agent
        val response = remoteAgent.ask("remoteAssistant", AgentRequest("你好，远程助手！"))
        println("远程助手回答: ${(response as AgentResponse).text}")
    }

    /**
     * 连接到服务器（使用旧 API）
     */
    fun connectToServerWithOldApi() = runBlocking {
        // 连接到远程系统 - 使用旧的 API
        // 创建客户端系统
        val clientSystem = ActorSystem("kastrax-client")
        val remoteAddress = "localhost:8090"
        val remoteAgent = ai.kastrax.actor.RemoteAgent(clientSystem, remoteAddress)

        // 发送消息给远程 Agent
        val response = remoteAgent.ask("remoteAssistant", AgentRequest("你好，远程助手！"))
        println("远程助手回答: ${(response as AgentResponse).text}")
    }

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty() || args[0] == "server") {
            startServer()
        } else if (args[0] == "server-config") {
            startServerWithConfig()
        } else if (args[0] == "client") {
            connectToServer()
        } else if (args[0] == "client-old") {
            connectToServerWithOldApi()
        } else {
            println("用法: RemoteActorExample [server|server-config|client|client-old]")
        }
    }
}
