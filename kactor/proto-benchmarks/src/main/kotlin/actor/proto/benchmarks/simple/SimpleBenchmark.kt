package actor.proto.benchmarks.simple

import actor.proto.*
import actor.proto.mailbox.newUnboundedMailbox
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 简单的 Actor 基准测试，专为 Native 编译设计
 * 这个基准测试不依赖 JMH，可以直接编译为 native 镜像
 */
class CounterActor : Actor {
    private var counter = 0

    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is StartMessage -> {
                // 开始计数
                repeat(msg.count) {
                    counter++
                }
                // 完成后通知
                msg.latch.countDown()
            }
            else -> println("Unknown message: $msg")
        }
    }
}

data class StartMessage(val count: Int, val latch: CountDownLatch)

fun main() {
    println("ProtoActor 简单基准测试")
    println("====================")

    // 创建 Actor 系统
    val system = ActorSystem("benchmark-system")

    // 测试参数
    val iterations = 5
    val messageCount = 1_000_000
    val actorCount = 10

    // 运行基准测试
    val results = mutableListOf<Long>()

    repeat(iterations) { iteration ->
        println("运行迭代 ${iteration + 1}/$iterations...")

        // 创建 actors
        val actors = List(actorCount) {
            val props = fromProducer { CounterActor() }
                .withMailbox { newUnboundedMailbox() }
            system.actorOf(props)
        }

        // 准备同步锁
        val latch = CountDownLatch(actorCount)

        // 开始计时
        val startTime = System.nanoTime()

        // 发送消息
        runBlocking {
            actors.forEach { pid ->
                system.send(pid, StartMessage(messageCount / actorCount, latch))
            }

            // 等待所有 actor 完成
            latch.await(30, TimeUnit.SECONDS)
        }

        // 结束计时
        val endTime = System.nanoTime()
        val duration = endTime - startTime
        val durationMs = duration / 1_000_000.0

        println("迭代 ${iteration + 1} 完成: $durationMs ms")
        results.add(duration)

        // 停止所有 actor
        runBlocking {
            actors.forEach { pid ->
                system.stop(pid)
            }
            // 给一点时间让 actor 停止
            Thread.sleep(100)
        }
    }

    // 计算结果
    val avgDurationNs = results.average()
    val avgDurationMs = avgDurationNs / 1_000_000.0
    val messagesPerSec = (messageCount / (avgDurationNs / 1_000_000_000.0)).toLong()

    println("\n结果摘要:")
    println("====================")
    println("消息总数: $messageCount")
    println("Actor 数量: $actorCount")
    println("平均耗时: $avgDurationMs ms")
    println("每秒消息数: $messagesPerSec msg/sec")

    println("\n基准测试完成!")
}
