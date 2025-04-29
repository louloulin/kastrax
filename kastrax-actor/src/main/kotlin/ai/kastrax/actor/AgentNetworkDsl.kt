package ai.kastrax.actor

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.Props

/**
 * Agent 网络，管理一组相关的 Agent
 *
 * @property system ActorSystem 实例
 * @property agents Agent 映射表
 * @property coordinator 协调者 Agent
 */
class AgentNetwork(
    private val system: ActorSystem,
    private val agents: Map<String, PID>,
    private val coordinator: PID?
) {
    /**
     * 发送消息给特定 Agent
     *
     * @param agentName Agent 名称
     * @param message 要发送的消息
     */
    fun send(agentName: String, message: AgentMessage) {
        val pid = agents[agentName] ?: throw IllegalArgumentException("Agent not found: $agentName")
        system.root.send(pid, message)
    }
    
    /**
     * 发送消息给协调者
     *
     * @param message 要发送的消息
     */
    fun sendToCoordinator(message: AgentMessage) {
        coordinator?.let { system.root.send(it, message) }
    }
    
    /**
     * 广播消息给所有 Agent
     *
     * @param message 要发送的消息
     */
    fun broadcast(message: AgentMessage) {
        agents.values.forEach { system.root.send(it, message) }
    }
    
    /**
     * 请求-响应模式，向特定 Agent 发送请求并等待响应
     *
     * @param agentName Agent 名称
     * @param message 要发送的消息
     * @return Agent 的响应
     */
    suspend fun ask(agentName: String, message: AgentMessage): AgentMessage {
        val pid = agents[agentName] ?: throw IllegalArgumentException("Agent not found: $agentName")
        return system.root.requestAwait<AgentMessage>(pid, message)
    }
}

/**
 * Agent 网络构建器
 *
 * @property system ActorSystem 实例
 */
class AgentNetworkBuilder(private val system: ActorSystem) {
    private val agents = mutableMapOf<String, PID>()
    private var coordinator: PID? = null
    
    /**
     * 添加 Agent
     *
     * @param name Agent 名称
     * @param block 配置 Actor Agent 的代码块
     */
    fun agent(name: String, block: ActorAgentBuilder.() -> Unit) {
        val builder = ActorAgentBuilder()
        builder.block()
        val agent = builder.agentBuilder.build()
        
        // 创建 Props，应用 actor 配置
        var props = Props.fromProducer { KastraxActor(agent) }
            .withMailbox(builder.actorBuilder.mailbox)
        
        // 如果指定了 dispatcher，则应用它
        builder.actorBuilder.dispatcher?.let {
            props = props.withDispatcher(it)
        }
        
        // 应用监督策略
        props = props.withSupervisor(builder.actorBuilder.supervisionStrategy)
        
        val pid = system.root.spawnNamed(props, name)
        agents[name] = pid
    }
    
    /**
     * 设置协调者
     *
     * @param block 配置协调者 Actor Agent 的代码块
     */
    fun coordinator(block: ActorAgentBuilder.() -> Unit) {
        val builder = ActorAgentBuilder()
        builder.block()
        val agent = builder.agentBuilder.build()
        
        // 创建 Props，应用 actor 配置
        var props = Props.fromProducer { KastraxActor(agent) }
            .withMailbox(builder.actorBuilder.mailbox)
        
        // 如果指定了 dispatcher，则应用它
        builder.actorBuilder.dispatcher?.let {
            props = props.withDispatcher(it)
        }
        
        // 应用监督策略
        props = props.withSupervisor(builder.actorBuilder.supervisionStrategy)
        
        coordinator = system.root.spawnNamed(props, "coordinator")
    }
    
    /**
     * 构建网络
     *
     * @return 构建的 Agent 网络
     */
    fun build(): AgentNetwork {
        return AgentNetwork(system, agents, coordinator)
    }
}

/**
 * 创建 Agent 网络
 *
 * @param block 配置 Agent 网络的代码块
 * @return 构建的 Agent 网络
 */
fun ActorSystem.agentNetwork(block: AgentNetworkBuilder.() -> Unit): AgentNetwork {
    val builder = AgentNetworkBuilder(this)
    builder.block()
    return builder.build()
}
