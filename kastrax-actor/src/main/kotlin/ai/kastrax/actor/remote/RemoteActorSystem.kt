package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.remote.RemoteConfig as KactorRemoteConfig
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.ActorAgentBuilder
import ai.kastrax.actor.RemoteAgent
import ai.kastrax.actor.actorAgent
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.agent

/**
 * 配置远程 Actor 系统
 *
 * @param port 端口号
 * @param systemName 系统名称
 * @param hostname 主机名，默认为 "0.0.0.0"
 * @param advertisedHostname 对外公布的主机名，默认为 hostname
 * @return 配置好的 ActorSystem
 */
fun configureRemoteActorSystem(port: Int, systemName: String, hostname: String = "0.0.0.0", advertisedHostname: String = hostname): ActorSystem {
    println("Configuring remote actor system: name=$systemName, hostname=$hostname, port=$port, advertisedHostname=$advertisedHostname")

    // 使用 127.0.0.1 而不是 localhost，避免主机名解析问题
    val effectiveAdvertisedHostname = if (advertisedHostname == "localhost") "127.0.0.1" else advertisedHostname

    // 创建 kactor 远程配置
    val config = KactorRemoteConfig(
        hostname = hostname,
        port = port,
        advertisedHostname = effectiveAdvertisedHostname,
        advertisedPort = port
    )

    // 创建 ActorSystem，使用传入的系统名称
    val system = ActorSystem(systemName)

    // 初始化远程系统
    val remote = actor.proto.remote.Remote.create(system, config)
    remote.start(hostname, port, config)

    // 等待远程系统启动
    Thread.sleep(1000)

    // 记录远程系统的地址
    val address = "$effectiveAdvertisedHostname:$port"
    println("Remote system address: $address")

    println("Remote actor system started: $systemName at $hostname:$port (advertised as $effectiveAdvertisedHostname:$port)")

    return system
}

/**
 * 连接到远程 Actor 系统
 *
 * @param address 远程地址
 * @param port 端口号
 * @return RemoteAgent 对象
 */
fun connectToRemoteSystem(address: String, port: Int, systemName: String = "kastrax-client-${System.currentTimeMillis()}-${System.nanoTime() % 10000}"): RemoteAgent {
    println("Connecting to remote system at $address:$port with client system name: $systemName")
    val system = ActorSystem(systemName)
    val remoteAddress = "$address:$port"
    val remoteAgent = RemoteAgent(system, remoteAddress)
    println("Created remote agent connection to $remoteAddress")
    return remoteAgent
}

/**
 * 配置远程 Actor 系统（使用 RemoteActorConfig）
 *
 * @param name 系统名称
 * @param config 远程配置
 * @return 配置好的 ActorSystem
 */
fun configureRemoteActorSystemWithConfig(name: String, config: RemoteActorConfig = RemoteActorConfig()): ActorSystem {
    // 创建 kactor 远程配置
    val kactorConfig = config.toKactorRemoteConfig()

    // 创建 ActorSystem
    val system = ActorSystem(name)

    // 初始化远程系统
    val remote = actor.proto.remote.Remote.create(system, kactorConfig)
    remote.start(config.hostname, config.port, kactorConfig)

    return system
}

/**
 * 在远程 Actor 系统中注册 Agent
 *
 * @param agent Agent 实例
 * @param name Agent 名称
 * @return PID 对象
 */
fun ActorSystem.registerRemoteAgent(agent: Agent, name: String): PID {
    // 创建一个代理 Agent
    val proxyAgent = ProxyAgent(agent)

    // 使用 actorAgent DSL 创建 Actor
    return this.actorAgent {
        // 设置代理 Agent 的名称
        agentBuilder.name = name

        actor {
            // 远程 Actor 特有的配置
            oneForOneStrategy {
                maxRetries = 5
                withinTimeRange = java.time.Duration.ofMinutes(1)
            }
            // 使用无界邮箱，适合远程通信
            unboundedMailbox()
        }
    }
}

/**
 * 创建远程地址字符串
 *
 * @param hostname 主机名
 * @param port 端口号
 * @return 格式为 "hostname:port" 的地址字符串
 */
fun remoteAddress(hostname: String, port: Int): String {
    return "$hostname:$port"
}
