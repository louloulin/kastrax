package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*

/**
 * 用于测试的协程运行时实现
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class TestCoroutineRuntime(
    private val testDispatcher: kotlinx.coroutines.test.TestDispatcher = kotlinx.coroutines.test.StandardTestDispatcher()
) : KastraxCoroutineRuntime {
    private val testScope = TestScope(testDispatcher)

    override fun getScope(owner: Any): KastraxCoroutineScope {
        return TestCoroutineScope(testScope)
    }

    override fun getScope(context: kotlin.coroutines.CoroutineContext): KastraxCoroutineScope {
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
        var result: T? = null
        testScope.runTest {
            result = block()
        }
        return result!!
    }

    /**
     * 运行当前所有待处理的协程
     */
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun runCurrent() {
        testScope.testScheduler.runCurrent()
    }

    /**
     * 设置是否自动推进虚拟时间
     *
     * @param autoAdvance 是否自动推进虚拟时间
     */
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun setAutoAdvance(autoAdvance: Boolean) {
        // 在测试环境中，我们可以使用advanceUntilIdle来模拟自动推进
        if (autoAdvance) {
            // 立即运行所有待处理的协程
            testScope.testScheduler.advanceUntilIdle()
        }
    }

    /**
     * 推进虚拟时间
     */
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun advanceTimeBy(delayTimeMillis: Long) {
        testScope.testScheduler.advanceTimeBy(delayTimeMillis)
    }

    override fun createCancellableScope(owner: Any): KastraxCoroutineScope {
        return getScope(owner)
    }

    override fun <T> flow(block: suspend ai.kastrax.runtime.coroutines.FlowCollector<T>.() -> Unit): KastraxFlow<T> {
        return TestFlow(kotlinx.coroutines.flow.flow {
            val collector = object : ai.kastrax.runtime.coroutines.FlowCollector<T> {
                override suspend fun emit(value: T) {
                    this@flow.emit(value)
                }
            }
            block(collector)
        })
    }

    override fun <T> sharedFlow(replay: Int, extraBufferCapacity: Int): KastraxSharedFlow<T> {
        val flow = MutableSharedFlow<T>(replay = replay, extraBufferCapacity = extraBufferCapacity)
        return TestSharedFlow(flow)
    }


}
