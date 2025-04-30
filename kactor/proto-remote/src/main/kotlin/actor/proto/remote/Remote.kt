package actor.proto.remote

import actor.proto.DeadLetterResponse
import actor.proto.EventStream
import actor.proto.MessageEnvelope
import actor.proto.PID
import actor.proto.ProcessRegistry
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.mailbox.newMpscUnboundedArrayMailbox
import actor.proto.remote.blocklist.BlocklistManager
import actor.proto.requestAwait
import actor.proto.send
import actor.proto.spawnNamed
import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

object Remote {
    private lateinit var _server: Server
    private val Kinds: HashMap<String, Props> = HashMap()
    lateinit var endpointManagerPid: PID
    lateinit var activatorPid: PID

    /**
     * Blocklist 管理器，用于管理远程节点的阻止列表
     */
    lateinit var blocklistManager: BlocklistManager
    fun getKnownKinds(): Set<String> = Kinds.keys
    fun registerKnownKind(kind: String, props: Props) {
        Kinds.put(kind, props)
    }

    fun getKnownKind(kind: String): Props {
        return Kinds.getOrElse(kind) {
            throw Exception("No Props found for kind '$kind'")
        }
    }

    var system: actor.proto.ActorSystem = actor.proto.ActorSystem.default()
        private set

    /**
     * Create a new Remote instance.
     * @param system The actor system to use
     * @param config The remote configuration
     * @return A new Remote instance
     */
    fun create(system: actor.proto.ActorSystem, config: RemoteConfig): Remote {
        this.system = system
        return this
    }

    /**
     * Shutdown the remote system.
     * @param graceful Whether to shutdown gracefully
     */
    fun shutdown(graceful: Boolean) {
        if (graceful) {
            _server.shutdown().awaitTermination(10, TimeUnit.SECONDS)
        } else {
            _server.shutdownNow()
        }

        // 关闭 Blocklist 管理器
        if (::blocklistManager.isInitialized) {
            blocklistManager.shutdown()
        }

        logger.info("Stopped Proto.Actor server")
    }

    fun start(hostname: String, port: Int, config: RemoteConfig = RemoteConfig(hostname, port)) {
        ProcessRegistry.registerHostResolver { pid -> RemoteProcess(pid) }
        val endpointReader = EndpointReader(this)
        val serverBuilder = NettyServerBuilder.forPort(port).addService(endpointReader)
        config.keepAliveTime?.let { serverBuilder.permitKeepAliveTime(config.keepAliveTime / 2, TimeUnit.MILLISECONDS) }
        config.keepAliveWithoutCalls?.let { serverBuilder.permitKeepAliveWithoutCalls(config.keepAliveWithoutCalls) }
        _server = serverBuilder.build().start()
        val boundPort: Int = _server.port
        val boundAddress: String = "$hostname:$boundPort"
        val address: String = "${config.advertisedHostname ?: hostname}:${config.advertisedPort ?: boundPort}"
        ProcessRegistry.address = address

        // 初始化 Blocklist 管理器
        if (config.enableBlocklist) {
            blocklistManager = BlocklistManager(this.system, config.blocklistCleanupInterval)
            blocklistManager.initialize()

            // 配置默认的阻止列表
            val defaultBlocklist = actor.proto.remote.blocklist.FailureCountingBlocklist(
                maxFailures = config.blocklistMaxFailures,
                blockDuration = config.blocklistDuration
            )
            blocklistManager.registerBlocklist("default", defaultBlocklist)

            logger.info("Blocklist enabled with max failures: ${config.blocklistMaxFailures}, duration: ${config.blocklistDuration}")
        } else {
            logger.info("Blocklist disabled")
        }

        spawnEndpointManager(config)
        spawnActivator()
        logger.info("Starting Proto.Actor server on $boundAddress")
    }

    private fun spawnActivator() {
        val props = fromProducer { Activator() }
        // 使用唯一的名称避免冲突
        val uniqueName = "activator-${System.currentTimeMillis()}-${System.nanoTime() % 10000}"
        activatorPid = spawnNamed(props, uniqueName)
    }

    private fun spawnEndpointManager(config: RemoteConfig) {
        val props = fromProducer { EndpointManager(config) }
                .withMailbox { newMpscUnboundedArrayMailbox(200) }
        // 使用唯一的名称避免冲突
        val uniqueName = "endpointmanager-${System.currentTimeMillis()}-${System.nanoTime() % 10000}"
        endpointManagerPid = spawnNamed(props, uniqueName)
        EventStream.subscribe({
            if (it is EndpointTerminatedEvent) {
                send(endpointManagerPid, it)
            }
        })
    }

    fun activatorForAddress(address: String): PID {
        // 使用当前 activator 的 ID
        val activatorId = activatorPid.id
        return PID(address, activatorId)
    }

    suspend fun spawn(address: String, kind: String, timeout: Duration): PID = spawnNamed(address, "", kind, timeout)

    suspend fun spawnNamed(address: String, name: String, kind: String, timeout: Duration): PID {
        val activator: PID = activatorForAddress(address)
        val req = ActorPidRequest(kind, name)
        val res = requestAwait<RemoteProtos.ActorPidResponse>(activator, req, timeout)
        // Convert the ActorProtos.PID to Protos.PID
        val pid = res.pid
        return PID(pid.address, pid.id)
    }

    fun sendMessage(pid: PID, msg: Any, serializerId: Int) {
        // 检查目标地址是否被阻止
        if (::blocklistManager.isInitialized && blocklistManager.isBlocked(pid.address)) {
            logger.debug { "Message not sent to blocked address: ${pid.address}" }

            // 如果消息有发送者，则发送死信响应
            val sender = when (msg) {
                is MessageEnvelope -> msg.sender
                else -> null
            }
            if (sender != null) {
                send(sender, DeadLetterResponse(pid))
            }

            return
        }

        val (message, sender) = when (msg) {
            is MessageEnvelope -> Pair(msg.message, msg.sender)
            else -> Pair(msg, null)
        }
        val env: RemoteDeliver = RemoteDeliver(message, pid, sender, serializerId)
        send(endpointManagerPid, env)
    }
}

