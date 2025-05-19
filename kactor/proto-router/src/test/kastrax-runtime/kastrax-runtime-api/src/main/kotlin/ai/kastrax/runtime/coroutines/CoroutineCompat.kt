package ai.kastrax.runtime.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 协程兼容性工具类
 *
 * 提供与kotlinx.coroutines兼容的API，但内部使用kastrax协程运行时
 */
object CoroutineCompat {
    /**
     * 兼容性launch方法
     *
     * 替代kotlinx.coroutines.GlobalScope.launch
     *
     * @param block 要执行的代码块
     * @return 协程作业
     */
    fun globalLaunch(block: suspend CoroutineScope.() -> Unit): Job {
        val kastraxScope = KastraxCoroutineGlobal.getScope(GlobalScope)
        return kastraxScope.launch { block(GlobalScope) } as Job
    }

    /**
     * 兼容性launch方法
     *
     * 替代kotlinx.coroutines.CoroutineScope.launch
     *
     * @param scope 协程作用域
     * @param block 要执行的代码块
     * @return 协程作业
     */
    fun scopeLaunch(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit): Job {
        val kastraxScope = KastraxCoroutineGlobal.getScope(scope)
        return kastraxScope.launch { block(scope) } as Job
    }

    /**
     * 兼容性withContext方法
     *
     * 替代kotlinx.coroutines.withContext
     *
     * @param dispatcher 调度器
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    suspend fun <T> withContextCompat(
        dispatcher: CoroutineDispatcher,
        block: suspend CoroutineScope.() -> T
    ): T {
        val kastraxDispatcher = when (dispatcher) {
            Dispatchers.IO -> KastraxCoroutineGlobal.ioDispatcher()
            Dispatchers.Default -> KastraxCoroutineGlobal.computeDispatcher()
            Dispatchers.Main -> KastraxCoroutineGlobal.uiDispatcher()
            else -> KastraxCoroutineGlobal.computeDispatcher()
        }

        return kastraxDispatcher.withContext {
            block(CoroutineScope(kotlinx.coroutines.coroutineContext))
        }
    }

    /**
     * 兼容性IO调度器
     *
     * 替代Dispatchers.IO
     */
    val IO: KastraxDispatcher
        get() = KastraxCoroutineGlobal.ioDispatcher()

    /**
     * 兼容性Default调度器
     *
     * 替代Dispatchers.Default
     */
    val Default: KastraxDispatcher
        get() = KastraxCoroutineGlobal.computeDispatcher()

    /**
     * 兼容性Main调度器
     *
     * 替代Dispatchers.Main
     */
    val Main: KastraxDispatcher
        get() = KastraxCoroutineGlobal.uiDispatcher()

    /**
     * 创建兼容性协程作用域
     *
     * @param dispatcher 调度器
     * @return 协程作用域
     */
    fun createScope(dispatcher: CoroutineDispatcher = Dispatchers.Default): CoroutineScope {
        val kastraxScope = KastraxCoroutineGlobal.createScope()
        return CoroutineScope(kastraxScope.coroutineContext)
    }
}
