package actor.proto.native

import actor.proto.*
import actor.proto.mailbox.newUnboundedMailbox
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 复杂示例，展示 ProtoActor 在 Native Image 中的高级功能
 * 
 * 这个示例展示了：
 * 1. Actor 层次结构和监督
 * 2. 消息传递和状态管理
 * 3. 请求-响应模式
 * 4. Actor 重入
 * 5. 错误处理和恢复
 */

// 消息定义
data class Add(val value: Int)
data class Subtract(val value: Int)
data class GetValue(val replyTo: PID)
data class Result(val value: Int)
data class Divide(val value: Int)
object Reset
object Crash

// 计算器 Actor
class CalculatorActor : Actor {
    private var value = 0
    
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Add -> value += msg.value
            is Subtract -> value -= msg.value
            is GetValue -> send(msg.replyTo, Result(value))
            is Reset -> value = 0
            is Divide -> {
                if (msg.value == 0) {
                    throw ArithmeticException("除以零")
                }
                value /= msg.value
            }
            is Crash -> throw RuntimeException("模拟崩溃")
            else -> println("计算器收到未知消息: ${msg.javaClass.name}")
        }
    }
}

// 监督者 Actor
class SupervisorActor : Actor {
    // 使用可变列表存储子 Actor
    private val childNames = mutableListOf<String>()
    private val childPids = mutableListOf<PID>()
    private val restartCount = AtomicInteger(0)
    
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> println("监督者已启动")
            
            is String -> {
                // 创建新的计算器 Actor
                val props = fromProducer { CalculatorActor() }
                    .withMailbox { newUnboundedMailbox() }
                    .withChildSupervisorStrategy(OneForOneStrategy({ _, _ -> SupervisorDirective.Restart }, 3, Duration.ofSeconds(1)))
                
                val child = spawnNamed(props, msg)
                childNames.add(msg)
                childPids.add(child)
                send(sender!!, child)
            }
            
            is Terminated -> {
                val childId = msg.who.id
                println("子 Actor $childId 已终止")
                
                // 查找并移除终止的子 Actor
                val index = childPids.indexOfFirst { it.id == childId }
                if (index >= 0) {
                    childNames.removeAt(index)
                    childPids.removeAt(index)
                }
                
                restartCount.incrementAndGet()
            }
            
            is GetValue -> {
                // 获取所有计算器的值
                val results = mutableMapOf<String, Int>()
                val latch = CountDownLatch(childPids.size)
                
                for (i in childNames.indices) {
                    val name = childNames[i]
                    val pid = childPids[i]
                    
                    try {
                        // 使用协程处理结果
                        runBlocking {
                            try {
                                val result = ActorSystem.default().requestAsync<Result>(pid, GetValue(self), Duration.ofSeconds(5))
                                results[name] = result.value
                            } catch (e: Exception) {
                                println("获取 $name 的值时出错: ${e.message}")
                            } finally {
                                latch.countDown()
                            }
                        }
                    } catch (e: Exception) {
                        println("创建请求时出错: ${e.message}")
                        latch.countDown()
                    }
                }
                
                // 等待所有请求完成
                latch.await(5, TimeUnit.SECONDS)
                
                // 发送结果
                send(msg.replyTo, results)
            }
            
            else -> println("监督者收到未知消息: ${msg.javaClass.name}")
        }
    }
}

fun main() {
    // 创建 Actor 系统
    val system = ActorSystem("complex-native-system")
    
    // 创建监督者 Actor
    val supervisorProps = fromProducer { SupervisorActor() }
    val supervisor = system.actorOf(supervisorProps, "supervisor")
    
    runBlocking {
        // 创建三个计算器 Actor
        val calc1 = system.requestAsync<PID>(supervisor, "calc1", Duration.ofSeconds(1))
        val calc2 = system.requestAsync<PID>(supervisor, "calc2", Duration.ofSeconds(1))
        val calc3 = system.requestAsync<PID>(supervisor, "calc3", Duration.ofSeconds(1))
        
        // 向计算器发送操作
        system.send(calc1, Add(10))
        system.send(calc2, Add(20))
        system.send(calc3, Add(30))
        
        system.send(calc1, Subtract(5))
        // 这个消息类型不存在，会被忽略
        system.send(calc2, Multiply(2))  
        system.send(calc3, Divide(3))
        
        // 尝试触发错误
        system.send(calc2, Divide(0))  // 会触发除以零错误，但会被监督者恢复
        
        // 等待错误恢复
        delay(1000)
        
        // 获取所有计算器的值
        val results = system.requestAsync<Map<String, Int>>(supervisor, GetValue(system.deadLetter()), Duration.ofSeconds(5))
        
        println("计算器的值:")
        results.forEach { (name, value) ->
            println("$name: $value")
        }
        
        // 关闭系统
        system.stop(supervisor)
        delay(100)
    }
    
    println("复杂示例完成!")
}

// 这个消息类型不存在，用于测试未知消息处理
data class Multiply(val value: Int)
