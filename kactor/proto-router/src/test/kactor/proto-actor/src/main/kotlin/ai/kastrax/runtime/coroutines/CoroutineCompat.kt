package ai.kastrax.runtime.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

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
        val scope = KastraxCoroutineGlobal.getScope("globalLaunch")
        return (scope.launch { block(kotlinx.coroutines.GlobalScope) }) as Job
    }

    /**
     * 兼容性async方法
     *
     * 替代kotlinx.coroutines.GlobalScope.async
     *
     * @param block 要执行的代码块
     * @return 延迟结果
     */
    fun <T> globalAsync(block: suspend CoroutineScope.() -> T): Deferred<T> {
        val scope = KastraxCoroutineGlobal.getScope("globalAsync")
        return (scope.async { block(kotlinx.coroutines.GlobalScope) }) as Deferred<T>
    }
}
