package actor.proto.examples.throttle

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.Props
import actor.proto.throttle.CountBasedThrottleStrategy
import actor.proto.throttle.LoggingCountBasedThrottleStrategy
import actor.proto.throttle.throttle
import actor.proto.throttle.throttleMessageTypeMiddleware
import actor.proto.throttle.throttleReceiveMiddleware
import actor.proto.withProducer
import actor.proto.withReceiveMiddleware
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * 节流器示例
 */
object ThrottleExample {
    
    /**
     * 消息类型
     */
    data class Message(val id: Int, val content: String)
    
    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        println("Throttle Example")
        println("================")
        
        // 创建一个Actor系统
        val system = ActorSystem.get("throttle-example")
        
        // 示例1：使用节流策略处理消息
        println("\nExample 1: Using throttle strategy")
        println("----------------------------------")
        
        // 创建一个计数器，用于记录处理的消息数
        val processedCounter = AtomicInteger(0)
        
        // 创建一个基于计数的节流策略，在1秒内最多允许3个事件
        val strategy = CountBasedThrottleStrategy(3, Duration.ofSeconds(1)) { throttledCount ->
            println("Throttled $throttledCount messages")
        }
        
        // 创建一个消息处理函数
        val handler = throttle<Message>(strategy) { message ->
            println("Processing message: $message")
            processedCounter.incrementAndGet()
        }
        
        // 处理10个消息
        println("Sending 10 messages...")
        for (i in 1..10) {
            handler(Message(i, "Content $i"))
            delay(100) // 短暂延迟，以便于观察
        }
        
        println("Processed ${processedCounter.get()} messages")
        
        // 等待1.5秒，让节流器重置
        println("\nWaiting for throttle to reset...")
        delay(1500)
        
        // 处理5个消息
        println("Sending 5 more messages...")
        for (i in 11..15) {
            handler(Message(i, "Content $i"))
            delay(100) // 短暂延迟，以便于观察
        }
        
        println("Total processed ${processedCounter.get()} messages")
        
        // 示例2：使用节流中间件
        println("\nExample 2: Using throttle middleware")
        println("-----------------------------------")
        
        // 创建一个带有节流中间件的Actor
        val props = Props().withProducer {
            object : Actor {
                private val counter = AtomicInteger(0)
                
                override suspend fun Context.receive(msg: Any) {
                    when (msg) {
                        is Message -> {
                            println("Actor processing message: $msg")
                            counter.incrementAndGet()
                        }
                        is String -> {
                            if (msg == "get-count") {
                                respond(counter.get())
                            }
                        }
                    }
                }
            }
        }.withReceiveMiddleware(
            throttleReceiveMiddleware(3, Duration.ofSeconds(1)) { throttledCount ->
                println("Actor throttled $throttledCount messages")
            }
        )
        
        // 创建Actor
        val pid = system.actorOf(props)
        
        // 发送10个消息
        println("Sending 10 messages to actor...")
        for (i in 1..10) {
            system.send(pid, Message(i, "Content $i"))
            delay(100) // 短暂延迟，以便于观察
        }
        
        // 获取处理的消息数
        val count = system.requestAwait<Int>(pid, "get-count")
        println("Actor processed $count messages")
        
        // 等待1.5秒，让节流器重置
        println("\nWaiting for throttle to reset...")
        delay(1500)
        
        // 发送5个消息
        println("Sending 5 more messages to actor...")
        for (i in 11..15) {
            system.send(pid, Message(i, "Content $i"))
            delay(100) // 短暂延迟，以便于观察
        }
        
        // 获取处理的消息数
        val totalCount = system.requestAwait<Int>(pid, "get-count")
        println("Actor total processed $totalCount messages")
        
        // 示例3：使用特定类型的节流中间件
        println("\nExample 3: Using type-specific throttle middleware")
        println("------------------------------------------------")
        
        // 创建一个带有特定类型节流中间件的Actor
        val typeSpecificProps = Props().withProducer {
            object : Actor {
                private val messageCounter = AtomicInteger(0)
                private val stringCounter = AtomicInteger(0)
                
                override suspend fun Context.receive(msg: Any) {
                    when (msg) {
                        is Message -> {
                            println("Actor processing message: $msg")
                            messageCounter.incrementAndGet()
                        }
                        is String -> {
                            if (msg == "get-counts") {
                                respond(Pair(messageCounter.get(), stringCounter.get()))
                            } else {
                                println("Actor processing string: $msg")
                                stringCounter.incrementAndGet()
                            }
                        }
                    }
                }
            }
        }.withReceiveMiddleware(
            // 只对Message类型的消息进行节流
            throttleMessageTypeMiddleware<Message>(2, Duration.ofSeconds(1)) { throttledCount ->
                println("Actor throttled $throttledCount messages of type Message")
            }
        )
        
        // 创建Actor
        val typedPid = system.actorOf(typeSpecificProps)
        
        // 交替发送Message和String类型的消息
        println("Sending alternating Message and String messages...")
        for (i in 1..6) {
            system.send(typedPid, Message(i, "Content $i"))
            system.send(typedPid, "String message $i")
            delay(100) // 短暂延迟，以便于观察
        }
        
        // 获取处理的消息数
        val counts = system.requestAwait<Pair<Int, Int>>(typedPid, "get-counts")
        println("Actor processed ${counts.first} messages and ${counts.second} strings")
        
        println("\nExample completed.")
    }
}
