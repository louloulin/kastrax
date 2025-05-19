package ai.kastrax.runtime.coroutines

import kotlin.coroutines.Continuation

/**
 * kastrax调度器抽象
 */
interface KastraxDispatcher {
    /**
     * 在此调度器上执行代码块
     */
    suspend fun <T> withContext(block: suspend () -> T): T

    /**
     * 在此调度器上分发协程上下文
     */
    fun <T> dispatchContext(continuation: Continuation<T>): Continuation<T>
}
