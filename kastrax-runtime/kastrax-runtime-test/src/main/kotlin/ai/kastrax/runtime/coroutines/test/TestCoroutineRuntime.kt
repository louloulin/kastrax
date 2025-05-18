package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*

/**
 * 用于测试的协程运行时实现
 */
class TestCoroutineRuntime(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : KastraxCoroutineRuntime {
    private val testScope = TestScope(testDispatcher)
    
    override fun getScope(owner: Any): KastraxCoroutineScope {
        return TestCoroutineScope(testScope)
    }
    
    override fun ioDispatcher(): KastraxDispatcher {
        return TestDispatcher(testDispatcher)
    }
    
    override fun computeDispatcher(): KastraxDispatcher {
        return TestDispatcher(testDispatcher)
    }
    
    override fun uiDispatcher(): KastraxDispatcher {
        return TestDispatcher(testDispatcher)
    }
    
    override fun <T> runBlocking(block: suspend () -> T): T {
        return testScope.runTest { block() }
    }
    
    override fun createCancellableScope(owner: Any): KastraxCoroutineScope {
        return getScope(owner)
    }
    
    override fun <T> flow(block: suspend FlowCollector<T>.() -> Unit): KastraxFlow<T> {
        return TestFlow(kotlinx.coroutines.flow.flow { 
            val collector = object : FlowCollector<T> {
                override suspend fun emit(value: T) {
                    emit(value)
                }
            }
            block(collector)
        })
    }
    
    override fun <T> sharedFlow(replay: Int, extraBufferCapacity: Int): KastraxSharedFlow<T> {
        val flow = MutableSharedFlow<T>(replay = replay, extraBufferCapacity = extraBufferCapacity)
        return TestSharedFlow(flow)
    }
    
    /**
     * 推进虚拟时间
     */
    fun advanceTimeBy(delayTimeMillis: Long) {
        testScope.testScheduler.advanceTimeBy(delayTimeMillis)
    }
    
    /**
     * 运行所有待处理的协程直到完成
     */
    fun runCurrent() {
        testScope.testScheduler.runCurrent()
    }
}
