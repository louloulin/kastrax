package actor.proto

import actor.proto.diagnostics.Diagnostics
import actor.proto.diagnostics.MatchType
import actor.proto.diagnostics.ProcessInfo
import actor.proto.guardian.GuardiansValue
import actor.proto.logging.DefaultLoggerFactory
import actor.proto.logging.Logger
import actor.proto.metrics.MetricsRegistry
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.future.await

/**
 * ActorSystem is the central management unit for actors.
 * It provides methods for creating, finding, and managing actors.
 */
class ActorSystem(val name: String) {
    /**
     * Logger for the actor system
     */
    val logger: Logger = DefaultLoggerFactory.getLogger("ActorSystem-$name")
    private val processRegistryImpl = ProcessRegistryImpl(this)

    /**
     * 度量注册表，用于收集系统度量
     */
    val metrics = MetricsRegistry()

    val address: String
        get() = processRegistryImpl.address

    fun eventStream(): EventStreamImpl = EventStream
    val root = RootContext(this)
    private val diagnostics = Diagnostics(this)
    private val hostResolvers = mutableListOf<(PID) -> Process?>()
    val scheduler = Scheduler()
    val deadLetter: Process = DeadLetterProcess
    val guardians = GuardiansValue(this)
    private val plugins = mutableMapOf<String, Any>()

    init {
        processRegistryImpl.registerHostResolver { pid ->
            hostResolvers.forEach { resolver ->
                val process = resolver(pid)
                if (process != null) {
                    return@registerHostResolver process
                }
            }
            deadLetter
        }

        logger.info("Actor system started", "id" to name)
    }

    /**
     * Get the root context
     * @return The root context
     */
    fun root(): RootContext = root

    /**
     * Get the PID of the dead letter actor
     * @return The PID of the dead letter actor
     */
    fun deadLetter(): PID = PID(processRegistryImpl.address, "deadletter")

    /**
     * Create a new actor with the given props and a generated ID
     * @param props The props to create the actor with
     * @return The PID of the new actor
     */
    fun actorOf(props: Props): PID {
        val name = processRegistryImpl.nextId()
        val startTime = System.nanoTime()
        val pid = actorOf(props, name)
        val duration = System.nanoTime() - startTime

        // 记录 actor 创建度量
        metrics.counter("actor.created", mapOf("system" to this.name)).inc()
        metrics.histogram("actor.creation.time", mapOf("system" to this.name)).observe(duration / 1_000_000.0)

        logger.debug("Actor created", "pid" to pid, "duration_ms" to (duration / 1_000_000.0))

        return pid
    }

    /**
     * Create a new actor with the given props and name
     * @param props The props to create the actor with
     * @param name The name of the actor
     * @return The PID of the new actor
     */
    fun actorOf(props: Props, name: String): PID {
        val startTime = System.nanoTime()

        val mailbox = props.mailboxProducer()
        val dispatcher = props.dispatcher
        val process = LocalProcess(mailbox)
        val self = processRegistryImpl.put(name, process)
        val ctx = ActorContext(props.producer!!, self, props.supervisorStrategy, props.receiveMiddleware, props.senderMiddleware, null, this)
        mailbox.registerHandlers(ctx, dispatcher)
        mailbox.postSystemMessage(Started)
        mailbox.start()

        val duration = System.nanoTime() - startTime
        metrics.counter("actor.created.named", mapOf(
            "system" to this.name,
            "actor_name" to name
        )).inc()
        metrics.histogram("actor.creation.time.named", mapOf(
            "system" to this.name,
            "actor_name" to name
        )).observe(duration / 1_000_000.0)

        logger.debug("Named actor created", "pid" to self, "name" to name, "duration_ms" to (duration / 1_000_000.0))

        return self
    }

    /**
     * Stop the actor with the given PID
     * @param pid The PID of the actor to stop
     */
    fun stop(pid: PID) {
        val process = processRegistryImpl.get(pid)
        process.stop(pid)

        // 记录 actor 停止度量
        metrics.counter("actor.stopped", mapOf(
            "system" to this.name,
            "actor_id" to pid.id
        )).inc()

        logger.debug("Actor stopped", "pid" to pid)
    }

    /**
     * Send a poison pill to the actor with the given PID
     * @param pid The PID of the actor to poison
     */
    fun poison(pid: PID) {
        logger.debug("Sending poison pill", "pid" to pid)
        send(pid, PoisonPill.getDefaultInstance())
    }

    /**
     * Send a message to the actor with the given PID
     * @param pid The PID of the actor to send the message to
     * @param message The message to send
     */
    fun send(pid: PID, message: Any) {
        val startTime = System.nanoTime()
        val process = processRegistryImpl.get(pid)
        process.sendUserMessage(pid, message)
        val duration = System.nanoTime() - startTime

        // 记录消息发送度量
        metrics.counter("actor.messages.sent", mapOf(
            "system" to this.name,
            "message_type" to message.javaClass.simpleName
        )).inc()
        metrics.histogram("actor.messages.sending.time", mapOf(
            "system" to this.name,
            "message_type" to message.javaClass.simpleName
        )).observe(duration / 1_000_000.0)
    }

