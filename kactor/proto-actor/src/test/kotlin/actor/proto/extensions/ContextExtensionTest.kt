package actor.proto.extensions

import actor.proto.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 测试Actor，用于测试上下文扩展
 */
class TestActor(private val latch: CountDownLatch) : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> {
                // 在Actor启动时设置用户数据
                this.setUserData(mapOf(
                    "name" to "Test User",
                    "age" to 25
                ))
            }
            is String -> {
                when (msg) {
                    "get-data" -> {
                        // 获取用户数据并响应
                        val userData = this.getUserData()
                        if (userData != null) {
                            val name = userData.get("name") as? String
                            val age = userData.get("age") as? Int
                            this.respond(mapOf("name" to name, "age" to age))
                        } else {
                            this.respond(mapOf<String, Any>())
                        }
                        latch.countDown()
                    }
                }
            }
        }
    }
}

class ContextExtensionTest {

    @Test
    fun `should create and retrieve context extension`() {
        // 创建扩展ID
        val extensionId = ContextExtensionID.next()

        // 创建扩展实例
        val extension = object : ContextExtension {
            override fun extensionId(): ContextExtensionID = extensionId
            val value = "test-value"
        }

        // 创建扩展容器
        val extensions = ContextExtensions()

        // 设置扩展
        extensions.set(extension)

        // 获取扩展
        val retrieved = extensions.get(extensionId)

        // 验证
        assertNotNull(retrieved)
        assertEquals(extension, retrieved)
        val retrievedExtension = retrieved as? Any
        assertEquals("test-value", retrievedExtension?.javaClass?.getDeclaredField("value")?.apply { isAccessible = true }?.get(retrievedExtension))
    }

    @Test
    fun `should handle multiple extensions`() {
        // 创建扩展ID
        val extensionId1 = ContextExtensionID.next()
        val extensionId2 = ContextExtensionID.next()

        // 创建扩展实例
        val extension1 = object : ContextExtension {
            override fun extensionId(): ContextExtensionID = extensionId1
            val value = "value-1"
        }

        val extension2 = object : ContextExtension {
            override fun extensionId(): ContextExtensionID = extensionId2
            val value = "value-2"
        }

        // 创建扩展容器
        val extensions = ContextExtensions()

        // 设置扩展
        extensions.set(extension1)
        extensions.set(extension2)

        // 获取扩展
        val retrieved1 = extensions.get(extensionId1)
        val retrieved2 = extensions.get(extensionId2)

        // 验证
        assertNotNull(retrieved1)
        assertNotNull(retrieved2)
        assertEquals(extension1, retrieved1)
        assertEquals(extension2, retrieved2)
    }

    @Test
    fun `should use extension in actor context`() {
        // 创建一个扩展实例
        val userData = UserDataExtension(mapOf(
            "name" to "Test User",
            "age" to 25
        ))

        // 创建一个扩展容器
        val extensions = ContextExtensions()

        // 设置扩展
        extensions.set(userData)

        // 验证可以获取扩展
        val retrievedData = extensions.get(UserDataExtension.ID) as? UserDataExtension

        // 验证数据
        assertNotNull(retrievedData)
        assertEquals("Test User", retrievedData?.get("name"))
        assertEquals(25, retrievedData?.get("age"))
    }

    @Test
    fun `should use extension in root context`() {
        // 创建Actor系统
        val system = ActorSystem.default()

        // 获取根上下文
        val rootContext = system.root

        // 设置用户数据
        rootContext.setUserData(mapOf(
            "system" to "test-system",
            "version" to 1.0
        ))

        // 获取用户数据
        val userData = rootContext.getUserData()

        // 验证
        assertNotNull(userData)
        assertEquals("test-system", userData?.get("system"))
        assertEquals(1.0, userData?.get("version"))
    }
}
