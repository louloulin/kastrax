package ai.kastrax.actor.cluster

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.Kind
import actor.proto.fromProducer
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.actorAgent
import ai.kastrax.core.agent.Agent
import kotlinx.coroutines.runBlocking

/**
 * 配置集群 Actor 系统
 *
 * @param name 系统名称
 * @param config 集群配置
 * @return 配置好的 ActorSystem
 */
fun configureCluster(name: String, config: ClusterConfig = ClusterConfig()): ActorSystem {
    // 创建 ActorSystem
    val system = ActorSystem(name)

    // 获取集群实例（会自动创建并缓存）
    system.getCluster(config)

    return system
}

/**
 * 集群实例缓存
 */
private val clusterCache = mutableMapOf<String, Cluster>()

/**
 * 获取集群实例
 *
 * @param config 集群配置，如果不提供则使用默认配置
 * @return 集群实例
 */
fun ActorSystem.getCluster(config: ClusterConfig = ClusterConfig()): Cluster {
    // 使用系统地址作为缓存键
    val cacheKey = this.address

    // 如果缓存中存在则直接返回
    return clusterCache.getOrPut(cacheKey) {
        // 创建 kactor 集群配置
        val kactorConfig = config.toKactorClusterConfig()

        // 创建集群实例
        Cluster.create(this, kactorConfig)
    }
}

/**
 * 加入集群
 */
fun ActorSystem.joinCluster() {
    // 获取集群实例
    val cluster = getCluster()

    // 启动集群成员
    runBlocking {
        cluster.startMember()
    }
}

/**
 * 离开集群
 */
fun ActorSystem.leaveCluster() {
    // 获取集群实例
    val cluster = getCluster()

    // 关闭集群
    runBlocking {
        cluster.shutdown(true)
    }
}

/**
 * 获取集群成员列表
 */
fun ActorSystem.getClusterMembers(): List<String> {
    // 获取集群实例
    val cluster = getCluster()

    // 返回成员列表
    return cluster.memberList.getMembers().map { it.address() }
}

/**
 * 在集群中注册 Agent
 *
 * @param agent Agent 实例
 * @param kind 类型名称
 * @param id 可选的 ID，如果不提供则使用 Agent 的名称
 * @return PID 对象
 */
fun ActorSystem.registerClusterAgent(agent: Agent, kind: String, id: String? = null): PID {
    // 获取集群实例
    val cluster = getCluster()

    // 创建 Actor Props
    val props = fromProducer { KastraxActor(agent) }

    // 创建 Kind
    val actorKind = Kind(kind, props)

    // 注册 Kind
    cluster.registerKind(actorKind)

    // 获取或创建虚拟 Actor
    val actorId = id ?: agent.name
    return runBlocking { cluster.get(actorId, kind) }
}

/**
 * 获取集群中的 Agent
 *
 * @param kind 类型名称
 * @param id 可选的 ID
 * @return PID 对象
 */
fun ActorSystem.getClusterAgent(kind: String, id: String? = null): PID? {
    // 获取集群实例
    val cluster = getCluster()

    // 获取指定类型的 PID
    return if (id != null) {
        runBlocking { cluster.get(id, kind) }
    } else {
        // 如果没有指定 ID，则返回 null
        // 注意：kactor 不支持直接获取所有特定类型的 Actor
        null
    }
}

/**
 * 广播消息给集群中的所有 Agent
 *
 * @param kind 类型名称
 * @param message 消息
 */
fun ActorSystem.broadcastToCluster(kind: String, message: Any) {
    // 获取集群实例
    val cluster = getCluster()

    // 使用集群的 PubSub 发送广播消息
    cluster.pubSub.publish(kind, message)
}
