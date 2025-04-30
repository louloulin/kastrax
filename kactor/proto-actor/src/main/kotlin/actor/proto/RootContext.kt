package actor.proto

import actor.proto.extensions.ContextExtension
import actor.proto.extensions.ContextExtensionID
import actor.proto.extensions.ContextExtensions
import actor.proto.logging.Logger
import actor.proto.middleware.SpawnFunc
import actor.proto.middleware.SpawnMiddleware
import actor.proto.middleware.makeSpawnMiddlewareChain
import actor.proto.middleware.makeSenderMiddlewareChain
import kotlinx.coroutines.runBlocking
import java.time.Duration

/**
 * RootContext 是顶级 Context 实现，用于创建顶级 Actor
 * @param actorSystem Actor 系统
 */
class RootContext(val actorSystem: ActorSystem) : SenderContext, SpawnerContext, ExtensionContext {
    /**
     * Logger for the root context
     */
    override val logger: Logger = actorSystem.logger.withContext("context" to "root")
    private var _headers: Map<String, String> = mapOf()
    private var senderMiddleware: Send? = null
    private var spawnMiddleware: SpawnFunc? = null
    private var guardianStrategy: SupervisorStrategy? = null
    private val extensions = ContextExtensions()

    /**
     * 获取当前 Actor 的 PID
     * @return 当前 Actor 的 PID
     */
    override val self: PID
        get() = if (guardianStrategy != null) {
            actorSystem.guardians.getGuardianPid(guardianStrategy!!)
        } else {
            actorSystem.deadLetter()
        }

    /**
     * 获取当前 Actor
     * @return 当前 Actor
     */
    override val actor: Actor? = null

    /**
     * 获取消息
     * @return 消息
     */
    override val message: Any
        get() = NullMessage

    /**
     * 获取消息头
     * @return 消息头
     */
    override val headers: MessageHeader?
        get() = null

    /**
     * 获取发送者
     * @return 发送者
     */
    override val sender: PID?
        get() = null

    /**
     * 创建一个新的 RootContext 实例，带有指定的消息头
     * @param headers 消息头
     * @return 新的 RootContext 实例
     */
    fun withHeaders(headers: Map<String, String>): RootContext {
        val ctx = RootContext(actorSystem)
        ctx._headers = headers
        ctx.senderMiddleware = this.senderMiddleware
        ctx.spawnMiddleware = this.spawnMiddleware
        ctx.guardianStrategy = this.guardianStrategy
        return ctx
    }

    /**
     * 创建一个新的 RootContext 实例，带有指定的发送中间件
     * @param middleware 发送中间件
     * @return 新的 RootContext 实例
     */
    fun withSenderMiddleware(vararg middleware: SenderMiddleware): RootContext {
        val ctx = RootContext(actorSystem)
        ctx._headers = this._headers
        ctx.senderMiddleware = makeSenderMiddlewareChain(middleware.toList(), { ctx: SenderContext, target: PID, envelope: MessageEnvelope ->
            val process = actorSystem.processRegistry().get(target)
            process.sendUserMessage(target, envelope)
        })
        ctx.spawnMiddleware = this.spawnMiddleware
        ctx.guardianStrategy = this.guardianStrategy
        return ctx
    }

    /**
     * 创建一个新的 RootContext 实例，带有指定的创建中间件
     * @param middleware 创建中间件
     * @return 新的 RootContext 实例
     */
    fun withSpawnMiddleware(vararg middleware: SpawnMiddleware): RootContext {
        val ctx = RootContext(actorSystem)
        ctx._headers = this._headers
        ctx.senderMiddleware = this.senderMiddleware
        ctx.spawnMiddleware = makeSpawnMiddlewareChain(middleware.toList()) { system, id, props, parentContext ->
            system.actorOf(props, id)
        }
        ctx.guardianStrategy = this.guardianStrategy
        return ctx
    }

