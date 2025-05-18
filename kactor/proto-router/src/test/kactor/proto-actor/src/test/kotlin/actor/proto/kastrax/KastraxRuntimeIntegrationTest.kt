package actor.proto.kastrax

import actor.proto.ActorSystem
import actor.proto.Props
import actor.proto.actorOfWithKastraxRuntime
import actor.proto.requestAwaitWithKastraxRuntime
import actor.proto.useKastraxRuntime
import ai.kastrax.runtime.coroutines.jvm.JvmCoroutineRuntime
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class KastraxRuntimeIntegrationTest {
    
    @Test
    fun `test ActorSystem with kastrax runtime`() {
        // 创建kastrax运行时
        val runtime = JvmCoroutineRuntime()
        
        // 创建ActorSystem并使用kastrax运行时
        val system = ActorSystem("test-system").useKastraxRuntime(runtime)
        
        // 创建一个简单的Actor
        val props = Props.fromProducer { EchoActor() }
        val pid = system.actorOfWithKastraxRuntime(props, runtime)
        
        // 发送消息并等待响应
        runBlocking {
            val response = system.root.requestAwaitWithKastraxRuntime<String>(
                pid,
                "hello",
                Duration.ofSeconds(5),
                runtime
            )
            
            // 验证响应
            assertEquals("hello", response)
        }
    }
    
    @Test
    fun `test performance comparison`() {
        // 创建kastrax运行时
        val runtime = JvmCoroutineRuntime()
        
        // 创建使用kastrax运行时的ActorSystem
        val kastraxSystem = ActorSystem("kastrax-system").useKastraxRuntime(runtime)
        
        // 创建使用标准kotlinx.coroutines的ActorSystem
        val standardSystem = ActorSystem("standard-system")
        
        // 创建Actor
        val kastraxProps = Props.fromProducer { EchoActor() }
        val standardProps = Props.fromProducer { EchoActor() }
        
        val kastraxPid = kastraxSystem.actorOfWithKastraxRuntime(kastraxProps, runtime)
        val standardPid = standardSystem.actorOf(standardProps)
        
        // 性能测试
        val messageCount = 1000
        val futures = mutableListOf<CompletableFuture<Void>>()
        
        // 测试kastrax运行时
        val kastraxStartTime = System.nanoTime()
        for (i in 1..messageCount) {
            val future = CompletableFuture<Void>()
            futures.add(future)
            
            runBlocking {
                try {
                    kastraxSystem.root.requestAwaitWithKastraxRuntime<String>(
                        kastraxPid,
                        "message-$i",
                        Duration.ofSeconds(5),
                        runtime
                    )
                    future.complete(null)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        }
        
        // 等待所有消息处理完成
        CompletableFuture.allOf(*futures.toTypedArray()).get(30, TimeUnit.SECONDS)
        val kastraxDuration = System.nanoTime() - kastraxStartTime
        
        // 清空futures
        futures.clear()
        
        // 测试标准运行时
        val standardStartTime = System.nanoTime()
        for (i in 1..messageCount) {
            val future = CompletableFuture<Void>()
            futures.add(future)
            
            runBlocking {
                try {
                    standardSystem.root.requestAwait<String>(
                        standardPid,
                        "message-$i",
                        Duration.ofSeconds(5)
                    )
                    future.complete(null)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        }
        
        // 等待所有消息处理完成
        CompletableFuture.allOf(*futures.toTypedArray()).get(30, TimeUnit.SECONDS)
        val standardDuration = System.nanoTime() - standardStartTime
        
        // 输出性能比较结果
        println("Performance comparison:")
        println("kastrax runtime: ${kastraxDuration / 1_000_000} ms")
        println("standard runtime: ${standardDuration / 1_000_000} ms")
        println("difference: ${(standardDuration - kastraxDuration) / 1_000_000} ms")
        println("ratio: ${standardDuration.toDouble() / kastraxDuration.toDouble()}")
    }
    
    class EchoActor : actor.proto.Actor {
        override suspend fun actor.proto.Context.receive(msg: Any) {
            when (msg) {
                is String -> sender?.tell(msg)
                else -> unhandled(msg)
            }
        }
    }
}
