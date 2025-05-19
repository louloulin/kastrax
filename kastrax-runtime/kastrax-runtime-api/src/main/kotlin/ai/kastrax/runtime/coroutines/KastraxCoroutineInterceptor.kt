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
     * 构造函数
     */
    init {
        // 安装GlobalScope拦截器
        installGlobalScopeInterceptor()
    }

    /**
     * 安装GlobalScope拦截器
     */
    private fun installGlobalScopeInterceptor() {
        try {
            // 获取GlobalScope类
            val globalScopeClass = kotlinx.coroutines.GlobalScope::class.java

            // 获取coroutineContext字段
            val contextField = globalScopeClass.getDeclaredField("coroutineContext")
            contextField.isAccessible = true

            // 获取当前上下文
            val currentContext = contextField.get(kotlinx.coroutines.GlobalScope) as CoroutineContext

            // 创建新的上下文，包含拦截器
            val newContext = currentContext + this

            // 替换上下文
            contextField.set(kotlinx.coroutines.GlobalScope, newContext)

            println("Successfully installed interceptor for GlobalScope")
        } catch (e: Exception) {
            // 忽略反射异常，不影响正常使用
            println("Failed to install interceptor for GlobalScope: ${e.message}")
        }
    }
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
