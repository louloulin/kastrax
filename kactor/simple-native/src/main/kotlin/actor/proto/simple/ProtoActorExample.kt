package actor.proto.simple

import actor.proto.*
import kotlinx.coroutines.runBlocking
import java.time.Duration

/**
 * 一个简单的 ProtoActor 示例，用于演示 Native Image 编译
 */

// 定义消息类型
data class Greeting(val who: String)
data class Response(val message: String)

// 定义一个简单的 Actor
class GreetingActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> println("GreetingActor started")
            is Greeting -> {
                println("Hello ${msg.who}!")
                respond(Response("Greeting processed for ${msg.who}"))
            }
            is Stopping -> println("GreetingActor stopping")
            is Stopped -> println("GreetingActor stopped")
            else -> println("Unknown message: ${msg.javaClass.name}")
        }
    }
}

// 定义一个响应处理 Actor
class ResponseHandlerActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> println("ResponseHandlerActor started")
            is Response -> println("Got response: ${msg.message}")
            is Stopping -> println("ResponseHandlerActor stopping")
            is Stopped -> println("ResponseHandlerActor stopped")
            else -> println("Unknown message: ${msg.javaClass.name}")
        }
    }
}

fun main() {
    println("ProtoActor Native Example")
    println("=========================")

    // 创建 Actor 系统
    val system = ActorSystem("proto-native-system")

    runBlocking {
        // 创建响应处理 Actor
        val responseHandlerProps = fromProducer { ResponseHandlerActor() }
        val responseHandler = system.actorOf(responseHandlerProps)

        // 创建问候 Actor
        val greetingProps = fromProducer { GreetingActor() }
        val greeter = system.actorOf(greetingProps)

        // 发送消息
        system.send(greeter, Greeting("ProtoActor"))

        // 等待消息处理完成
        Thread.sleep(100)

        // 使用简单的消息发送
        system.send(greeter, Greeting("Native"))

        // 等待消息处理完成
        Thread.sleep(100)

        // 停止 Actors
        system.stop(greeter)
        system.stop(responseHandler)

        // 等待 Actors 停止
        Thread.sleep(100)
    }

    println("Example completed!")
}
