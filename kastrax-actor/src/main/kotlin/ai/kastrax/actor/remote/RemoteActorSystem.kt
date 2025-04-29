package ai.kastrax.actor.remote

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.ActorAgentBuilder
import ai.kastrax.actor.actorAgent
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.agent

/**
 * 配置远程 Actor 系统
 *
 * @param name 系统名称
 * @param config 远程配置
 * @return 配置好的 ActorSystem
 */
fun configureRemoteActorSystem(name: String, config: RemoteActorConfig = RemoteActorConfig()): ActorSystem {
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
 * 连接到远程 Actor 系统
 *
 * @param address 远程地址，格式为 "hostname:port"
 * @param systemName 本地系统名称
 * @return RemoteAgent 对象
 */
fun connectToRemoteSystem(address: String, systemName: String = "kastrax-client"): RemoteAgent {
    val system = ActorSystem(systemName)
    return RemoteAgent(system, address)
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
