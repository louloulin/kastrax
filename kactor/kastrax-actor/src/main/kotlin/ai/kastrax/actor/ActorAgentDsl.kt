package ai.kastrax.actor

import actor.proto.ActorSystem
import actor.proto.DefaultStrategy
import actor.proto.Dispatcher
import actor.proto.MailboxProducer
import actor.proto.PID
import actor.proto.Props
import actor.proto.SupervisorDirective
import actor.proto.SupervisorStrategy
import actor.proto.UnboundedMailboxProducer
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
    var supervisionStrategy: SupervisorStrategy = DefaultStrategy
    var mailbox: MailboxProducer = UnboundedMailboxProducer
    var dispatcher: Dispatcher? = null

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
        mailbox = UnboundedMailboxProducer
    }

    /**
     * 有界邮箱配置
     *
     * @param capacity 邮箱容量
     */
    fun boundedMailbox(capacity: Int) {
        mailbox = BoundedMailboxProducer(capacity)
    }
}

/**
 * 监督策略构建器
 */
class OneForOneStrategyBuilder {
    var maxRetries: Int = 10
    var withinTimeRange: Duration = 10.seconds
    var decider: (Exception) -> SupervisorDirective = { SupervisorDirective.Restart }

    /**
     * 构建监督策略
     */
    fun build(): SupervisorStrategy {
        return OneForOneStrategy(maxRetries, withinTimeRange, decider)
    }
}

/**
 * 有界邮箱生产者
 *
 * @property capacity 邮箱容量
 */
class BoundedMailboxProducer(private val capacity: Int) : MailboxProducer {
    override fun create() = actor.proto.BoundedMailbox(capacity)
}

/**
 * 一对一监督策略
 *
 * @property maxRetries 最大重试次数
 * @property withinTimeRange 时间范围
 * @property decider 决策函数
 */
class OneForOneStrategy(
    private val maxRetries: Int,
    private val withinTimeRange: Duration,
    private val decider: (Exception) -> SupervisorDirective
) : SupervisorStrategy {
    override fun handleFailure(
        supervisor: actor.proto.Supervisor,
        child: PID,
        rs: Int,
        cause: Exception,
        message: Any?
    ) {
        val directive = decider(cause)
        when (directive) {
            SupervisorDirective.Resume -> supervisor.resumeChildren(child)
            SupervisorDirective.Restart -> supervisor.restartChildren(child)
            SupervisorDirective.Stop -> supervisor.stopChildren(child)
            SupervisorDirective.Escalate -> supervisor.escalateFailure(cause, message)
        }
    }
}

/**
 * 创建一个 Actor 化的 Agent
 *
 * @param block 配置 Actor Agent 的代码块
 * @return Agent 的 PID
 */
fun ActorSystem.actorAgent(block: ActorAgentBuilder.() -> Unit): PID {
    val builder = ActorAgentBuilder()
    builder.block()
    val agent = builder.agentBuilder.build()

    // 创建 Props，应用 actor 配置
    var props = actor.proto.fromProducer { KastraxActor(agent) }
        .withMailbox(builder.actorBuilder.mailbox)

    // 如果指定了 dispatcher，则应用它
    builder.actorBuilder.dispatcher?.let {
        props = props.withDispatcher(it)
    }

    // 应用监督策略
    props = props.withSupervisor(builder.actorBuilder.supervisionStrategy)

    // 使用 agent 名称或生成随机名称
    return if (agent.name.isNotEmpty()) {
        this.root.spawnNamed(props, agent.name)
    } else {
        this.root.spawn(props)
    }
}
