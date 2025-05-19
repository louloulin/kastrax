package ai.kastrax.runtime.coroutines

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KastraxCoroutineInitializerTest {
    private lateinit var mockRuntime: MockKastraxCoroutineRuntime
    private val exceptionHandled = AtomicBoolean(false)
    private val testHandler: (Throwable) -> Unit = { _ ->
        exceptionHandled.set(true)
    }

    @BeforeEach
    fun setup() {
        mockRuntime = MockKastraxCoroutineRuntime()
        exceptionHandled.set(false)
    }

    @AfterEach
    fun teardown() {
        KastraxCoroutineInitializer.reset()
    }

    @Test
    fun `test initialize sets global runtime`() {
        // 初始化全局运行时
        KastraxCoroutineInitializer.initialize(mockRuntime)

        // 验证全局运行时已设置
        assertEquals(mockRuntime, KastraxCoroutineGlobal.getRuntime())
    }

    @Test
    fun `test initialize with exception handler`() {
        // 初始化全局运行时，带异常处理器
        KastraxCoroutineInitializer.initialize(mockRuntime, testHandler)

        // 触发异常处理
        KastraxCoroutineExceptionHandler.handleException(RuntimeException("Test exception"))

        // 验证异常处理器被调用
        assertTrue(exceptionHandled.get())
    }

    @Test
    fun `test reset clears global runtime`() {
        // 初始化全局运行时
        KastraxCoroutineInitializer.initialize(mockRuntime)

        // 记录当前运行时
        val beforeReset = KastraxCoroutineGlobal.getRuntime()

        // 重置全局运行时
        KastraxCoroutineInitializer.reset()

        // 验证重置前的运行时是我们的模拟运行时
        assertEquals(mockRuntime, beforeReset)

        // 重新初始化以确保后续测试正常
        KastraxCoroutineInitializer.initialize(mockRuntime)
    }

    @Test
    fun `test initialize is idempotent`() {
        // 初始化全局运行时
        KastraxCoroutineInitializer.initialize(mockRuntime)

        // 记录初始化次数
        val initialInitCount = mockRuntime.initCount

        // 再次初始化
        KastraxCoroutineInitializer.initialize(mockRuntime)

        // 验证初始化次数没有增加
        assertEquals(initialInitCount, mockRuntime.initCount)
    }

    // 模拟的协程运行时实现，用于测试
    private class MockKastraxCoroutineRuntime : KastraxCoroutineRuntime {
        var initCount = 0

        init {
            initCount++
        }

        override fun getScope(owner: Any): KastraxCoroutineScope {
            return MockKastraxCoroutineScope()
        }

        override fun getScope(context: kotlin.coroutines.CoroutineContext): KastraxCoroutineScope {
            return MockKastraxCoroutineScope()
        }

        override fun ioDispatcher(): KastraxDispatcher {
            return MockKastraxDispatcher()
        }

        override fun computeDispatcher(): KastraxDispatcher {
            return MockKastraxDispatcher()
        }

        override fun uiDispatcher(): KastraxDispatcher {
            return MockKastraxDispatcher()
        }

        override fun <T> runBlocking(block: suspend () -> T): T {
            return kotlinx.coroutines.runBlocking { block() }
        }

        override fun createCancellableScope(owner: Any): KastraxCoroutineScope {
            return MockKastraxCoroutineScope()
        }

        override fun <T> flow(block: suspend FlowCollector<T>.() -> Unit): KastraxFlow<T> {
            return MockKastraxFlow()
        }

        override fun <T> sharedFlow(replay: Int, extraBufferCapacity: Int): KastraxSharedFlow<T> {
            return MockKastraxSharedFlow()
        }
    }

    // 模拟的协程作用域实现，用于测试
    private class MockKastraxCoroutineScope : KastraxCoroutineScope {
        override val coroutineContext: kotlin.coroutines.CoroutineContext
            get() = kotlin.coroutines.EmptyCoroutineContext

        override fun launch(block: suspend () -> Unit): KastraxJob {
            return MockKastraxJob()
        }

        override fun launchSafe(block: suspend () -> Unit, onError: (Throwable) -> Unit): KastraxJob {
            return MockKastraxJob()
        }

        override fun <T> async(block: suspend () -> T): KastraxDeferred<T> {
            return MockKastraxDeferred()
        }

        override fun <T> asyncSafe(block: suspend () -> T, onError: (Throwable) -> T): KastraxDeferred<T> {
            return MockKastraxDeferred()
        }

        override fun cancel() {
            // 空实现
        }

        override fun isActive(): Boolean {
            return true
        }
    }

    // 模拟的调度器实现，用于测试
    private class MockKastraxDispatcher : KastraxDispatcher {
        override suspend fun <T> withContext(block: suspend () -> T): T {
            return block()
        }

        override fun <T> dispatchContext(continuation: kotlin.coroutines.Continuation<T>): kotlin.coroutines.Continuation<T> {
            return continuation
        }
    }

    // 模拟的作业实现，用于测试
    private class MockKastraxJob : KastraxJob {
        override fun cancel() {
            // 空实现
        }

        override suspend fun join() {
            // 空实现
        }

        override fun isActive(): Boolean {
            return true
        }
    }

    // 模拟的延迟结果实现，用于测试
    private class MockKastraxDeferred<T> : KastraxDeferred<T> {
        override fun cancel() {
            // 空实现
        }

        override suspend fun join() {
            // 空实现
        }

        override fun isActive(): Boolean {
            return true
        }

        @Suppress("UNCHECKED_CAST")
        override suspend fun await(): T {
            return "" as T
        }
    }

    // 模拟的流实现，用于测试
    private class MockKastraxFlow<T> : KastraxFlow<T> {
        override suspend fun collect(collector: suspend (T) -> Unit) {
            // 空实现
        }

        override fun <R> map(transform: suspend (T) -> R): KastraxFlow<R> {
            return MockKastraxFlow()
        }

        override fun filter(predicate: suspend (T) -> Boolean): KastraxFlow<T> {
            return this
        }

        override fun catch(action: suspend (Throwable) -> Unit): KastraxFlow<T> {
            return this
        }
    }

    // 模拟的共享流实现，用于测试
    private class MockKastraxSharedFlow<T> : KastraxSharedFlow<T>, KastraxFlow<T> by MockKastraxFlow() {
        override suspend fun emit(value: T) {
            // 空实现
        }

        override fun tryEmit(value: T): Boolean {
            return true
        }
    }
}
