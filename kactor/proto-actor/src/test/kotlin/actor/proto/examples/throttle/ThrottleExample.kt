package actor.proto.examples.throttle

import actor.proto.*
import actor.proto.throttle.throttleReceiveMiddleware
import actor.proto.throttle.throttleSenderMiddleware
import actor.proto.throttle.throttleMessageTypeMiddleware
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 这个示例展示了如何使用节流中间件来限制消息处理速率
 */
fun main() {
    println("=== Throttle Middleware Example ===")

    // 创建一个Actor系统
    val system = ActorSystem.default()

    // 创建一个带有接收节流中间件的Actor
    val receiverProps = fromFunc { msg ->
        when (msg) {
            is String -> println("Received message: $msg")
        }
    }.withReceiveMiddleware(
        throttleReceiveMiddleware(
            maxEventsInPeriod = 5,
            period = Duration.ofSeconds(1),
            onThrottled = { count -> println("Throttled $count messages in the last second") }
        )
    )

    val receiverPid = system.root.spawn(receiverProps)
    println("Sending 10 messages to throttled receiver...")

    // 发送10条消息，但只有5条会被处理
    repeat(10) { i ->
        system.root.send(receiverPid, "Message $i")
    }

    // 等待一秒，让节流器重置
    Thread.sleep(1100)
    println("\nAfter waiting, sending 3 more messages...")

    // 发送3条消息，这些应该都会被处理
    repeat(3) { i ->
        system.root.send(receiverPid, "Additional message $i")
    }

    // 创建一个带有发送节流中间件的Actor
    val senderProps = fromFunc { msg ->
        when (msg) {
            is String -> {
                val target = msg.split(":")[0]
                val message = msg.split(":")[1]
                val targetPid = PID(system.address, target)
                println("Sending to $target: $message")
                send(targetPid, message)
            }
        }
    }.withSenderMiddleware(
        throttleSenderMiddleware(
            maxEventsInPeriod = 3,
            period = Duration.ofSeconds(1),
            onThrottled = { count -> println("Throttled $count send operations in the last second") }
        )
    )

    val senderPid = system.root.spawn(senderProps)
    println("\n=== Sender Throttling ===")
    println("Sending 5 messages through throttled sender...")

    // 发送5条消息，但只有3条会被发送
    repeat(5) { i ->
        system.root.send(senderPid, "receiver:Message from sender $i")
    }

    // 等待所有消息处理完成
    Thread.sleep(2000)
    println("\nExample completed. Press Enter to exit.")
    readLine()

    // 停止所有Actor
    system.stop(receiverPid)
    system.stop(senderPid)
}
