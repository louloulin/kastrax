package ai.kastrax.actor.cluster

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.cluster.Cluster
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.ActorAgentBuilder
import ai.kastrax.actor.actorAgent
import ai.kastrax.core.agent.Agent

/**
 * 配置集群 Actor 系统
 *
 * @param name 系统名称
 * @param config 集群配置
 * @return 配置好的 ActorSystem
 */
fun configureCluster(name: String, config: ClusterConfig = ClusterConfig()): ActorSystem {
    // 创建 kactor 集群配置
    val kactorConfig = config.toKactorClusterConfig()

    // 创建 ActorSystem
    val system = ActorSystem(name)

    // 初始化集群
    Cluster.get(system, kactorConfig)

    return system
}

/**
 * 加入集群
 */
fun ActorSystem.joinCluster() {
    // 获取集群实例并加入
    val cluster = Cluster.get(this)
    cluster.join()
}

/**
 * 离开集群
 */
fun ActorSystem.leaveCluster() {
    // 获取集群实例并离开
    val cluster = Cluster.get(this)
    cluster.leave()
}

/**
 * 获取集群成员列表
 */
fun ActorSystem.getClusterMembers(): List<String> {
    // 获取集群实例并返回成员列表
    val cluster = Cluster.get(this)
    return cluster.memberList.map { it.address }
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
    val cluster = Cluster.get(this)
    
    // 使用 actorAgent DSL 创建 Actor
    val pid = this.actorAgent {
        // 设置 Agent
        agentBuilder.name = id ?: agent.name
        
        actor {
            // 集群 Actor 特有的配置
            oneForOneStrategy {
                maxRetries = 5
                withinTimeRange = java.time.Duration.ofMinutes(1)
            }
            // 使用无界邮箱，适合分布式通信
            unboundedMailbox()
        }
    }
    
    // 注册到集群
    cluster.registerMember(kind, pid)
    
    return pid
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
    val cluster = Cluster.get(this)
    
    // 获取指定类型的 PID
    return if (id != null) {
        cluster.get(kind, id)
    } else {
        // 如果没有指定 ID，则获取该类型的任意一个 PID
        val members = cluster.getMembers(kind)
        if (members.isNotEmpty()) members[0] else null
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
    val cluster = Cluster.get(this)
    
    // 获取所有指定类型的 PID
    val members = cluster.getMembers(kind)
    
    // 向所有成员发送消息
    members.forEach { pid ->
        this.root.send(pid, message)
    }
}
