package cluster

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
import actor.proto.cluster.pubsub.BatchingProducerConfig
import actor.proto.cluster.pubsub.PubSubBatch
import actor.proto.remote.RemoteConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 这个示例展示了如何使用 PubSub Batch 功能
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

    // 订阅主题
    val topic = "example-topic"
    val subscriptionResponse = system.root.requestAwait<SubscriptionResponse>(
        cluster.pubSub.pid,
        SubscriptionRequest(topic, subscriber)
    )

    if (subscriptionResponse.success) {
        println("Subscribed to topic: $topic")

        // 创建批处理生产者
        val producerConfig = BatchingProducerConfig(
            maxBatchSize = 10,
            flushInterval = Duration.ofMillis(100)
        )

        val producer = cluster.pubSub.batchingProducer(topic, producerConfig)

        try {
            // 发布单个消息
            val response1 = producer.produce("Hello from batch producer!")
            println("Publish response: ${response1.success}")

            // 发布多个消息
            val messages = listOf(
                "Message 1",
                "Message 2",
                "Message 3",
                "Message 4",
                "Message 5"
            )

            val responses = producer.produceAll(messages)
            println("Published ${responses.count { it.success }} of ${messages.size} messages successfully")

            // 等待消息处理
            delay(1000)

            // 手动刷新队列
            producer.flush()
            println("Flushed producer")

            // 等待消息处理
            delay(1000)
        } finally {
            // 关闭生产者
            producer.close()
            println("Producer closed")

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
        }
    } else {
        println("Failed to subscribe to topic: $topic")
    }

    // 关闭集群
    cluster.shutdown(true)
    println("Cluster shutdown")
}
