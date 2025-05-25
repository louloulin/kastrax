package ai.kastrax.runtime.coroutines

import ai.kastrax.runtime.coroutines.test.TestCoroutineRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalCoroutinesApi
class KastraxCoroutineGlobalTest {

    private lateinit var testRuntime: TestCoroutineRuntime

    @BeforeEach
    fun setup() {
        // 创建测试运行时
        testRuntime = TestCoroutineRuntime()

        // 初始化全局协程运行时
        KastraxCoroutineInitializer.initialize(testRuntime)

        // 设置测试调度器为立即执行模式
        testRuntime.setAutoAdvance(true)
    }

    @AfterEach
    fun tearDown() {
        // 重置全局协程运行时
        KastraxCoroutineInitializer.reset()
    }

    @Test
    fun `test withIO executes in IO dispatcher`() {
        // 在IO调度器上执行代码块
        runBlockingKastrax {
            val result = withIO {
                // 模拟IO操作
                delay(100)
                "IO result"
            }

            // 验证结果
            assertEquals("IO result", result)
        }
    }

    @Test
    fun `test withCompute executes in compute dispatcher`() {
        // 在计算调度器上执行代码块
        runBlockingKastrax {
            val result = withCompute {
                // 模拟计算操作
                delay(100)
                "Compute result"
            }

            // 验证结果
            assertEquals("Compute result", result)
        }
    }

    @Test
    fun `test withUI executes in UI dispatcher`() {
        // 在UI调度器上执行代码块
        runBlockingKastrax {
            val result = withUI {
                // 模拟UI操作
                delay(100)
                "UI result"
            }

            // 验证结果
            assertEquals("UI result", result)
        }
    }

    @Test
    fun `test launch launches a new coroutine`() {
        // 创建计数器
        val counter = AtomicInteger(0)

        // 获取作用域
        val scope = getScope(this)

        // 启动协程
        scope.launchKastrax {
            // 模拟操作
            counter.incrementAndGet()
        }

        // 运行所有待处理的协程
        testRuntime.runCurrent()

        // 验证计数器值
        assertEquals(1, counter.get())
    }

    @Test
    fun `test createScope creates a new coroutine scope`() {
        // 创建作用域
        val scope = createScope()

        // 验证作用域不为空
        assertNotNull(scope)
    }

    @Test
    fun `test IO dispatcher`() {
        // 获取IO调度器
        val dispatcher = IO

        // 验证调度器不为空
        assertNotNull(dispatcher)

        // 在IO调度器上执行代码块
        runBlockingKastrax {
            val result = dispatcher.withContext {
                // 模拟IO操作
                delay(100)
                "IO result"
            }

            // 验证结果
            assertEquals("IO result", result)
        }
    }

    @Test
    fun `test Default dispatcher`() {
        // 获取计算调度器
        val dispatcher = Default

        // 验证调度器不为空
        assertNotNull(dispatcher)

        // 在计算调度器上执行代码块
        runBlockingKastrax {
            val result = dispatcher.withContext {
                // 模拟计算操作
                delay(100)
                "Compute result"
            }

            // 验证结果
            assertEquals("Compute result", result)
        }
    }

    @Test
    fun `test Main dispatcher`() {
        // 获取UI调度器
        val dispatcher = Main

        // 验证调度器不为空
        assertNotNull(dispatcher)

        // 在UI调度器上执行代码块
        runBlockingKastrax {
            val result = dispatcher.withContext {
                // 模拟UI操作
                delay(100)
                "UI result"
            }

            // 验证结果
            assertEquals("UI result", result)
        }
    }
}
