package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import kotlinx.coroutines.delay
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmCoroutineRuntimeTest {

    @Test
    fun `test getScope creates valid scope`() {
        val runtime = JvmCoroutineRuntime()
        val scope = runtime.getScope(this)

        assertTrue(scope.isActive())
    }

    @Test
    fun `test launch executes block`() {
        val runtime = JvmCoroutineRuntime()
        val scope = runtime.getScope(this)
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
        val runtime = JvmCoroutineRuntime()
        val scope = runtime.getScope(this)

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
        val runtime = JvmCoroutineRuntime()
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
        val runtime = JvmCoroutineRuntime()
        // 使用replay=1确保至少保留最后一个值
        val sharedFlow = runtime.sharedFlow<String>(replay = 1, extraBufferCapacity = 10)

        val results = mutableListOf<String>()

        // 先启动收集器
        val job = runtime.getScope(this).launch {
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
}
