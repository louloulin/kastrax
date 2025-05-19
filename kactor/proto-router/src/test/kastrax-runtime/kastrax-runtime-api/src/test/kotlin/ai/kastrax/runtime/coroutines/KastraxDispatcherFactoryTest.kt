package ai.kastrax.runtime.coroutines

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KastraxDispatcherFactoryTest {
    private lateinit var mockRuntime: MockKastraxCoroutineRuntime
    
    @BeforeEach
    fun setup() {
        mockRuntime = MockKastraxCoroutineRuntime()
        KastraxCoroutineInitializer.initialize(mockRuntime)
    }
    
    @Test
    fun `test getDispatcher returns correct dispatcher for IO task type`() {
        // 获取IO调度器
        val dispatcher = KastraxDispatcherFactory.getDispatcher(KastraxDispatcherFactory.TaskType.IO)
        
        // 验证调用了正确的方法
        assertTrue(mockRuntime.ioDispatcherCalled)
    }
    
    @Test
    fun `test getDispatcher returns correct dispatcher for COMPUTE task type`() {
        // 获取计算调度器
        val dispatcher = KastraxDispatcherFactory.getDispatcher(KastraxDispatcherFactory.TaskType.COMPUTE)
        
        // 验证调用了正确的方法
        assertTrue(mockRuntime.computeDispatcherCalled)
    }
    
    @Test
    fun `test getDispatcher returns correct dispatcher for UI task type`() {
        // 获取UI调度器
        val dispatcher = KastraxDispatcherFactory.getDispatcher(KastraxDispatcherFactory.TaskType.UI)
        
        // 验证调用了正确的方法
        assertTrue(mockRuntime.uiDispatcherCalled)
    }
    
    @Test
    fun `test getDispatcher returns correct dispatcher for DEFAULT task type`() {
        // 获取默认调度器
        val dispatcher = KastraxDispatcherFactory.getDispatcher(KastraxDispatcherFactory.TaskType.DEFAULT)
        
        // 验证调用了正确的方法
        assertTrue(mockRuntime.computeDispatcherCalled)
    }
    
    @Test
    fun `test io returns IO dispatcher`() {
        // 获取IO调度器
        val dispatcher = KastraxDispatcherFactory.io()
        
        // 验证调用了正确的方法
        assertTrue(mockRuntime.ioDispatcherCalled)
    }
    
    @Test
    fun `test compute returns COMPUTE dispatcher`() {
        // 获取计算调度器
        val dispatcher = KastraxDispatcherFactory.compute()
        
        // 验证调用了正确的方法
        assertTrue(mockRuntime.computeDispatcherCalled)
    }
    
    @Test
    fun `test ui returns UI dispatcher`() {
        // 获取UI调度器
        val dispatcher = KastraxDispatcherFactory.ui()
        
        // 验证调用了正确的方法
        assertTrue(mockRuntime.uiDispatcherCalled)
    }
    
    @Test
    fun `test default returns DEFAULT dispatcher`() {
        // 获取默认调度器
        val dispatcher = KastraxDispatcherFactory.default()
        
        // 验证调用了正确的方法
        assertTrue(mockRuntime.computeDispatcherCalled)
    }
    
    @Test
    fun `test withTaskType executes block with correct dispatcher`() = runTest {
        // 使用withTaskType执行代码块
        val result = KastraxCoroutineGlobal.withTaskType(KastraxDispatcherFactory.TaskType.IO) {
            "result"
        }
        
        // 验证结果
        assertEquals("result", result)
        assertTrue(mockRuntime.ioDispatcherCalled)
    }
    
    // 模拟的协程运行时实现，用于测试
    private class MockKastraxCoroutineRuntime : KastraxCoroutineRuntime {
        var ioDispatcherCalled = false
        var computeDispatcherCalled = false
        var uiDispatcherCalled = false
        
        override fun getScope(owner: Any): KastraxCoroutineScope {
            return MockKastraxCoroutineScope()
        }
        
        override fun getScope(context: kotlin.coroutines.CoroutineContext): KastraxCoroutineScope {
            return MockKastraxCoroutineScope()
        }
        
        override fun ioDispatcher(): KastraxDispatcher {
            ioDispatcherCalled = true
            return MockKastraxDispatcher()
        }
        
        override fun computeDispatcher(): KastraxDispatcher {
            computeDispatcherCalled = true
            return MockKastraxDispatcher()
        }
        
        override fun uiDispatcher(): KastraxDispatcher {
            uiDispatcherCalled = true
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
