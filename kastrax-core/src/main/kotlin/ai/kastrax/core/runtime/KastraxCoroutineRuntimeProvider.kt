package ai.kastrax.core.runtime

import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineScope
import ai.kastrax.runtime.coroutines.KastraxDispatcher

/**
 * kastrax协程运行时提供者
 *
 * 提供全局的kastrax协程运行时实例
 *
 * @deprecated 使用 ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal 替代
 */
@Deprecated(
    message = "使用 ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal 替代",
    replaceWith = ReplaceWith("KastraxCoroutineGlobal", "ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal")
)
object KastraxCoroutineRuntimeProvider {
    /**
     * 获取协程运行时
     *
     * @return 协程运行时
     * @deprecated 使用 KastraxCoroutineGlobal.getRuntime() 替代
     */
    @Deprecated(
        message = "使用 KastraxCoroutineGlobal.getRuntime() 替代",
        replaceWith = ReplaceWith("KastraxCoroutineGlobal.getRuntime()", "ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal")
    )
    fun getRuntime(): KastraxCoroutineRuntime {
        return KastraxCoroutineGlobal.getRuntime()
    }

    /**
     * 设置协程运行时
     *
     * @param runtime 协程运行时
     * @deprecated 使用 KastraxCoroutineGlobal.setRuntime(runtime) 替代
     */
    @Deprecated(
        message = "使用 KastraxCoroutineGlobal.setRuntime(runtime) 替代",
        replaceWith = ReplaceWith("KastraxCoroutineGlobal.setRuntime(runtime)", "ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal")
    )
    fun setRuntime(runtime: KastraxCoroutineRuntime) {
        KastraxCoroutineGlobal.setRuntime(runtime)
    }

    /**
     * 重置协程运行时
     *
     * @deprecated 使用 KastraxCoroutineGlobal.resetRuntime() 替代
     */
    @Deprecated(
        message = "使用 KastraxCoroutineGlobal.resetRuntime() 替代",
        replaceWith = ReplaceWith("KastraxCoroutineGlobal.resetRuntime()", "ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal")
    )
    fun resetRuntime() {
        KastraxCoroutineGlobal.resetRuntime()
    }

    /**
     * 获取IO调度器
     *
     * @return IO调度器
     * @deprecated 使用 KastraxCoroutineGlobal.ioDispatcher() 替代
     */
    @Deprecated(
        message = "使用 KastraxCoroutineGlobal.ioDispatcher() 替代",
        replaceWith = ReplaceWith("KastraxCoroutineGlobal.ioDispatcher()", "ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal")
    )
    fun ioDispatcher(): KastraxDispatcher {
        return KastraxCoroutineGlobal.ioDispatcher()
    }

    /**
     * 获取计算调度器
     *
     * @return 计算调度器
     * @deprecated 使用 KastraxCoroutineGlobal.computeDispatcher() 替代
     */
    @Deprecated(
        message = "使用 KastraxCoroutineGlobal.computeDispatcher() 替代",
        replaceWith = ReplaceWith("KastraxCoroutineGlobal.computeDispatcher()", "ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal")
    )
    fun computeDispatcher(): KastraxDispatcher {
        return KastraxCoroutineGlobal.computeDispatcher()
    }

    /**
     * 获取UI调度器
     *
     * @return UI调度器
     * @deprecated 使用 KastraxCoroutineGlobal.uiDispatcher() 替代
     */
    @Deprecated(
        message = "使用 KastraxCoroutineGlobal.uiDispatcher() 替代",
        replaceWith = ReplaceWith("KastraxCoroutineGlobal.uiDispatcher()", "ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal")
    )
    fun uiDispatcher(): KastraxDispatcher {
        return KastraxCoroutineGlobal.uiDispatcher()
    }

    /**
     * 获取协程作用域
     *
     * @param owner 作用域拥有者
     * @return 协程作用域
     * @deprecated 使用 KastraxCoroutineGlobal.getScope(owner) 替代
     */
    @Deprecated(
        message = "使用 KastraxCoroutineGlobal.getScope(owner) 替代",
        replaceWith = ReplaceWith("KastraxCoroutineGlobal.getScope(owner)", "ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal")
    )
    fun getScope(owner: Any): KastraxCoroutineScope {
        return KastraxCoroutineGlobal.getScope(owner)
    }

    /**
     * 在阻塞上下文中运行协程
     *
     * @param block 要运行的代码块
     * @return 代码块的返回值
     * @deprecated 使用 KastraxCoroutineGlobal.runBlocking(block) 替代
     */
    @Deprecated(
        message = "使用 KastraxCoroutineGlobal.runBlocking(block) 替代",
        replaceWith = ReplaceWith("KastraxCoroutineGlobal.runBlocking(block)", "ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal")
    )
    fun <T> runBlocking(block: suspend () -> T): T {
        return KastraxCoroutineGlobal.runBlocking(block)
    }
}
