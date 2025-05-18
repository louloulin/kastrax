package ai.kastrax.core.runtime

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory
import ai.kastrax.runtime.coroutines.KastraxCoroutineScope
import ai.kastrax.runtime.coroutines.KastraxDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

/**
 * kastrax协程运行时提供者
 *
 * 提供全局的kastrax协程运行时实例
 */
object KastraxCoroutineRuntimeProvider {
    /**
     * 当前使用的协程运行时
     */
    private var currentRuntime: KastraxCoroutineRuntime? = null

    /**
     * 获取协程运行时
     *
     * 如果已经设置了运行时，则返回已设置的运行时
     * 否则，使用KastraxCoroutineRuntimeFactory.getRuntime()获取默认运行时
     *
     * @return 协程运行时
     */
    fun getRuntime(): KastraxCoroutineRuntime {
        return currentRuntime ?: KastraxCoroutineRuntimeFactory.getRuntime().also {
            currentRuntime = it
        }
    }

    /**
     * 设置协程运行时
     *
     * @param runtime 协程运行时
     */
    fun setRuntime(runtime: KastraxCoroutineRuntime) {
        currentRuntime = runtime
    }

    /**
     * 重置协程运行时
     *
     * 清除当前设置的运行时，下次调用getRuntime()时将重新获取默认运行时
     */
    fun resetRuntime() {
        currentRuntime = null
    }

    /**
     * 获取IO调度器
     *
     * @return IO调度器
     */
    fun ioDispatcher(): KastraxDispatcher {
        return getRuntime().ioDispatcher()
    }

    /**
     * 获取计算调度器
     *
     * @return 计算调度器
     */
    fun computeDispatcher(): KastraxDispatcher {
        return getRuntime().computeDispatcher()
    }

    /**
     * 获取UI调度器
     *
     * @return UI调度器
     */
    fun uiDispatcher(): KastraxDispatcher {
        return getRuntime().uiDispatcher()
    }

    /**
     * 获取协程作用域
     *
     * @param owner 作用域拥有者
     * @return 协程作用域
     */
    fun getScope(owner: Any): KastraxCoroutineScope {
        return getRuntime().getScope(owner)
    }

    /**
     * 在阻塞上下文中运行协程
     *
     * @param block 要运行的代码块
     * @return 代码块的返回值
     */
    fun <T> runBlocking(block: suspend () -> T): T {
        return getRuntime().runBlocking(block)
    }
}
