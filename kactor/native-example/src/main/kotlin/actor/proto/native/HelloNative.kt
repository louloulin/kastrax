package actor.proto.native

import actor.proto.*
import kotlinx.coroutines.runBlocking

class HelloActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> println("Actor started")
            is String -> {
                println("Hello, $msg!")
                self.let { stop(it) }
            }
            is Stopping -> println("Actor stopping")
            is Stopped -> println("Actor stopped")
            else -> println("Unknown message: ${msg.javaClass.name}")
        }
    }
}

fun main() {
    // 创建一个 actor 系统
    val system = ActorSystem("hello-native-system")

    // 创建 actor 的配置
    val props = fromProducer { HelloActor() }

    // 创建 actor
    val pid = system.actorOf(props)

    // 向 actor 发送消息
    runBlocking {
        system.send(pid, "Native World")

        // 等待消息处理完成
        Thread.sleep(100)
    }

    println("Native example completed!")
}
