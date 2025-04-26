package ai.kastrax.core.agent

import ai.kastrax.core.agent.routing.ContextAwareRoutingStrategy
import ai.kastrax.core.agent.routing.DefaultRoutingStrategy
import ai.kastrax.core.agent.routing.RoutingStrategy
import ai.kastrax.core.llm.LlmProvider

/**
 * 代理网络构建器
 */
class AgentNetworkBuilder {
    var name: String = ""
    var instructions: String = ""
    lateinit var model: LlmProvider
    val agents: MutableList<Agent> = mutableListOf()
    var routingStrategy: RoutingStrategy = DefaultRoutingStrategy()
    var visualizeInteractions: Boolean = false

    /**
     * 添加代理
     */
    fun agent(agent: Agent) {
        agents.add(agent)
    }
    
    /**
     * 设置路由策略
     */
    fun routingStrategy(strategy: RoutingStrategy) {
        routingStrategy = strategy
    }
    
    /**
     * 使用上下文感知路由策略
     */
    fun useContextAwareRouting() {
        routingStrategy = ContextAwareRoutingStrategy()
    }
    
    /**
     * 启用交互可视化
     */
    fun enableVisualization() {
        visualizeInteractions = true
    }

    /**
     * 构建代理网络
     */
    fun build(): AgentNetwork {
        require(name.isNotEmpty()) { "代理网络名称不能为空" }
        require(instructions.isNotEmpty()) { "代理网络指令不能为空" }
        require(::model.isInitialized) { "代理网络模型必须定义" }
        require(agents.isNotEmpty()) { "代理网络必须至少有一个代理" }

        return AgentNetwork(
            AgentNetworkConfig(
                name = name,
                instructions = instructions,
                model = model,
                agents = agents,
                routingStrategy = routingStrategy,
                visualizeInteractions = visualizeInteractions
            )
        )
    }
}

/**
 * 创建代理网络的DSL函数
 */
fun agentNetwork(init: AgentNetworkBuilder.() -> Unit): AgentNetwork {
    val builder = AgentNetworkBuilder()
    builder.init()
    return builder.build()
}
