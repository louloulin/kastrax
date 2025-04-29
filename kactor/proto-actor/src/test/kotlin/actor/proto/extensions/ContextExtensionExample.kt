package actor.proto.extensions

import actor.proto.*
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 示例上下文扩展，用于存储和检索用户数据
 */
class UserDataExtension(private val userData: Map<String, Any>) : ContextExtension {
    companion object {
        // 创建一个全局唯一的扩展ID
        val ID = ContextExtensionID.next()
    }

    /**
     * 获取扩展ID
     * @return 扩展ID
     */
    override fun extensionId(): ContextExtensionID = ID

    /**
     * 获取用户数据
     * @param key 数据键
     * @return 数据值，如果不存在则返回null
     */
    fun get(key: String): Any? = userData[key]

    /**
     * 检查是否包含指定键的数据
     * @param key 数据键
     * @return 如果包含则返回true，否则返回false
     */
    fun contains(key: String): Boolean = userData.containsKey(key)
}

/**
 * 从上下文中获取UserDataExtension
 * @return UserDataExtension实例，如果不存在则返回null
 */
fun ExtensionContext.getUserData(): UserDataExtension? {
    return this.get(UserDataExtension.ID) as? UserDataExtension
}

/**
 * 设置UserDataExtension到上下文
 * @param userData 用户数据
 */
fun ExtensionContext.setUserData(userData: Map<String, Any>) {
    this.set(UserDataExtension(userData))
}

/**
 * 示例Actor，使用上下文扩展
 */
class UserActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> {
                // 在Actor启动时设置用户数据
                this.setUserData(mapOf(
                    "name" to "John Doe",
                    "age" to 30,
                    "email" to "john.doe@example.com"
                ))
                println("UserActor started with user data")
            }
            is String -> {
                when (msg) {
                    "get-name" -> {
                        // 获取用户数据并响应
                        val userData = this.getUserData()
                        if (userData != null) {
                            val name = userData.get("name") as? String
                            this.respond(name ?: "Unknown")
                        } else {
                            this.respond("No user data found")
                        }
                    }
                    "get-age" -> {
                        val userData = this.getUserData()
                        if (userData != null) {
                            val age = userData.get("age") as? Int
                            this.respond(age ?: 0)
                        } else {
                            this.respond(0)
                        }
                    }
                    "check-email" -> {
                        val userData = this.getUserData()
                        if (userData != null) {
                            val hasEmail = userData.contains("email")
                            this.respond(hasEmail)
                        } else {
                            this.respond(false)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 主函数，演示上下文扩展的使用
 */
fun main() = runBlocking {
    println("=== Context Extension Example ===")

    // 创建Actor系统
    val system = ActorSystem.default()

    // 创建UserActor
    val props = fromProducer { UserActor() }
    val pid = system.actorOf(props)

    // 等待Actor启动
    Thread.sleep(100)

    // 请求用户数据
    val name = system.requestAsync<String>(pid, "get-name", Duration.ofSeconds(5))
    val age = system.requestAsync<Int>(pid, "get-age", Duration.ofSeconds(5))
    val hasEmail = system.requestAsync<Boolean>(pid, "check-email", Duration.ofSeconds(5))

    // 打印结果
    println("Name: $name")
    println("Age: $age")
    println("Has Email: $hasEmail")

    // 停止Actor
    system.stop(pid)

    println("Example completed")
}
