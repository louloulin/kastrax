package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 标准JVM环境的协程运行时实现
 */
class JvmCoroutineRuntime : KastraxCoroutineRuntime {
    override fun getScope(owner: Any): KastraxCoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return JvmCoroutineScope(scope)
    }

    override fun getScope(context: kotlin.coroutines.CoroutineContext): KastraxCoroutineScope {
        val scope = CoroutineScope(context + SupervisorJob())
        return JvmCoroutineScope(scope)
    }

    override fun ioDispatcher(): KastraxDispatcher {
        return JvmDispatcher(Dispatchers.IO)
    }

    override fun computeDispatcher(): KastraxDispatcher {
        return JvmDispatcher(Dispatchers.Default)
    }

    override fun uiDispatcher(): KastraxDispatcher {
        // 在标准JVM中，没有UI调度器，使用Unconfined作为后备
        return JvmDispatcher(Dispatchers.Unconfined)
    }

    override fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
    }

    override fun createCancellableScope(owner: Any): KastraxCoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return JvmCoroutineScope(scope)
    }

    override fun <T> flow(block: suspend ai.kastrax.runtime.coroutines.FlowCollector<T>.() -> Unit): KastraxFlow<T> {
        return JvmFlow(kotlinx.coroutines.flow.flow {
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
        return JvmSharedFlow(flow)
    }
}
