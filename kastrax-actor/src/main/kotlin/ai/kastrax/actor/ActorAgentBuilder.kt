package ai.kastrax.actor

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.Props
import actor.proto.SupervisorStrategy
import actor.proto.SupervisorDirective
import actor.proto.Supervisor
import actor.proto.fromProducer
import actor.proto.mailbox.Dispatcher
import actor.proto.mailbox.Mailbox
import actor.proto.mailbox.Dispatchers
import actor.proto.mailbox.newUnboundedMailbox
import ai.kastrax.core.agent.AgentBuilder
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.llm.LlmMessage
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 创建一个 Actor 化的 Agent，直接复用现有的 agent DSL
 */
fun ActorSystem.actorAgent(block: ActorAgentBuilder.() -> Unit): PID {
    val builder = ActorAgentBuilder()
    builder.block()

    // 验证必要的字段
    if (builder.agentBuilder.name.isEmpty()) {
        throw IllegalArgumentException("Agent name must not be empty")
    }

    // 创建一个简单的 Agent 实现
    val agent = object : Agent {
        override val name = builder.agentBuilder.name
        override val versionManager = null

        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions) =
            AgentResponse(text = "Response from $name")

        override suspend fun generate(prompt: String, options: AgentGenerateOptions) =
            AgentResponse(text = "Response from $name to: $prompt")

        override suspend fun stream(prompt: String, options: AgentStreamOptions) =
            AgentResponse(text = "Response from $name")

        override suspend fun getState() = AgentState(status = AgentStatus.IDLE)

        override suspend fun updateState(status: AgentStatus) =
            AgentState(status = status)

        override suspend fun createSession(title: String?, resourceId: String?, metadata: Map<String, String>) = null

        override suspend fun getSession(sessionId: String) = null

        override suspend fun getSessionMessages(sessionId: String, limit: Int) = emptyList<SessionMessage>()

        override suspend fun reset() {}

        override suspend fun createVersion(instructions: String, name: String?, description: String?,
                                         metadata: Map<String, String>, activateImmediately: Boolean) = null

        override suspend fun getVersions(limit: Int, offset: Int) = emptyList<AgentVersion>()

        override suspend fun getActiveVersion() = null

        override suspend fun activateVersion(versionId: String) = null

        override suspend fun rollbackToVersion(versionId: String) = null
    }

    // 创建 Props，应用 actor 配置
    var props = fromProducer { KastraxActor(agent) }
        .withMailbox(builder.actorBuilder.mailbox)
        .withDispatcher(builder.actorBuilder.dispatcher)

    // 应用监督策略
    builder.actorBuilder.supervisionStrategy?.let {
        props = props.withChildSupervisorStrategy(it)
    }

    // 使用 agent 名称或生成随机名称
    return if (agent.name != null && agent.name.isNotEmpty()) {
        this.root.spawnNamed(props, agent.name)
    } else {
        this.root.spawn(props)
    }
}

/**
 * Actor Agent 构建器，包含 agent 和 actor 两部分配置
 */
class ActorAgentBuilder {
    val agentBuilder = AgentBuilder()
    val actorBuilder = ActorBuilder()

    /**
     * 配置 agent 部分
     */
    fun agent(block: AgentBuilder.() -> Unit) {
        agentBuilder.apply(block)
    }

    /**
     * 配置 actor 部分
     */
    fun actor(block: ActorBuilder.() -> Unit) {
        actorBuilder.apply(block)
    }
}

/**
 * Actor 配置构建器
 */
class ActorBuilder {
    var supervisionStrategy: SupervisorStrategy? = null
    var mailbox: () -> Mailbox = { newUnboundedMailbox() }
    var dispatcher: Dispatcher = Dispatchers.DEFAULT_DISPATCHER

    /**
     * 监督策略配置
     */
    fun oneForOneStrategy(block: OneForOneStrategyBuilder.() -> Unit) {
        val builder = OneForOneStrategyBuilder()
        builder.block()
        supervisionStrategy = builder.build()
    }

    /**
     * 无界邮箱配置
     */
    fun unboundedMailbox() {
        mailbox = { newUnboundedMailbox() }
    }

    /**
     * 有界邮箱配置
     *
     * @param capacity 邮箱容量
     */
    fun boundedMailbox(capacity: Int) {
        mailbox = { BoundedMailbox(capacity) }
    }
}

/**
 * 监督策略构建器
 */
class OneForOneStrategyBuilder {
    var maxRetries: Int = 10
    var withinTimeRange: Duration = Duration.ofSeconds(10)
    var decider: (PID, Exception) -> SupervisorDirective = { _, _ -> SupervisorDirective.Restart }

    /**
     * 构建监督策略
     */
    fun build(): SupervisorStrategy {
        return actor.proto.OneForOneStrategy(decider, maxRetries, withinTimeRange)
    }
}

/**
 * 有界邮箱
 *
 * @property capacity 邮箱容量
 */
class BoundedMailbox(private val capacity: Int) : Mailbox {
    private val systemMessages = ConcurrentLinkedQueue<Any>()
    private val userMessages = ConcurrentLinkedQueue<Any>()
    private lateinit var invoker: actor.proto.mailbox.MessageInvoker
    private lateinit var dispatcher: Dispatcher

    override fun postUserMessage(msg: Any) {
        if (userMessages.size < capacity) {
            userMessages.add(msg)
        }
    }

    override fun postSystemMessage(msg: Any) {
        systemMessages.add(msg)
    }

    override fun registerHandlers(invoker: actor.proto.mailbox.MessageInvoker, dispatcher: Dispatcher) {
        this.invoker = invoker
        this.dispatcher = dispatcher
    }

    override fun start() {
        // 启动邮箱
    }

    override suspend fun run() {
        // 处理系统消息
        var systemMsg = systemMessages.poll()
        while (systemMsg != null) {
            invoker.invokeSystemMessage(systemMsg as actor.proto.mailbox.SystemMessage)
            systemMsg = systemMessages.poll()
        }

        // 处理用户消息
        for (i in 0 until dispatcher.throughput) {
            val userMsg = userMessages.poll() ?: break
            invoker.invokeUserMessage(userMsg)
        }

        // 如果还有消息，重新调度
        if (systemMessages.isNotEmpty() || userMessages.isNotEmpty()) {
            dispatcher.schedule(this)
        }
    }
}


