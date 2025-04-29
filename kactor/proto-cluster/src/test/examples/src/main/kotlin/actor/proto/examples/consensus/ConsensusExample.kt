package actor.proto.examples.consensus

import actor.proto.ActorSystem
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterConfig
import actor.proto.cluster.MemberStatus
import actor.proto.cluster.consensus.ConsensusCheckBuilder
import actor.proto.cluster.informer.DefaultInformer
import actor.proto.cluster.providers.AutoManagedClusterProvider
import actor.proto.cluster.providers.DistributedHashIdentityLookup
import actor.proto.remote.RemoteConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 这个示例展示了如何使用 Consensus 机制
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
    
    // 创建集群配置
    val config = ClusterConfig(
        name = "example-cluster",
        clusterProvider = clusterProvider,
        identityLookup = identityLookup,
        remoteConfig = remoteConfig,
        informer = DefaultInformer()
    )
    
    // 创建集群
    val cluster = Cluster.create(system, config)
    
    // 启动集群
    cluster.startMember()
    println("Cluster started")
    
    // 创建共识检查
    val statusBuilder = ConsensusCheckBuilder.create()
        .withMemberStatus(MemberStatus.ALIVE, minCount = 1)
    
    val countBuilder = ConsensusCheckBuilder.create()
        .withMemberCount(minCount = 1, maxCount = 10)
    
    // 注册共识检查
    val statusConsensus = cluster.registerConsensusCheck("status-consensus", statusBuilder)
    val countConsensus = cluster.registerConsensusCheck("count-consensus", countBuilder)
    
    println("Registered consensus checks")
    
    // 等待共识
    val statusResult = statusConsensus.waitForConsensus(5000)
    if (statusResult != null) {
        println("Status consensus reached: $statusResult")
    } else {
        println("Status consensus not reached within timeout")
    }
    
    val countResult = countConsensus.waitForConsensus(5000)
    if (countResult != null) {
        println("Count consensus reached: $countResult")
    } else {
        println("Count consensus not reached within timeout")
    }
    
    // 创建自定义共识检查
    val customBuilder = ConsensusCheckBuilder.create()
        .withCustomCheck { members ->
            if (members.size >= 1) {
                println("Custom check: ${members.size} members found")
                members.size
            } else {
                null
            }
        }
    
    val customConsensus = cluster.registerConsensusCheck("custom-consensus", customBuilder)
    
    // 等待自定义共识
    val customResult = customConsensus.waitForConsensus(5000)
    if (customResult != null) {
        println("Custom consensus reached: $customResult")
    } else {
        println("Custom consensus not reached within timeout")
    }
    
    // 等待一段时间，让共识管理器运行
    delay(5000)
    
    // 关闭集群
    cluster.shutdown(true)
    println("Cluster shutdown")
}
