package actor.proto.kastrax

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.Props
import actor.proto.requestAwait
import actor.proto.useKastraxRuntime
import actor.proto.withProducer
import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory
import ai.kastrax.runtime.coroutines.jvm.JvmCoroutineRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * 测试kastrax协程运行时与kactor的集成
 */
class KastraxRuntimeIntegrationTest {

    /**
     * 简单的回显Actor
     */
    class EchoActor : Actor {
        override suspend fun Context.receive(msg: Any) {
            when (msg) {
                is String -> sender?.let { send(it, msg) }
                is Int -> sender?.let { send(it, msg * 2) }
                else -> sender?.let { send(it, "unknown") }
            }
        }
    }

    /**
     * 计数器Actor
     */
    class CounterActor(private val counter: AtomicInteger) : Actor {
        override suspend fun Context.receive(msg: Any) {
            when (msg) {
                is String -> {
                    if (msg == "increment") {
                        counter.incrementAndGet()
                        sender?.let { send(it, counter.get()) }
                    } else {
                        sender?.let { send(it, counter.get()) }
                    }
                }
                else -> sender?.let { send(it, counter.get()) }
            }
        }
    }

    @Test
    fun `test ActorSystem with kastrax runtime`() {
        // 创建kastrax运行时
        val runtime = JvmCoroutineRuntime()

        // 设置全局运行时
        KastraxCoroutineRuntimeFactory.setRuntime(runtime)

        // 创建ActorSystem并使用kastrax运行时
        val system = ActorSystem("test-system").useKastraxRuntime(runtime)

        // 创建一个简单的Actor
        val props = Props().withProducer { EchoActor() }
        val pid = system.actorOf(props)

        // 发送消息并等待响应
        val response = KastraxCoroutineGlobal.runBlocking {
            system.root.requestAwait<String>(
                pid,
                "hello",
                Duration.ofSeconds(5)
            )
        }

        // 验证响应
        assertEquals("hello", response)
    }

    @Test
    fun `test counter actor with kastrax runtime`() {
        // 创建kastrax运行时
        val runtime = JvmCoroutineRuntime()

        // 设置全局运行时
        KastraxCoroutineRuntimeFactory.setRuntime(runtime)

        // 创建ActorSystem并使用kastrax运行时
        val system = ActorSystem("counter-system").useKastraxRuntime(runtime)

        // 创建计数器
        val counter = AtomicInteger(0)

        // 创建计数器Actor
        val props = Props().withProducer { CounterActor(counter) }
        val pid = system.actorOf(props)

        // 发送多个增量消息
        val incrementCount = 10
        for (i in 1..incrementCount) {
            KastraxCoroutineGlobal.runBlocking {
                system.root.requestAwait<Int>(
                    pid,
                    "increment",
                    Duration.ofSeconds(1)
                )
            }
        }

        // 获取最终计数
        val finalCount = KastraxCoroutineGlobal.runBlocking {
            system.root.requestAwait<Int>(
                pid,
                "get",
                Duration.ofSeconds(1)
            )
        }

        // 验证计数
        assertEquals(incrementCount, finalCount)
    }
}
