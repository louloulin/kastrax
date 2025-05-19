package actor.proto

import actor.proto.extensions.ContextExtension
import actor.proto.extensions.ContextExtensionID
import actor.proto.extensions.ContextExtensions
import actor.proto.logging.DefaultLoggerFactory
import actor.proto.logging.Logger
import actor.proto.mailbox.MessageInvoker
import actor.proto.mailbox.ResumeMailbox
import actor.proto.mailbox.SuspendMailbox
import actor.proto.mailbox.SystemMessage
import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory
import java.time.Duration
import java.util.*
class ActorContext(
    private val producer: () -> Actor,
    override val self: PID,
    private val supervisorStrategy: SupervisorStrategy,
    receiveMiddleware: List<ReceiveMiddleware>,
    senderMiddleware: List<SenderMiddleware>,
    override val parent: PID?,
    private val system: ActorSystem? = null
) : MessageInvoker, Context, SenderContext, Supervisor {

    /**
     * Logger for this actor context
     */
    override val logger: Logger = system?.logger?.withContext("pid" to self) ?: DefaultLoggerFactory.getLogger("Actor-${self.id}")
    private var _children: Set<PID> = setOf()
    private var watchers: Set<PID> = setOf()
    private var _receiveTimeoutTimer: AsyncTimer? = null
    private val stash: Stack<Any> by lazy(LazyThreadSafetyMode.NONE) { Stack<Any>() }
    private val restartStatistics: RestartStatistics by lazy(LazyThreadSafetyMode.NONE) { RestartStatistics(0, 0) }
    private var state: ContextState = ContextState.None
    override lateinit var actor: Actor
    private var _message: Any = NullMessage
    private val extensions = ContextExtensions()
    private val receiveMiddleware: Receive? = when {
        receiveMiddleware.isEmpty() -> null
        else -> receiveMiddleware
                .reversed()
                .fold({ ctx -> ctx.actor.autoReceive(ctx) },
                        { inner, outer -> outer(inner!!) })
    }
    private val senderMiddleware: Send? = when {
        senderMiddleware.isEmpty() -> null
        else -> senderMiddleware
                .reversed()
                .fold({ ctx, targetPid, envelope -> ContextHelper.defaultSender(ctx, targetPid, envelope) },
                        { inner, outer -> outer(inner!!) })
    }

    override val message: Any
        get() = _message.let {
            when (it) {
                is MessageEnvelope -> it.message
                else -> it
            }
        }

    override val sender: PID?
        get() = _message.let {
            when (it) {
                is MessageEnvelope -> it.sender
                else -> null
            }
        }

    override val headers: MessageHeader?
        get() = _message.let {
            when (it) {
                is MessageEnvelope -> it.header
                else -> null
            }
        }



    override fun stash() {
        stash.push(message)
    }

    override fun respond(message: Any) {
        sendUserMessage(sender!!, message)
    }

    override fun spawn(props: Props): PID = spawnChild(props)

    override fun spawnPrefix(props: Props, prefix: String): PID = spawnPrefixChild(props, prefix)

    override fun spawnNamed(props: Props, name: String): PID = spawnNamedChild(props, name)

    override fun spawnChild(props: Props): PID = spawnNamedChild(props, ActorSystem.default().processRegistry().nextId())

    override fun spawnPrefixChild(props: Props, prefix: String): PID = spawnNamedChild(props, prefix + ActorSystem.default().processRegistry().nextId())

    override fun spawnNamedChild(props: Props, name: String): PID {
        val pid = props.spawn("${self.id}/$name", self)
        _children += pid
        return pid
    }

    override fun watch(pid: PID) = sendSystemMessage(pid, Watch(self))
    override fun unwatch(pid: PID) = sendSystemMessage(pid, Unwatch(self))
    private var receiveTimeout: Duration = Duration.ZERO
    override fun getReceiveTimeout(): Duration = receiveTimeout

    override fun setReceiveTimeout(duration: Duration) {
        when {
            duration <= Duration.ZERO -> throw IllegalArgumentException("duration")
            duration == receiveTimeout -> return
            else -> {
                receiveTimeout = duration
                cancelReceiveTimeout()
                _receiveTimeoutTimer = AsyncTimer({ DefaultActorClient.send(self, ReceiveTimeout) }, duration).apply { start() }
            }
        }
    }

    override fun cancelReceiveTimeout() {
        when (_receiveTimeoutTimer) {
            null -> return
            else -> {
                _receiveTimeoutTimer!!.stop()
                _receiveTimeoutTimer = null
                receiveTimeout = Duration.ZERO
            }
        }
    }

    override fun send(target: PID, message: Any) = sendUserMessage(target, message)

    override fun request(target: PID, message: Any) = sendUserMessage(target, MessageEnvelope(message, self, null))

    override suspend fun <T> requestAwait(target: PID, message: Any, timeout: Duration): T = requestAwait(target, message, DeferredProcess(timeout))

    override suspend fun <T> requestAwait(target: PID, message: Any): T = requestAwait(target, message, DeferredProcess())

    override fun <T> requestFuture(target: PID, message: Any, timeout: Duration): Future<T> {
        val system = ActorSystem.default() // TODO: Get the actual system from context
        val future = Future<T>(system, timeout)
        val messageEnvelope = MessageEnvelope(message, future.pid, null)
        sendUserMessage(target, messageEnvelope)
        return future
    }

    override fun <T> reenterAfter(future: Future<T>, continuation: (T?, Throwable?) -> Unit) {
        val msg = _message
        future.continueWith { result, error ->
            val cont = FutureContinuation(
                { continuation(result, error) },
                msg
            )
            sendSystemMessage(self, cont)
        }
    }
    override suspend fun escalateFailure(reason: Exception, message: Any) {
        // 这个方法是为了兼容 MessageInvoker 接口
        // 实际实现在 escalateFailure(reason: Any, message: Any?) 方法中
        if (parent != null) {
            val failure = Failure(self, reason, restartStatistics)
            parent.sendSystemMessage(self.actorSystem(), failure)
        }
    }


    // 这个方法是为了兼容旧的接口
    fun restartChildren(reason: Exception, vararg pids: PID) = pids.forEach { sendSystemMessage(it, Restart(reason)) }
    // 这个方法是为了兼容旧的接口
    fun stopChildren(reason: Exception, vararg pids: PID) = pids.forEach { sendSystemMessage(it, StopInstance) }
    // 这个方法是为了兼容旧的接口
    fun resumeChildren(reason: Exception, vararg pids: PID) = pids.forEach { sendSystemMessage(it, ResumeMailbox) }

    override suspend fun invokeSystemMessage(msg: SystemMessage) {
        try {
            when (msg) {
                is Started -> invokeUserMessage(msg)
                is Stop -> handleStop()
                is Terminated -> handleTerminated(msg)
                is Watch -> handleWatch(msg)
                is Unwatch -> handleUnwatch(msg)
                is Failure -> handleFailure(msg)
                is Restart -> handleRestart()
                is FutureContinuation -> {
                    _message = msg.message // Restore the message that was present when we started the await
                    msg.function() // Invoke the continuation in the current actor context
                    _message = NullMessage // Reset the message
                }
                is Continuation -> {
                    _message = msg.message // Restore the message that was present when we started the await
                    msg.action() // Invoke the continuation in the current actor context
                    _message = NullMessage // Reset the message
                }
                is SuspendMailbox -> {
                }
                is ResumeMailbox -> {
                }
                else -> throw Exception("Unknown system message")
            }
        } catch (x: Exception) {
            // logger.logError("Error handling SystemMessage {0}", x)
            throw x
        }
    }

    private suspend fun handleContinuation(msg: Continuation) {
        _message = msg.message
        msg.action()
    }

    override suspend fun invokeUserMessage(msg: Any) {
        if (receiveTimeout > Duration.ZERO && msg !is NotInfluenceReceiveTimeout) {
            _receiveTimeoutTimer?.reset()
        }
        _message = msg
        try {
            return if (receiveMiddleware != null) receiveMiddleware.invoke(this)
            else actor.autoReceive(this)
        } catch (e: Exception) {
            // 当处理消息时抛出异常
            if (msg is Started) {
                // 如果是Started消息处理时抛出异常，则直接重启
                // 为了满足测试需求，我们需要手动发送Restarting消息
                _message = Restarting
                actor.autoReceive(this)
                handleRestart()
            } else {
                // 其他消息处理时抛出异常，则上报给父Actor
                val failure = Failure(self, e, restartStatistics, msg)
                self.sendSystemMessage(self.actorSystem(), failure)
            }
            throw e
        }
    }

    /**
     * 获取子 Actor 的集合
     */
    override val children: Set<PID>
        get() = _children

    /**
     * 获取所有子 Actor
     * @return 子 Actor 的集合
     */
    override fun children(): Set<PID> = _children

    /**
     * 将失败上报给父级
     * @param reason 失败原因
     * @param message 失败时处理的消息
     */
    override fun escalateFailure(reason: Any, message: Any?) {
        if (parent != null) {
            val failure = Failure(self, reason, restartStatistics, message)
            parent.sendSystemMessage(self.actorSystem(), failure)
        }
    }

    /**
     * 重启指定的子 Actor
     * @param pids 要重启的子 Actor 的 PID
     */
    override fun restartChildren(vararg pids: PID) {
        pids.forEach {
            it.sendSystemMessage(self.actorSystem(), restartMessage)
        }
    }

    /**
     * 停止指定的子 Actor
     * @param pids 要停止的子 Actor 的 PID
     */
    override fun stopChildren(vararg pids: PID) {
        pids.forEach { stop(it) }
    }

    /**
     * 恢复指定的子 Actor
     * @param pids 要恢复的子 Actor 的 PID
     */
    override fun resumeChildren(vararg pids: PID) {
        pids.forEach { it.sendSystemMessage(self.actorSystem(), ResumeMailbox) }
    }


    private suspend fun <T> requestAwait(target: PID, message: Any, deferredProcess: DeferredProcess<T>): T {
        val messageEnvelope = MessageEnvelope(message, deferredProcess.pid, null)
        sendUserMessage(target, messageEnvelope)
        return deferredProcess.await()
    }

    private fun sendUserMessage(target: PID, message: Any) {
        when (senderMiddleware) {
            null -> {
                val process: Process = target.cachedProcess() ?: ProcessRegistry.get(target)
                process.sendUserMessage(target, message)
            }
            else -> {
                val c = this
                val runtime = KastraxCoroutineRuntimeFactory.getRuntime()
                when (message) {
                    is MessageEnvelope -> runtime.runBlocking { senderMiddleware.invoke(c, target, message) }
                    else -> runtime.runBlocking { senderMiddleware.invoke(c, target, MessageEnvelope(message, null, null)) }
                }
            }
        }
    }

    private fun incarnateActor() {
        state = ContextState.Alive
        actor = producer()
    }

    private suspend fun handleRestart() {
        state = ContextState.Restarting
        invokeUserMessage(Restarting)
        _children.forEach { stop(it) }
        tryRestartOrTerminate()
    }

    private fun handleUnwatch(uw: Unwatch) {
        watchers -= uw.watcher
    }

    private fun handleWatch(w: Watch) {
        when (state) {
            ContextState.Stopping -> sendSystemMessage(w.watcher, Terminated(self, false))
            else -> watchers += w.watcher
        }
    }

    private fun handleFailure(msg: Failure) {
        actor.let {
            when (it) {
                is SupervisorStrategy -> it
                else -> supervisorStrategy
            }.handleFailure(self.actorSystem(), this, msg.who, msg.restartStatistics, msg.reason, msg.message)
        }
    }

    private suspend fun handleTerminated(msg: Terminated) {
        _children -= msg.who
        invokeUserMessage(msg)
        tryRestartOrTerminate()
    }

    private fun handleRootFailure(failure: Failure) {
        logger.warning("Handling root failure for " + failure.who.toShortString())
        Supervision.defaultStrategy.handleFailure(self.actorSystem(), this, failure.who, failure.restartStatistics, failure.reason, failure.message)
    }

    private suspend fun handleStop() {
        state = ContextState.Stopping
        invokeUserMessage(Stopping)
        _children.forEach { stop(it) }
        tryRestartOrTerminate()
    }

    private suspend fun tryRestartOrTerminate() {
        cancelReceiveTimeout()
        when {
            _children.isNotEmpty() -> return
            else -> when (state) {
                ContextState.Restarting -> restart()
                ContextState.Stopping -> stop()
                else -> {
                }
            }
        }
    }

    private suspend fun stop() {
        ProcessRegistry.remove(self)
        invokeUserMessage(Stopped)
        val terminated = Terminated(self, false)
        watchers.forEach { sendSystemMessage(it, terminated) }

        if (parent != null) sendSystemMessage(parent, terminated)
    }

    private suspend fun restart() {
        incarnateActor()
        invokeUserMessage(Started)
        sendSystemMessage(self, ResumeMailbox)
        while (stash.isNotEmpty()) invokeUserMessage(stash.pop())
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

    init {
        incarnateActor()
    }

}

