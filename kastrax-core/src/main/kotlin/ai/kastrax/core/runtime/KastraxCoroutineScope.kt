package ai.kastrax.core.runtime

import ai.kastrax.runtime.coroutines.KastraxCoroutineScope
import ai.kastrax.runtime.coroutines.KastraxDeferred
import ai.kastrax.runtime.coroutines.KastraxJob

/**
 * kastrax协程作用域
 *
 * 提供便捷的协程作用域方法
 *
 * @property owner 作用域拥有者
 */
class KastraxScope(private val owner: Any) {
    /**
     * 获取协程作用域
     */
    private val scope: KastraxCoroutineScope
        get() = KastraxCoroutineRuntimeProvider.getScope(owner)

    /**
     * 启动协程
     *
     * @param block 要执行的代码块
     * @return 协程作业
     */
    fun launch(block: suspend () -> Unit): KastraxJob {
        return scope.launch(block)
    }

    /**
     * 异步执行并返回结果
     *
     * @param block 要执行的代码块
     * @return 延迟结果
     */
    fun <T> async(block: suspend () -> T): KastraxDeferred<T> {
        return scope.async(block)
    }

    /**
     * 取消作用域中的所有协程
     */
    fun cancel() {
        scope.cancel()
    }

    /**
     * 检查作用域是否活跃
     *
     * @return 作用域是否活跃
     */
    fun isActive(): Boolean {
        return scope.isActive()
    }

    companion object {
        /**
         * 获取协程作用域
         *
         * @param owner 作用域拥有者
         * @return 协程作用域
         */
        fun of(owner: Any): KastraxScope {
            return KastraxScope(owner)
        }
    }
}