    /**
     * 创建一个新的 RootContext 实例，带有指定的 Guardian 策略
     * @param strategy 监督策略
     * @return 新的 RootContext 实例
     */
    fun withGuardian(strategy: SupervisorStrategy): RootContext {
        val ctx = RootContext(actorSystem)
        ctx._headers = this._headers
        ctx.senderMiddleware = this.senderMiddleware
        ctx.spawnMiddleware = this.spawnMiddleware
        ctx.guardianStrategy = strategy
        return ctx
    }

    /**
     * 获取指定ID的上下文扩展
     * @param id 扩展ID
     * @return 扩展实例，如果不存在则返回null
     */
    override fun get(id: ContextExtensionID): ContextExtension? {
        return extensions.get(id)
    }

    /**
     * 设置上下文扩展
     * @param extension 扩展实例
     */
    override fun set(extension: ContextExtension) {
        extensions.set(extension)
    }

    /**
     * 发送消息给指定的 Actor
     * @param target 目标 Actor 的 PID
     * @param message 消息
     */
    override fun send(target: PID, message: Any) {
        when (senderMiddleware) {
            null -> {
                val process = actorSystem.processRegistry().get(target)
                process.sendUserMessage(target, message)
            }
            else -> {
                val envelope = when (message) {
                    is MessageEnvelope -> message
                    else -> MessageEnvelope(message, null, null)
                }
                runBlocking { senderMiddleware!!.invoke(this@RootContext, target, envelope) }
            }
        }
    }

    /**
     * 发送请求给指定的 Actor
     * @param target 目标 Actor 的 PID
     * @param message 消息
     */
    override fun request(target: PID, message: Any) {
        val envelope = MessageEnvelope(message, self, null)
        send(target, envelope)
    }

    /**
     * 创建一个新的 Actor
     * @param props Actor 的属性
     * @return 新 Actor 的 PID
     */
    override fun spawn(props: Props): PID {
        return when (spawnMiddleware) {
            null -> actorSystem.actorOf(props)
            else -> spawnMiddleware!!.invoke(actorSystem, actorSystem.processRegistry().nextId(), props, this)
        }
    }

    /**
     * 创建一个带前缀的新 Actor
     * @param props Actor 的属性
     * @param prefix Actor 名称的前缀
     * @return 新 Actor 的 PID
     */
    override fun spawnPrefix(props: Props, prefix: String): PID {
        val name = prefix + actorSystem.processRegistry().nextId()
        return spawnNamed(props, name)
    }

    /**
     * 创建一个指定名称的新 Actor
     * @param props Actor 的属性
     * @param name Actor 的名称
     * @return 新 Actor 的 PID
     */
    override fun spawnNamed(props: Props, name: String): PID {
        return when (spawnMiddleware) {
            null -> actorSystem.actorOf(props, name)
            else -> spawnMiddleware!!.invoke(actorSystem, name, props, this)
        }
    }

    /**
     * 停止指定的 Actor
     * @param pid 要停止的 Actor 的 PID
     */
    fun stop(pid: PID) {
        actorSystem.stop(pid)
    }

    /**
     * 发送毒丸消息给指定的 Actor
     * @param pid 目标 Actor 的 PID
     */
    fun poison(pid: PID) {
        actorSystem.poison(pid)
    }

    /**
     * 发送请求并等待响应
     * @param target 目标 Actor 的 PID
     * @param message 消息
     * @param timeout 超时时间
     * @return 响应
     */
    suspend fun <T> requestAwait(target: PID, message: Any, timeout: Duration): T {
        // 创建一个 Future 并发送请求
        val future = Future<T>(actorSystem, timeout)
        val process = actorSystem.processRegistry().get(target)
        val envelope = MessageEnvelope(message, future.pid, null)
        process.sendUserMessage(target, envelope)
        return future.result()
    }

    /**
     * 发送请求并等待响应
     * @param target 目标 Actor 的 PID
     * @param message 消息
     * @return 响应
     */
    suspend fun <T> requestAwait(target: PID, message: Any): T {
        return actorSystem.requestAsync(target, message, Duration.ofSeconds(5))
    }
}
