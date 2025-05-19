package ai.kastrax.runtime.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 协程上下文拦截器
 *
 * 拦截所有协程调用，替换为kastrax协程运行时
 */
class KastraxCoroutineInterceptor : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    /**
     * 拦截协程调度器
     *
     * @param context 协程上下文
     * @return 拦截后的协程调度器
     */
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        val dispatcher = continuation.context[ContinuationInterceptor] as? CoroutineDispatcher

        // 如果没有调度器，使用默认调度器
        if (dispatcher == null) {
            return KastraxCoroutineGlobal.computeDispatcher().dispatchContext(continuation)
        }

        // 根据调度器类型选择对应的kastrax调度器
        val kastraxDispatcher = when (dispatcher) {
            Dispatchers.IO -> KastraxCoroutineGlobal.ioDispatcher()
            Dispatchers.Default -> KastraxCoroutineGlobal.computeDispatcher()
            Dispatchers.Main -> KastraxCoroutineGlobal.uiDispatcher()
            else -> KastraxCoroutineGlobal.computeDispatcher()
        }

        return kastraxDispatcher.dispatchContext(continuation)
    }

    /**
     * 获取协程上下文中的键
     *
     * @param key 键
     * @return 键对应的值
     */
    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
        return if (key == ContinuationInterceptor.Key) {
            this as E
        } else {
            null
        }
    }

    /**
     * 合并协程上下文
     *
     * @param context 要合并的协程上下文
     * @return 合并后的协程上下文
     */
    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        return if (key == ContinuationInterceptor.Key) {
            EmptyCoroutineContext
        } else {
            this
        }
    }
}
