package ai.kastrax.runtime.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 在IO调度器上执行代码块
 *
 * 替代kotlinx.coroutines.withContext(Dispatchers.IO)
 *
 * @param block 要执行的代码块
 * @return 代码块的返回值
 */
suspend fun <T> withIO(block: suspend () -> T): T {
    return KastraxCoroutineGlobal.withIO(block)
}

/**
 * 在计算调度器上执行代码块
 *
 * 替代kotlinx.coroutines.withContext(Dispatchers.Default)
 *
 * @param block 要执行的代码块
 * @return 代码块的返回值
 */
suspend fun <T> withCompute(block: suspend () -> T): T {
    return KastraxCoroutineGlobal.withCompute(block)
}

/**
 * 在UI调度器上执行代码块
 *
 * 替代kotlinx.coroutines.withContext(Dispatchers.Main)
 *
 * @param block 要执行的代码块
 * @return 代码块的返回值
 */
suspend fun <T> withUI(block: suspend () -> T): T {
    return KastraxCoroutineGlobal.withUI(block)
}

/**
 * 在协程作用域中启动一个新的协程
 *
 * 替代kotlinx.coroutines.launch
 *
 * @param block 要执行的代码块
 * @return 协程作业
 */
fun KastraxCoroutineScope.launchKastrax(block: suspend () -> Unit): KastraxJob {
    return this.launch(block)
}

/**
 * 在阻塞上下文中运行协程
 *
 * 替代kotlinx.coroutines.runBlocking
 *
 * @param block 要运行的代码块
 * @return 代码块的返回值
 */
fun <T> runBlockingKastrax(block: suspend () -> T): T {
    return KastraxCoroutineGlobal.runBlocking(block)
}

/**
 * 获取协程作用域
 *
 * @param owner 作用域拥有者
 * @return 协程作用域
 */
fun getScope(owner: Any): KastraxCoroutineScope {
    return KastraxCoroutineGlobal.getScope(owner)
}

/**
 * 在协程作用域中启动一个新的协程
 *
 * 替代kotlinx.coroutines.GlobalScope.launch
 *
 * @param owner 作用域拥有者
 * @param block 要执行的代码块
 * @return 协程作业
 */
fun launch(owner: Any, block: suspend () -> Unit): KastraxJob {
    return KastraxCoroutineGlobal.getScope(owner).launch(block)
}

/**
 * 创建一个新的协程作用域
 *
 * 替代CoroutineScope(context)
 *
 * @param context 协程上下文
 * @return 协程作用域
 */
fun createScope(context: CoroutineContext = EmptyCoroutineContext): KastraxCoroutineScope {
    return KastraxCoroutineGlobal.createScope(context)
}

/**
 * 获取IO调度器
 *
 * 替代Dispatchers.IO
 *
 * @return IO调度器
 */
val IO: KastraxDispatcher
    get() = KastraxCoroutineGlobal.ioDispatcher()

/**
 * 获取计算调度器
 *
 * 替代Dispatchers.Default
 *
 * @return 计算调度器
 */
val Default: KastraxDispatcher
    get() = KastraxCoroutineGlobal.computeDispatcher()

/**
 * 获取UI调度器
 *
 * 替代Dispatchers.Main
 *
 * @return UI调度器
 */
val Main: KastraxDispatcher
    get() = KastraxCoroutineGlobal.uiDispatcher()
