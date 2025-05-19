package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal
import ai.kastrax.runtime.coroutines.KastraxCoroutineInitializer
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineScope
import kotlinx.coroutines.delay
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmCoroutineRuntimeTest {
    private lateinit var runtime: JvmCoroutineRuntime
    private lateinit var scope: KastraxCoroutineScope

    @BeforeEach
    fun setup() {
        runtime = JvmCoroutineRuntime()
        scope = runtime.getScope(this)
        KastraxCoroutineInitializer.initialize(runtime)
    }

    @AfterEach
    fun teardown() {
        scope.cancel()
        KastraxCoroutineInitializer.reset()
    }

    @Test
    fun `test getScope creates valid scope`() {
        assertTrue(scope.isActive())
    }

    @Test
    fun `test launch executes block`() {
        var executed = false

        val job = scope.launch {
            executed = true
        }

        runtime.runBlocking {
            job.join()
        }

        assertTrue(executed)
    }

    @Test
    fun `test async returns result`() {
        val deferred = scope.async {
            delay(100)
            "Result"
        }

        val result = runtime.runBlocking {
            deferred.await()
        }

        assertEquals("Result", result)
    }

    @Test
    fun `test flow collects values`() {
        val flow = runtime.flow<Int> {
            emit(1)
            emit(2)
            emit(3)
        }

        val results = mutableListOf<Int>()

        runtime.runBlocking {
            flow.collect { value ->
                results.add(value)
            }
        }

        assertEquals(listOf(1, 2, 3), results)
    }

    @Test
    fun `test sharedFlow emits values`() {
        // 使用replay=1确保至少保留最后一个值
        val sharedFlow = runtime.sharedFlow<String>(replay = 1, extraBufferCapacity = 10)

        val results = mutableListOf<String>()

        // 先启动收集器
        val job = scope.launch {
            sharedFlow.collect { value ->
                results.add(value)
            }
        }

        // 给收集器一些时间启动
        runtime.runBlocking {
            delay(50)
            sharedFlow.emit("Hello")
            sharedFlow.emit("World")
            delay(100) // 给收集器时间处理
            job.cancel()
        }

        assertEquals(listOf("Hello", "World"), results)
    }

    @Test
    fun `test launchSafe with exception handling`() {
        val errorHandled = AtomicBoolean(false)

        val job = scope.launchSafe(
            block = {
                throw RuntimeException("Test exception")
            },
            onError = {
                errorHandled.set(true)
            }
        )

        // 等待协程执行完成
        runtime.runBlocking {
            try {
                job.join()
            } catch (e: Exception) {
                // 忽略异常
            }
        }

        // 验证异常被处理
        assertTrue(errorHandled.get())
    }

    @Test
    fun `test asyncSafe with exception handling`() {
        val deferred = scope.asyncSafe(
            block = {
                throw RuntimeException("Test exception")
            },
            onError = {
                "fallback"
            }
        )

        // 等待结果
        val result = runtime.runBlocking {
            deferred.await()
        }

        // 验证异常被处理，返回了后备值
        assertEquals("fallback", result)
    }

    @Test
    fun `test withIO dispatcher`() {
        val result = runtime.runBlocking {
            KastraxCoroutineGlobal.withIO {
                "io result"
            }
        }

        // 验证结果
        assertEquals("io result", result)
    }

    @Test
    fun `test withCompute dispatcher`() {
        val result = runtime.runBlocking {
            KastraxCoroutineGlobal.withCompute {
                "compute result"
            }
        }

        // 验证结果
        assertEquals("compute result", result)
    }

    @Test
    fun `test multiple concurrent operations`() {
        val counter = AtomicInteger(0)
        val jobs = List(10) {
            scope.launch {
                counter.incrementAndGet()
            }
        }

        // 等待所有协程执行完成
        runtime.runBlocking {
            jobs.forEach { it.join() }
        }

        // 验证所有协程都执行了
        assertEquals(10, counter.get())
    }
}
