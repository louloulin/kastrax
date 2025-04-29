package actor.proto.examples.metrics

import actor.proto.*
import actor.proto.metrics.MetricsRegistry
import actor.proto.metrics.prometheus.PrometheusExporter
import actor.proto.metrics.prometheus.PrometheusMetricsRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration
import kotlin.random.Random

/**
 * 示例 Actor，用于演示度量系统
 */
class MetricsExampleActor : Actor {
    private var messageCount = 0
    
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> {
                println("MetricsExampleActor started")
            }
            is String -> {
                messageCount++
                println("Received message: $msg (total: $messageCount)")
                
                // 模拟处理时间
                val processingTime = Random.nextLong(10, 100)
                delay(processingTime)
                
                // 如果是请求，则回复
                if (sender != null) {
                    respond("Response to: $msg")
                }
                
                // 随机生成错误
                if (Random.nextInt(10) == 0) {
                    throw RuntimeException("Random error in processing message: $msg")
                }
            }
            is Stopping -> {
                println("MetricsExampleActor stopping")
            }
            is Stopped -> {
                println("MetricsExampleActor stopped")
            }
            else -> {
                println("Unknown message: ${msg.javaClass.name}")
            }
        }
    }
}

/**
 * 主函数
 */
fun main() = runBlocking {
    // 创建 Prometheus 度量注册表
    val metricsRegistry = PrometheusMetricsRegistry()
    
    // 创建 Prometheus 导出器并启动 HTTP 服务器
    val exporter = PrometheusExporter(metricsRegistry.getRegistry())
    exporter.start()
    
    // 创建 Actor 系统
    val system = ActorSystem("metrics-example")
    
    // 使用自定义度量注册表
    val originalMetrics = system.metrics
    // 这里我们假设 ActorSystem 有一个 setMetrics 方法，实际上可能需要通过反射或其他方式设置
    // system.setMetrics(metricsRegistry)
    
    // 创建 Actor
    val props = fromProducer { MetricsExampleActor() }
    val pid = system.actorOf(props, "metrics-example-actor")
    
    // 发送消息
    repeat(100) { i ->
        println("Sending message $i")
        system.send(pid, "Message $i")
        delay(100)
    }
    
    // 发送请求并等待响应
    repeat(10) { i ->
        println("Sending request $i")
        val response = system.requestAsync<String>(pid, "Request $i", Duration.ofSeconds(5))
        println("Received response: $response")
        delay(500)
    }
    
    // 停止 Actor
    system.stop(pid)
    
    // 等待一段时间，让度量被收集
    delay(1000)
    
    // 关闭 Prometheus 导出器
    exporter.close()
    
    println("Metrics example completed!")
}
