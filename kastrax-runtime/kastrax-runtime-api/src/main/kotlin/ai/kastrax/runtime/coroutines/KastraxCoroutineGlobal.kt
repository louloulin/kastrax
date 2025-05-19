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
 * 全局协程运行时
 *
 * 提供全局的协程运行时函数，替代kotlinx.coroutines的函数
 */
object KastraxCoroutineGlobal {
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
     * 在IO调度器上执行代码块
     *
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    suspend fun <T> withIO(block: suspend () -> T): T {
        return KastraxDispatcherFactory.io().withContext(block)
    }

    /**
     * 在计算调度器上执行代码块
     *
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    suspend fun <T> withCompute(block: suspend () -> T): T {
        return KastraxDispatcherFactory.compute().withContext(block)
    }

    /**
     * 在UI调度器上执行代码块
     *
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    suspend fun <T> withUI(block: suspend () -> T): T {
        return KastraxDispatcherFactory.ui().withContext(block)
    }

    /**
     * 在默认调度器上执行代码块
     *
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    suspend fun <T> withDefault(block: suspend () -> T): T {
        return KastraxDispatcherFactory.default().withContext(block)
    }

    /**
     * 在指定任务类型的调度器上执行代码块
     *
     * @param taskType 任务类型
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    suspend fun <T> withTaskType(taskType: KastraxDispatcherFactory.TaskType, block: suspend () -> T): T {
        return KastraxDispatcherFactory.getDispatcher(taskType).withContext(block)
    }

    /**
     * 在协程作用域中启动一个新的协程
     *
     * @param owner 作用域拥有者
     * @param block 要执行的代码块
     * @return 协程作业
     */
    fun launch(owner: Any, block: suspend () -> Unit): KastraxJob {
        return getRuntime().getScope(owner).launch(block)
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
     * 创建一个新的协程作用域
     *
     * @param context 协程上下文
     * @return 协程作用域
     */
    fun createScope(context: CoroutineContext = EmptyCoroutineContext): KastraxCoroutineScope {
        return getRuntime().getScope(context)
    }

    /**
     * 获取IO调度器
     *
     * @return IO调度器
     */
    fun ioDispatcher(): KastraxDispatcher {
        return KastraxDispatcherFactory.io()
    }

    /**
     * 获取计算调度器
     *
     * @return 计算调度器
     */
    fun computeDispatcher(): KastraxDispatcher {
        return KastraxDispatcherFactory.compute()
    }

    /**
     * 获取UI调度器
     *
     * @return UI调度器
     */
    fun uiDispatcher(): KastraxDispatcher {
        return KastraxDispatcherFactory.ui()
    }

    /**
     * 获取默认调度器
     *
     * @return 默认调度器
     */
    fun defaultDispatcher(): KastraxDispatcher {
        return KastraxDispatcherFactory.default()
    }

    /**
     * 获取指定任务类型的调度器
     *
     * @param taskType 任务类型
     * @return 调度器
     */
    fun getDispatcher(taskType: KastraxDispatcherFactory.TaskType): KastraxDispatcher {
        return KastraxDispatcherFactory.getDispatcher(taskType)
    }
}
