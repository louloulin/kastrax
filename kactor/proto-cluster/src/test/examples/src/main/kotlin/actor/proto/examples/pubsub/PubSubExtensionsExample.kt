package actor.proto.examples.pubsub

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterConfig
import actor.proto.cluster.PubSubMessage
import actor.proto.cluster.SubscriptionRequest
import actor.proto.cluster.SubscriptionResponse
import actor.proto.cluster.UnsubscriptionRequest
import actor.proto.cluster.UnsubscriptionResponse
import actor.proto.cluster.providers.AutoManagedClusterProvider
import actor.proto.cluster.providers.DistributedHashIdentityLookup
import actor.proto.cluster.pubsub.extensions.FilterExtension
import actor.proto.cluster.pubsub.extensions.LoggingExtension
import actor.proto.cluster.pubsub.extensions.MetricsExtension
import actor.proto.cluster.pubsub.extensions.SecurityExtension
import actor.proto.remote.RemoteConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 这个示例展示了如何使用 PubSub Extensions
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
        remoteConfig = remoteConfig
    )
    
    // 创建集群
    val cluster = Cluster.create(system, config)
    
    // 启动集群
    cluster.startMember()
    println("Cluster started")
    
    // 创建订阅者
    val subscriberProps = actor.proto.fromProducer {
        object : actor.proto.Actor {
            override suspend fun actor.proto.Context.receive(msg: Any) {
                when (msg) {
                    is String -> println("Subscriber received: $msg")
                }
            }
        }
    }
    
    val subscriber = system.actorOf(subscriberProps)
    
    // 创建并注册扩展
    val loggingExtension = LoggingExtension()
    val metricsExtension = MetricsExtension()
    val filterExtension = FilterExtension()
    val securityExtension = SecurityExtension()
    
    // 配置过滤扩展
    filterExtension.addTopicFilter("example-topic") { message ->
        message is String && !message.toString().contains("blocked")
    }
    
    // 配置安全扩展
    securityExtension.addSubscriberRole("secure-topic", "admin")
    securityExtension.addPidRole(subscriber, "admin")
    
    // 注册扩展
    cluster.pubSub.registerExtension(loggingExtension)
    cluster.pubSub.registerExtension(metricsExtension)
    cluster.pubSub.registerExtension(filterExtension)
    cluster.pubSub.registerExtension(securityExtension)
    
    println("Registered PubSub extensions")
    
    // 订阅主题
    val topic = "example-topic"
    val subscriptionResponse = system.root.requestAwait<SubscriptionResponse>(
        cluster.pubSub.pid,
        SubscriptionRequest(topic, subscriber)
    )
    
    if (subscriptionResponse.success) {
        println("Subscribed to topic: $topic")
        
        // 发布消息
        system.send(cluster.pubSub.pid, PubSubMessage(topic, "Hello, world!"))
        println("Published message: Hello, world!")
        
        // 发布被过滤的消息
        system.send(cluster.pubSub.pid, PubSubMessage(topic, "This message is blocked"))
        println("Attempted to publish blocked message")
        
        // 等待消息处理
        delay(1000)
        
        // 打印指标
        println("Metrics:")
        println("- Publish count: ${metricsExtension.getPublishCount()}")
        println("- Topic publish count: ${metricsExtension.getTopicPublishCount(topic)}")
        
        // 尝试订阅安全主题
        val secureTopic = "secure-topic"
        val secureSubscriptionResponse = system.root.requestAwait<SubscriptionResponse>(
            cluster.pubSub.pid,
            SubscriptionRequest(secureTopic, subscriber)
        )
        
        if (secureSubscriptionResponse.success) {
            println("Subscribed to secure topic: $secureTopic")
            
            // 发布消息到安全主题
            system.send(cluster.pubSub.pid, PubSubMessage(secureTopic, "Secure message"))
            println("Published message to secure topic")
            
            // 等待消息处理
            delay(1000)
            
            // 取消订阅安全主题
            val unsubscriptionResponse = system.root.requestAwait<UnsubscriptionResponse>(
                cluster.pubSub.pid,
                UnsubscriptionRequest(secureTopic, subscriber)
            )
            
            if (unsubscriptionResponse.success) {
                println("Unsubscribed from secure topic: $secureTopic")
            } else {
                println("Failed to unsubscribe from secure topic: $secureTopic")
            }
        } else {
            println("Failed to subscribe to secure topic: $secureTopic")
        }
        
        // 取消订阅
        val unsubscriptionResponse = system.root.requestAwait<UnsubscriptionResponse>(
            cluster.pubSub.pid,
            UnsubscriptionRequest(topic, subscriber)
        )
        
        if (unsubscriptionResponse.success) {
            println("Unsubscribed from topic: $topic")
        } else {
            println("Failed to unsubscribe from topic: $topic")
        }
    } else {
        println("Failed to subscribe to topic: $topic")
    }
    
    // 关闭集群
    cluster.shutdown(true)
    println("Cluster shutdown")
}
