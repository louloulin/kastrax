package actor.proto.examples.prioritymailbox

import actor.proto.*
import actor.proto.mailbox.priority.*
import actor.proto.priority.withPriorityMailbox
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 消息类
 */
data class Message(val text: String, val timestamp: LocalDateTime = LocalDateTime.now())

/**
 * 优先级消息处理 Actor
 */
class PriorityActor : Actor {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> {
                println("PriorityActor started")
            }
            is Message -> {
                val priority = when (msg) {
                    is MessagePriority -> (msg as MessagePriority).getPriority()
                    else -> "none"
                }
                
                val formattedTime = msg.timestamp.format(formatter)
                println("Received message at ${LocalDateTime.now().format(formatter)} (sent at $formattedTime): '${msg.text}' with priority $priority")
                
                // 模拟处理时间
                delay(100)
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
    // 创建 Actor 系统
    val system = ActorSystem("priority-mailbox-example")
    
    // 创建带优先级邮箱的 Actor
    val props = fromProducer { PriorityActor() }.withPriorityMailbox()
    val pid = system.actorOf(props)
    
    println("Sending messages with different priorities...")
    
    // 发送低优先级消息
    for (i in 1..5) {
        system.sendWithLowPriority(pid, Message("Low priority message $i"))
    }
    
    // 发送普通消息（没有优先级）
    for (i in 1..5) {
        system.send(pid, Message("Regular message $i"))
    }
    
    // 发送中优先级消息
    for (i in 1..5) {
        system.sendWithMediumPriority(pid, Message("Medium priority message $i"))
    }
    
    // 发送高优先级消息
    for (i in 1..5) {
        system.sendWithHighPriority(pid, Message("High priority message $i"))
    }
    
    // 等待所有消息处理完成
    delay(5000)
    
    // 停止 Actor
    system.stop(pid)
    
    println("Example completed!")
}
