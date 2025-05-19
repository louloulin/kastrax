package ai.kastrax.core.runtime

import ai.kastrax.runtime.coroutines.*

/**
 * 在IO调度器上执行代码块
 *
 * @param block 要执行的代码块
 * @return 代码块的返回值
 *
 * @deprecated 使用 ai.kastrax.runtime.coroutines.withIO 替代
 */
@Deprecated(
    message = "使用 ai.kastrax.runtime.coroutines.withIO 替代",
    replaceWith = ReplaceWith("withIO(block)", "ai.kastrax.runtime.coroutines.withIO")
)
suspend fun <T> withIO(block: suspend () -> T): T {
    return ai.kastrax.runtime.coroutines.withIO(block)
}

/**
 * 在计算调度器上执行代码块
 *
 * @param block 要执行的代码块
 * @return 代码块的返回值
 *
 * @deprecated 使用 ai.kastrax.runtime.coroutines.withCompute 替代
 */
@Deprecated(
    message = "使用 ai.kastrax.runtime.coroutines.withCompute 替代",
    replaceWith = ReplaceWith("withCompute(block)", "ai.kastrax.runtime.coroutines.withCompute")
)
suspend fun <T> withCompute(block: suspend () -> T): T {
    return ai.kastrax.runtime.coroutines.withCompute(block)
}

/**
 * 在UI调度器上执行代码块
 *
 * @param block 要执行的代码块
 * @return 代码块的返回值
 *
 * @deprecated 使用 ai.kastrax.runtime.coroutines.withUI 替代
 */
@Deprecated(
    message = "使用 ai.kastrax.runtime.coroutines.withUI 替代",
    replaceWith = ReplaceWith("withUI(block)", "ai.kastrax.runtime.coroutines.withUI")
)
suspend fun <T> withUI(block: suspend () -> T): T {
    return ai.kastrax.runtime.coroutines.withUI(block)
}

/**
 * 在协程作用域中启动一个新的协程
 *
 * @param owner 作用域拥有者
 * @param block 要执行的代码块
 * @return 协程作业
 *
 * @deprecated 使用 ai.kastrax.runtime.coroutines.launch 替代
 */
@Deprecated(
    message = "使用 ai.kastrax.runtime.coroutines.launch 替代",
    replaceWith = ReplaceWith("launch(owner, block)", "ai.kastrax.runtime.coroutines.launch")
)
fun launchCoroutine(owner: Any, block: suspend () -> Unit): KastraxJob {
    return ai.kastrax.runtime.coroutines.launch(owner, block)
}

/**
 * 在阻塞上下文中运行协程
 *
 * @param block 要运行的代码块
 * @return 代码块的返回值
 *
 * @deprecated 使用 ai.kastrax.runtime.coroutines.runBlockingKastrax 替代
 */
@Deprecated(
    message = "使用 ai.kastrax.runtime.coroutines.runBlockingKastrax 替代",
    replaceWith = ReplaceWith("runBlockingKastrax(block)", "ai.kastrax.runtime.coroutines.runBlockingKastrax")
)
fun <T> runBlockingCoroutine(block: suspend () -> T): T {
    return ai.kastrax.runtime.coroutines.runBlockingKastrax(block)
}
