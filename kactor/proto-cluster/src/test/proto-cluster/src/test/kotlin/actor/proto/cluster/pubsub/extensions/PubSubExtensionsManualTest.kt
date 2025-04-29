package actor.proto.cluster.pubsub.extensions

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterConfig
import actor.proto.cluster.PubSub
import actor.proto.cluster.PubSubMessage
import actor.proto.cluster.SubscriptionRequest
import actor.proto.cluster.SubscriptionResponse
import actor.proto.cluster.UnsubscriptionRequest
import actor.proto.cluster.UnsubscriptionResponse
import actor.proto.cluster.providers.SimpleClusterProvider
import actor.proto.remote.RemoteConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*

/**
 * 这个测试类用于手动验证 PubSub Extensions 功能
 * 它不会自动运行，需要手动运行
 */
@Disabled("This test is for manual verification only")
class PubSubExtensionsManualTest {
    
    @Test
    fun `test PubSub Extensions`() = runBlocking {
        // 创建 Actor 系统
        val system = ActorSystem()
        
        // 创建集群配置
        val clusterProvider = mock<SimpleClusterProvider>()
        val remoteConfig = RemoteConfig("localhost", 0)
        val config = ClusterConfig(
            name = "test-cluster",
            clusterProvider = clusterProvider,
            identityLookup = mock(),
            remoteConfig = remoteConfig
        )
        
        // 创建集群
        val cluster = mock<Cluster>()
        whenever(cluster.actorSystem).thenReturn(system)
        whenever(cluster.config).thenReturn(config)
        
        // 创建 PubSub
        val pubSub = PubSub(cluster)
        
        // 创建并注册扩展
        val loggingExtension = LoggingExtension()
        val metricsExtension = MetricsExtension()
        val filterExtension = FilterExtension()
        
        // 配置过滤扩展
        filterExtension.addTopicFilter("test-topic") { message ->
            message is String && !message.toString().contains("blocked")
        }
        
        // 注册扩展
        pubSub.registerExtension(loggingExtension)
        pubSub.registerExtension(metricsExtension)
        pubSub.registerExtension(filterExtension)
        
        println("Registered PubSub extensions")
        
        // 创建订阅者
        val subscriber = mock<PID>()
        
        // 订阅主题
        val topic = "test-topic"
        val success = pubSub.subscribe(topic, subscriber)
        
        println("Subscribed to topic: $topic, success: $success")
        
        // 发布消息
        val result1 = pubSub.publish(topic, "Hello, world!")
        println("Published message: Hello, world!, success: $result1")
        
        // 发布被过滤的消息
        val result2 = pubSub.publish(topic, "This message is blocked")
        println("Attempted to publish blocked message, success: $result2")
        
        // 打印指标
        println("Metrics:")
        println("- Publish count: ${metricsExtension.getPublishCount()}")
        println("- Topic publish count: ${metricsExtension.getTopicPublishCount(topic)}")
        
        // 验证结果
        assertTrue(success)
        assertTrue(result1)
        assertFalse(result2)
        assertEquals(1, metricsExtension.getPublishCount())
        assertEquals(1, metricsExtension.getTopicPublishCount(topic))
        
        // 取消订阅
        val unsubscribeSuccess = pubSub.unsubscribe(topic, subscriber)
        println("Unsubscribed from topic: $topic, success: $unsubscribeSuccess")
        
        // 验证结果
        assertTrue(unsubscribeSuccess)
    }
}
