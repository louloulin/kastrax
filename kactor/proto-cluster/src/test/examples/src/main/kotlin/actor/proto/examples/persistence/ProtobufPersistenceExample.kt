package actor.proto.examples.persistence

import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.persistence.OfferSnapshot
import actor.proto.persistence.PersistentActor
import actor.proto.persistence.Replay
import actor.proto.persistence.RequestSnapshot
import actor.proto.persistence.providers.ProtobufProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.Serializable
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 这个示例展示了如何使用 ProtobufProvider 进行持久化
 */
fun main() = runBlocking {
    // 创建临时文件
    val tempFile = File.createTempFile("persistence", ".pb")
    tempFile.deleteOnExit()

    println("Using persistence file: ${tempFile.absolutePath}")

    // 创建 Actor 系统
    val system = ActorSystem()

    // 创建持久化提供程序
    val provider = ProtobufProvider(snapshotInterval = 5, filePath = tempFile.absolutePath)

    // 创建持久化 Actor
    val props = fromProducer { CounterActor() }
    val pid = system.actorOf(props)

    // 发送 Replay 消息，初始化 Actor
    system.send(pid, provider)

    // 等待 Actor 初始化
    delay(100)

    println("Actor initialized")

    // 发送增加计数的消息
    for (i in 1..10) {
        system.send(pid, Increment())
        println("Sent increment message $i")
        delay(100)
    }

    // 请求快照
    system.send(pid, RequestSnapshot)
    println("Requested snapshot")

    // 等待快照完成
    delay(100)

    // 停止 Actor
    system.stop(pid)
    println("Stopped first actor")

    // 创建新的 Actor 系统
    val newSystem = ActorSystem()
    println("Created new actor system")

    // 创建新的持久化 Actor
    val newProps = fromProducer { CounterActor() }
    val newPid = newSystem.actorOf(newProps)

    // 发送 Replay 消息，初始化 Actor
    newSystem.send(newPid, provider)

    // 等待 Actor 初始化
    delay(100)

    println("New actor initialized")

    // 发送获取计数的消息
    val response = newSystem.requestAwait<CounterValue>(newPid, GetCounter())

    // 打印计数
    println("Counter value: ${response.value}")
    println("Persistence example completed successfully!")

    // 停止 Actor
    newSystem.stop(newPid)
}

/**
 * 增加计数的消息
 */
class Increment : Serializable

/**
 * 获取计数的消息
 */
class GetCounter : Serializable

/**
 * 计数值的响应
 */
data class CounterValue(val value: Int) : Serializable

/**
 * 计数器 Actor
 */
class CounterActor : PersistentActor() {
    private var counter = 0

    override suspend fun receiveRecover(context: Context, message: Any) {
        when (message) {
            is Increment -> {
                counter++
                println("Recovered: counter = $counter")
            }
            is OfferSnapshot -> {
                val snapshot = message.snapshot as? CounterValue ?: return
                counter = snapshot.value
                println("Recovered from snapshot: counter = $counter")
            }
        }
    }

    override suspend fun receiveCommand(context: Context, message: Any) {
        when (message) {
            is Increment -> {
                counter++
                persistReceive(message)
                logger.info { "Incremented: counter = $counter" }
            }
            is GetCounter -> {
                context.respond(CounterValue(counter))
            }
            is RequestSnapshot -> {
                persistSnapshot(CounterValue(counter))
                logger.info { "Snapshot taken: counter = $counter" }
            }
        }
    }
}
