package ai.kastrax.a2a

import ai.kastrax.a2a.adapter.A2AAgentAdapter
import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2a.client.A2AClient
import ai.kastrax.a2a.client.A2AClientConfig
import ai.kastrax.a2a.discovery.A2ADiscoveryConfig
import ai.kastrax.a2a.discovery.A2ADiscoveryService
import ai.kastrax.a2a.dsl.a2aAgent
import ai.kastrax.a2a.server.A2AServerConfig
import ai.kastrax.core.agent.Agent
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * A2A 模块的主入口类
 */
class A2A {
    /**
     * 代理适配器
     */
    private val agentAdapter = A2AAgentAdapter()
    
    /**
     * 代理发现服务
     */
    private val discoveryService = A2ADiscoveryService()
    
    /**
     * 已注册的代理
     */
    private val agents = mutableMapOf<String, A2AAgent>()
    
    /**
     * 服务器实例
     */
    private var server: ApplicationEngine? = null
    
    /**
     * 将 kastrax 代理转换为 A2A 代理
     */
    fun adaptAgent(agent: Agent, endpoint: String = "/a2a/v1/agents"): A2AAgent {
        return agentAdapter.adapt(agent, endpoint)
    }
    
    /**
     * 注册 A2A 代理
     */
    fun registerAgent(agent: A2AAgent) {
        agents[agent.getAgentCard().id] = agent
    }
    
    /**
     * 注销 A2A 代理
     */
    fun unregisterAgent(agentId: String) {
        agents.remove(agentId)
    }
    
    /**
     * 获取 A2A 代理
     */
    fun getAgent(agentId: String): A2AAgent? {
        return agents[agentId]
    }
    
    /**
     * 获取所有 A2A 代理
     */
    fun getAllAgents(): List<A2AAgent> {
        return agents.values.toList()
    }
    
    /**
     * 创建 A2A 客户端
     */
    fun createClient(serverUrl: String, apiKey: String? = null): A2AClient {
        return A2AClient(A2AClientConfig(serverUrl = serverUrl, apiKey = apiKey))
    }
    
    /**
     * 启动 A2A 服务器
     */
    fun startServer(config: A2AServerConfig = A2AServerConfig()) {
        if (server != null) {
            return
        }
        
        server = embeddedServer(Netty, port = config.port, host = config.host) {
            ai.kastrax.a2a.server.configureA2AServer(agents)
        }
        
        server?.start(wait = false)
    }
    
    /**
     * 停止 A2A 服务器
     */
    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
    }
    
    /**
     * 添加服务器 URL 到发现服务
     */
    fun addServerToDiscovery(serverUrl: String) {
        discoveryService.addServer(serverUrl)
    }
    
    /**
     * 从发现服务移除服务器 URL
     */
    fun removeServerFromDiscovery(serverUrl: String) {
        discoveryService.removeServer(serverUrl)
    }
    
    companion object {
        /**
         * 单例实例
         */
        private val instance = A2A()
        
        /**
         * 获取单例实例
         */
        fun getInstance(): A2A = instance
    }
}

/**
 * 创建 A2A 代理的 DSL 函数
 */
fun a2a(init: A2ADslBuilder.() -> Unit): A2A {
    val builder = A2ADslBuilder()
    builder.init()
    return builder.build()
}

/**
 * A2A DSL 构建器
 */
class A2ADslBuilder {
    /**
     * A2A 实例
     */
    private val a2a = A2A.getInstance()
    
    /**
     * 服务器配置
     */
    private var serverConfig: A2AServerConfig? = null
    
    /**
     * 是否启动服务器
     */
    private var startServer = false
    
    /**
     * 配置服务器
     */
    fun server(init: ServerConfigBuilder.() -> Unit) {
        val builder = ServerConfigBuilder()
        builder.init()
        serverConfig = builder.build()
        startServer = true
    }
    
    /**
     * 注册代理
     */
    fun agent(agent: Agent) {
        val a2aAgent = a2a.adaptAgent(agent)
        a2a.registerAgent(a2aAgent)
    }
    
    /**
     * 注册 A2A 代理
     */
    fun a2aAgent(init: ai.kastrax.a2a.dsl.A2AAgentBuilder.() -> Unit) {
        val a2aAgent = ai.kastrax.a2a.dsl.a2aAgent(init)
        a2a.registerAgent(a2aAgent)
    }
    
    /**
     * 添加服务器到发现服务
     */
    fun discovery(serverUrl: String) {
        a2a.addServerToDiscovery(serverUrl)
    }
    
    /**
     * 构建 A2A 实例
     */
    fun build(): A2A {
        if (startServer) {
            a2a.startServer(serverConfig ?: A2AServerConfig())
        }
        
        return a2a
    }
}

/**
 * 服务器配置构建器
 */
class ServerConfigBuilder {
    /**
     * 服务器主机
     */
    var host: String = "0.0.0.0"
    
    /**
     * 服务器端口
     */
    var port: Int = 8080
    
    /**
     * 是否启用 CORS
     */
    var enableCors: Boolean = true
    
    /**
     * 是否启用 HTTPS
     */
    var enableHttps: Boolean = false
    
    /**
     * HTTPS 证书路径
     */
    var certPath: String? = null
    
    /**
     * HTTPS 私钥路径
     */
    var keyPath: String? = null
    
    /**
     * 构建服务器配置
     */
    fun build(): A2AServerConfig {
        return A2AServerConfig(
            host = host,
            port = port,
            enableCors = enableCors,
            enableHttps = enableHttps,
            certPath = certPath,
            keyPath = keyPath
        )
    }
}
