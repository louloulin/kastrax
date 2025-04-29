package cluster

import actor.proto.ActorSystem
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterConfig
import actor.proto.cluster.MemberStatus
import actor.proto.cluster.WithInformer
import actor.proto.cluster.informer.DefaultInformer
import actor.proto.cluster.informer.MemberJoinedEvent
import actor.proto.cluster.informer.MemberLeftEvent
import actor.proto.cluster.informer.MemberStatusChangedEvent
import actor.proto.cluster.providers.AutoManagedClusterProvider
import actor.proto.cluster.providers.DistributedHashIdentityLookup
import actor.proto.remote.RemoteConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration

/**
 * 这个示例展示了如何使用 Informer 监控集群成员状态
 */
fun main() = runBlocking {
    // 创建 Actor 系统
    val system = ActorSystem()
    
    // 创建集群提供者
    val clusterProvider = AutoManagedClusterProvider(
        clusterName = "example-cluster",
        host = "localhost",
        port = 12000
    )
    
    // 创建身份查找
    val identityLookup = DistributedHashIdentityLookup()
    
    // 创建远程配置
    val remoteConfig = RemoteConfig("localhost", 12000)
    
    // 创建 Informer
    val informer = DefaultInformer()
    
    // 创建集群配置
    val config = ClusterConfig.create(
        name = "example-cluster",
        clusterProvider = clusterProvider,
        identityLookup = identityLookup,
        remoteConfig = remoteConfig,
        WithInformer(informer)
    )
    
    // 创建集群
    val cluster = Cluster.create(system, config)
    
    // 订阅成员事件
    system.eventStream().subscribe<MemberJoinedEvent> { event ->
        println("Member joined: ${event.member.id} (${event.member.host}:${event.member.port})")
    }
    
    system.eventStream().subscribe<MemberLeftEvent> { event ->
        println("Member left: ${event.memberId}")
    }
    
    system.eventStream().subscribe<MemberStatusChangedEvent> { event ->
        println("Member status changed: ${event.memberId} ${event.oldStatus} -> ${event.newStatus}")
    }
    
    // 启动集群
    cluster.startMember()
    println("Cluster started")
    
    // 等待一段时间，让集群稳定
    delay(5000)
    
    // 获取成员列表
    val members = informer.getMembers()
    println("Cluster members:")
    members.forEach { member ->
        println("- ${member.id} (${member.host}:${member.port}) [${member.status}]")
    }
    
    // 等待用户输入，然后关闭集群
    println("Press enter to shutdown...")
    readLine()
    
    // 关闭集群
    cluster.shutdown(true)
    println("Cluster shutdown")
}