    /**
     * Send a message to the actor with the given PID and set the sender
     * @param pid The PID of the actor to send the message to
     * @param message The message to send
     * @param sender The PID of the sender
     */
    fun request(pid: PID, message: Any, sender: PID) {
        val process = processRegistryImpl.get(pid)
        val envelope = MessageEnvelope(message, sender)
        process.sendUserMessage(pid, envelope)
    }

    /**
     * Send a message to the actor with the given PID and wait for a response
     * @param pid The PID of the actor to send the message to
     * @param message The message to send
     * @param timeout The timeout for the request
     * @return The response from the actor
     */
    suspend fun <T> requestAsync(pid: PID, message: Any, timeout: Duration): T {
        return root.requestAwait(pid, message, timeout)
    }

    /**
     * Register a host resolver
     * @param resolver The resolver function
     */
    fun registerHostResolver(resolver: (PID) -> Process?) {
        hostResolvers.add(resolver)
    }

    /**
     * 获取进程注册表实现
     * @return 进程注册表实现
     */
    fun processRegistry(): ProcessRegistryImpl = processRegistryImpl

    /**
     * 注册插件
     * @param plugin 插件实例
     */
    fun registerPlugin(plugin: Any) {
        // 使用反射获取id和version
        val idMethod = plugin.javaClass.getMethod("id")
        val versionMethod = plugin.javaClass.getMethod("version")
        val initMethod = plugin.javaClass.getMethod("init", ActorSystem::class.java)

        val id = idMethod.invoke(plugin) as String
        val version = versionMethod.invoke(plugin) as String

        plugins[id] = plugin
        initMethod.invoke(plugin, this)

        logger.info("Plugin registered", "id" to id, "version" to version)
    }

    /**
     * 获取插件
     * @param id 插件ID
     * @return 插件实例，如果不存在则返回null
     */
    fun getPlugin(id: String): Any? = plugins[id]

    /**
     * Get information about a process
     * @param pid The PID of the process
     * @return The process information
     */
    fun getProcessInfo(pid: PID): ProcessInfo {
        return diagnostics.getProcessInfo(pid)
    }

    /**
     * List processes matching a pattern
     * @param pattern The pattern to match
     * @param matchType The type of matching to perform
     * @return The list of PIDs matching the pattern
     */
    fun listProcesses(pattern: String, matchType: MatchType): List<PID> {
        return diagnostics.listProcesses(pattern, matchType)
    }

    /**
     * Get all processes in the system
     * @return A list of all PIDs in the system
     */
    fun getAllProcesses(): List<PID> {
        return diagnostics.getAllProcesses()
    }

    /**
     * Get detailed information about all processes matching a pattern
     * @param pattern The pattern to match
     * @param matchType The type of matching to perform
     * @return A list of ProcessInfo objects for all matching processes
     */
    fun getProcessInfos(pattern: String, matchType: MatchType): List<ProcessInfo> {
        return diagnostics.getProcessInfos(pattern, matchType)
    }

    /**
     * Get detailed information about all processes in the system
     * @return A list of ProcessInfo objects for all processes
     */
    fun getAllProcessInfos(): List<ProcessInfo> {
        return diagnostics.getAllProcessInfos()
    }

    /**
     * 关闭 Actor 系统
     */
    fun shutdown() {
        // 关闭所有插件
        plugins.values.forEach { plugin ->
            try {
                val shutdownMethod = plugin.javaClass.getMethod("shutdown")
                shutdownMethod.invoke(plugin)
            } catch (e: Exception) {
                logger.error("Failed to shutdown plugin", e)
            }
        }
        plugins.clear()

        // 关闭调度器
        scheduler.shutdown()

        logger.info("Actor system shutdown", "id" to name)
    }

    companion object {
        private val systems = ConcurrentHashMap<String, ActorSystem>()
        private val defaultSystemRef = AtomicReference<ActorSystem>(null)

        /**
         * 获取默认的 ActorSystem 实例
         * @return 默认的 ActorSystem 实例
         */
        fun default(): ActorSystem {
            var system = defaultSystemRef.get()
            if (system == null) {
                system = ActorSystem("default")
                if (!defaultSystemRef.compareAndSet(null, system)) {
                    system = defaultSystemRef.get()
                }
            }
            return system
        }

        /**
         * Get or create an ActorSystem with the given name
         * @param name The name of the ActorSystem
         * @return The ActorSystem
         */
        fun get(name: String): ActorSystem {
            return systems.getOrPut(name) { ActorSystem(name) }
        }

        // 已经在上面定义了 default() 方法
    }
}
