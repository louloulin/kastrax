package actor.proto.examples.remoteblocklist

import actor.proto.*
import actor.proto.remote.Remote
import actor.proto.remote.RemoteConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration

/**
 * 消息类
 */
data class Message(val text: String)

/**
 * 响应类
 */
data class Response(val text: String)

/**
 * 示例 Actor
 */
class ExampleActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> {
                println("ExampleActor started")
            }
            is Message -> {
                println("Received message: ${msg.text}")
                respond(Response("Response to: ${msg.text}"))
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
    val system = ActorSystem("remote-blocklist-example")
    
    // 配置远程系统
    val config = RemoteConfig(
        hostname = "localhost",
        port = 8090,
        enableBlocklist = true,
        blocklistMaxFailures = 2,
        blocklistDuration = Duration.ofSeconds(10)
    )
    
    // 启动远程系统
    Remote.create(system, config)
    Remote.start("localhost", 8090, config)
    
    // 注册 Actor 类型
    Remote.registerKnownKind("example-actor", fromProducer { ExampleActor() })
    
    // 创建本地 Actor
    val props = fromProducer { ExampleActor() }
    val localPid = system.actorOf(props)
    
    // 创建远程 Actor
    val remotePid = Remote.spawnNamed("localhost:8090", "remote-example", "example-actor", Duration.ofSeconds(5))
    
    println("Local PID: $localPid")
    println("Remote PID: $remotePid")
    
    // 向远程 Actor 发送消息
    println("Sending message to remote actor...")
    val response = system.requestAsync<Response>(remotePid, Message("Hello from local"), Duration.ofSeconds(5))
    println("Received response: ${response.await()}")
    
    // 手动阻止远程地址
    println("Blocking remote address...")
    Remote.blocklistManager.block(remotePid.address, Duration.ofSeconds(10))
    
    // 尝试向被阻止的远程 Actor 发送消息
    println("Trying to send message to blocked remote actor...")
    try {
        val response2 = system.requestAsync<Response>(remotePid, Message("This should fail"), Duration.ofSeconds(5))
        println("Received response: ${response2.await()}")
    } catch (e: Exception) {
        println("Failed to send message to blocked address: ${e.message}")
    }
    
    // 等待阻止过期
    println("Waiting for block to expire...")
    delay(11000)
    
    // 检查地址是否仍被阻止
    val isBlocked = Remote.blocklistManager.isBlocked(remotePid.address)
    println("Is address still blocked: $isBlocked")
    
    // 如果地址不再被阻止，尝试再次发送消息
    if (!isBlocked) {
        println("Sending message to unblocked remote actor...")
        val response3 = system.requestAsync<Response>(remotePid, Message("Hello again"), Duration.ofSeconds(5))
        println("Received response: ${response3.await()}")
    }
    
    // 模拟远程节点失败
    println("Simulating remote node failures...")
    for (i in 1..3) {
        println("Recording failure $i...")
        val blocked = Remote.blocklistManager.recordFailure(remotePid.address, null, "Simulated failure")
        println("Address blocked: $blocked")
    }
    
    // 检查地址是否被阻止
    val isBlockedAfterFailures = Remote.blocklistManager.isBlocked(remotePid.address)
    println("Is address blocked after failures: $isBlockedAfterFailures")
    
    // 获取阻止信息
    val blockInfo = Remote.blocklistManager.getBlockInfo(remotePid.address)
    if (blockInfo != null) {
        println("Block info: address=${blockInfo.address}, blockedAt=${blockInfo.blockedAt}, duration=${blockInfo.duration}, reason=${blockInfo.reason}, failureCount=${blockInfo.failureCount}")
    }
    
    // 手动解除阻止
    println("Manually unblocking address...")
    Remote.blocklistManager.unblock(remotePid.address)
    
    // 检查地址是否仍被阻止
    val isBlockedAfterUnblock = Remote.blocklistManager.isBlocked(remotePid.address)
    println("Is address still blocked: $isBlockedAfterUnblock")
    
    // 停止远程系统
    println("Shutting down...")
    Remote.shutdown(true)
    
    println("Example completed!")
}
