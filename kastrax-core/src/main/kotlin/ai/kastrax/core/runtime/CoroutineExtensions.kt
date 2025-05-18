package ai.kastrax.core.runtime

import ai.kastrax.runtime.coroutines.KastraxJob

/**
 * 在IO调度器上执行代码块
 *
 * @param block 要执行的代码块
 * @return 代码块的返回值
 */
suspend fun <T> withIO(block: suspend () -> T): T {
    return KastraxCoroutineRuntimeProvider.ioDispatcher().withContext(block)
}

/**
 * 在计算调度器上执行代码块
 *
 * @param block 要执行的代码块
 * @return 代码块的返回值
 */
suspend fun <T> withCompute(block: suspend () -> T): T {
    return KastraxCoroutineRuntimeProvider.computeDispatcher().withContext(block)
}

/**
 * 在UI调度器上执行代码块
 *
 * @param block 要执行的代码块
 * @return 代码块的返回值
 */
suspend fun <T> withUI(block: suspend () -> T): T {
    return KastraxCoroutineRuntimeProvider.uiDispatcher().withContext(block)
}

/**
 * 在协程作用域中启动一个新的协程
 *
 * @param owner 作用域拥有者
 * @param block 要执行的代码块
 * @return 协程作业
 */
fun launchCoroutine(owner: Any, block: suspend () -> Unit): KastraxJob {
    return KastraxCoroutineRuntimeProvider.getScope(owner).launch {
        block()
    }
}

/**
 * 在阻塞上下文中运行协程
 *
 * @param block 要运行的代码块
 * @return 代码块的返回值
 */
fun <T> runBlockingCoroutine(block: suspend () -> T): T {
    return KastraxCoroutineRuntimeProvider.runBlocking(block)
}
