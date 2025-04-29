package actor.proto.plugin.examples

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.plugin.ProtoPluginManager
import actor.proto.plugin.examples.cache.Cacheable
import actor.proto.plugin.examples.monitoring.MonitoringData
import org.pf4j.DefaultPluginManager
import org.pf4j.PluginDependency
import org.pf4j.PluginDescriptor
import org.pf4j.PluginWrapper
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 可缓存的查询消息
 */
@Cacheable(ttl = 60)
data class QueryMessage(val query: String)

/**
 * 查询Actor
 * 处理查询请求
 */
class QueryActor : Actor {
    private var queryCount = 0

    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is QueryMessage -> {
                // 模拟查询处理
                queryCount++
                println("Processing query: ${msg.query} (count: $queryCount)")
                Thread.sleep(100)  // 模拟耗时操作
                respond("Result for ${msg.query}")
            }
            is String -> {
                when {
                    msg.startsWith("query:") -> {
                        // 模拟查询处理
                        queryCount++
                        val query = msg.substring(6)
                        println("Processing query: $query (count: $queryCount)")
                        Thread.sleep(100)  // 模拟耗时操作
                        respond("Result for $query")
                    }
                    else -> {
                        println("Received message: $msg")
                        respond("Echo: $msg")
                    }
                }
            }
        }
    }
}

/**
 * 示例应用
 * 展示如何使用插件
 */
suspend fun main() {
    println("=== ProtoActor Plugin Example ===")

    // 创建Actor系统
    val system = ActorSystem("example")

    // 创建插件管理器
    val pluginManager = ProtoPluginManager.getInstance(Paths.get("plugins"))

    // 手动注册插件
    val pf4jManager = DefaultPluginManager()
    // Create a plugin descriptor
    val descriptor = object : PluginDescriptor {
        override fun getPluginId(): String = "example-plugin"
        override fun getPluginDescription(): String = "Example Plugin"
        override fun getPluginClass(): String = ExamplePlugin::class.java.name
        override fun getVersion(): String = "1.0.0"
        override fun getProvider(): String = "Example Provider"
        override fun getDependencies(): List<PluginDependency> = emptyList()
        override fun getRequires(): String = "*"
        override fun getLicense(): String = "Apache License 2.0"
    }

    val plugin = ExamplePlugin(PluginWrapper(pf4jManager, descriptor, null, Thread.currentThread().contextClassLoader))

    // 获取PF4J插件管理器
    val field = ProtoPluginManager::class.java.getDeclaredField("pluginManager")
    field.isAccessible = true
    field.set(pluginManager, pf4jManager)

    // 添加插件到插件管理器
    val pluginsField = DefaultPluginManager::class.java.getDeclaredField("plugins")
    pluginsField.isAccessible = true
    val plugins = pluginsField.get(pf4jManager) as MutableMap<String, PluginWrapper>
    plugins["example-plugin"] = plugin.wrapper

    // 初始化插件
    pluginManager.initPlugins(system)

    println("Plugin initialized: ${plugin.id()}")

    // 创建查询Actor
    val props = fromProducer { QueryActor() }
    val pid = system.actorOf(props)

    // 发送查询消息
    println("\n=== Testing Caching ===")
    println("Sending the same query multiple times...")

    repeat(3) {
        val response = system.requestAsync<String>(pid, "query:users", Duration.ofSeconds(5))
        println("Response: $response")
    }

    // 发送不同的查询
    println("\nSending different queries...")

    val response1 = system.requestAsync<String>(pid, "query:products", Duration.ofSeconds(5))
    println("Response: $response1")

    val response2 = system.requestAsync<String>(pid, "query:orders", Duration.ofSeconds(5))
    println("Response: $response2")

    // 测试限流
    println("\n=== Testing Rate Limiting ===")
    println("Sending many messages quickly...")

    val latch = CountDownLatch(1)
    var successCount = 0

    val messageHandler = fromProducer {
        object : Actor {
            override suspend fun Context.receive(msg: Any) {
                when (msg) {
                    is String -> {
                        if (msg == "done") {
                            respond(successCount)
                            latch.countDown()
                        } else {
                            successCount++
                        }
                    }
                }
            }
        }
    }

    val handlerPid = system.actorOf(messageHandler)

    // 快速发送大量消息
    repeat(200) {
        system.send(handlerPid, "Message $it")
    }

    // 发送完成信号
    system.send(handlerPid, "done")

    // 等待处理完成
    latch.await(2, TimeUnit.SECONDS)

    // 获取成功处理的消息数
    val finalCount = system.requestAsync<Int>(handlerPid, "done", Duration.ofSeconds(5))
    println("Successfully processed $finalCount messages")

    // 显示监控数据
    println("\n=== Monitoring Data ===")
    MonitoringData.getAllMetrics().forEach { (key, value) ->
        println("$key: $value")
    }

    // 关闭Actor系统
    println("\nShutting down...")
    system.shutdown()

    println("Example completed")
}
