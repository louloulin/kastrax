package ai.kastrax.runtime.coroutines.idea

import ai.kastrax.runtime.coroutines.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.coroutines.runBlockingCancellable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * IntelliJ IDEA插件环境的协程运行时实现
 *
 * 该实现专门为 IntelliJ IDEA 插件环境设计，充分利用了 IntelliJ 平台的协程支持。
 */
class IdeaCoroutineRuntime : KastraxCoroutineRuntime {
    /**
     * 获取适合当前平台的协程作用域
     *
     * @param owner 作用域拥有者
     * @return 协程作用域
     */
    override fun getScope(owner: Any): KastraxCoroutineScope {
        val scope = when (owner) {
            is Project -> {
                // 获取项目级协程作用域
                try {
                    // 尝试使用新的 service() 扩展函数
                    owner.service<CoroutineScope>()
                } catch (e: Exception) {
                    // 如果失败，回退到旧的方式
                    owner.getService(CoroutineScope::class.java) ?: createProjectScope(owner)
                }
            }
            is com.intellij.openapi.application.Application -> {
                // 获取应用级协程作用域
                try {
                    // 尝试使用新的 service() 扩展函数
                    owner.service<CoroutineScope>()
                } catch (e: Exception) {
                    // 如果失败，回退到旧的方式
                    owner.getService(CoroutineScope::class.java) ?: createApplicationScope()
                }
            }
            is Disposable -> {
                // 创建可释放的作用域
                createDisposableScope(owner)
            }
            else -> {
                // 创建默认作用域
                createDefaultScope()
            }
        }

        return IdeaCoroutineScope(scope)
    }

    /**
     * 获取适合当前平台的协程作用域，使用指定的协程上下文
     *
     * @param context 协程上下文
     * @return 协程作用域
     */
    override fun getScope(context: kotlin.coroutines.CoroutineContext): KastraxCoroutineScope {
        // 确保上下文中有 SupervisorJob，以防止异常传播
        val enhancedContext = context + SupervisorJob(context[Job])
        val scope = CoroutineScope(enhancedContext)
        return IdeaCoroutineScope(scope)
    }

    /**
     * 获取IO调度器
     *
     * @return IO调度器
     */
    override fun ioDispatcher(): KastraxDispatcher {
        return IdeaDispatcher(Dispatchers.IO)
    }

    /**
     * 获取计算调度器
     *
     * @return 计算调度器
     */
    override fun computeDispatcher(): KastraxDispatcher {
        return IdeaDispatcher(Dispatchers.Default)
    }

    /**
     * 获取UI调度器
     *
     * @return UI调度器
     */
    override fun uiDispatcher(): KastraxDispatcher {
        // 使用IntelliJ平台的EDT调度器
        return IdeaDispatcher(getEdtDispatcher())
    }

    /**
     * 执行阻塞操作
     *
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    override fun <T> runBlocking(block: suspend () -> T): T {
        // 使用IntelliJ平台的runBlockingCancellable
        return runBlockingCancellable { block() }
    }

    /**
     * 创建可取消的作用域
     *
     * @param owner 作用域拥有者
     * @return 协程作用域
     */
    override fun createCancellableScope(owner: Any): KastraxCoroutineScope {
        return getScope(owner)
    }

    /**
     * 创建流
     *
     * @param block 流收集器代码块
     * @return 流
     */
    override fun <T> flow(block: suspend FlowCollector<T>.() -> Unit): KastraxFlow<T> {
        return IdeaFlow(kotlinx.coroutines.flow.flow {
            val collector = object : FlowCollector<T> {
                override suspend fun emit(value: T) {
                    emit(value)
                }
            }
            block(collector)
        })
    }

    /**
     * 创建共享流
     *
     * @param replay 重放缓冲区大小
     * @param extraBufferCapacity 额外缓冲区大小
     * @return 共享流
     */
    override fun <T> sharedFlow(replay: Int, extraBufferCapacity: Int): KastraxSharedFlow<T> {
        val flow = MutableSharedFlow<T>(replay = replay, extraBufferCapacity = extraBufferCapacity)
        return IdeaSharedFlow(flow)
    }

    /**
     * 创建项目作用域
     *
     * @param project 项目
     * @return 协程作用域
     */
    private fun createProjectScope(project: Any): CoroutineScope {
        val job = SupervisorJob()
        val dispatcher = Dispatchers.Default
        val scope = CoroutineScope(job + dispatcher)

        // 如果是Project，在项目关闭时取消协程
        if (project is Project) {
            project.service<Disposable>().register {
                job.cancel()
            }
        }

        return scope
    }

    /**
     * 创建应用作用域
     *
     * @return 协程作用域
     */
    private fun createApplicationScope(): CoroutineScope {
        val job = SupervisorJob()
        val dispatcher = Dispatchers.Default
        return CoroutineScope(job + dispatcher)
    }

    /**
     * 创建可释放的作用域
     *
     * @param disposable 可释放对象
     * @return 协程作用域
     */
    private fun createDisposableScope(disposable: Disposable): CoroutineScope {
        val job = SupervisorJob()
        val dispatcher = Dispatchers.Default
        val scope = CoroutineScope(job + dispatcher)

        // 在Disposable释放时取消协程
        disposable.register {
            job.cancel()
        }

        return scope
    }

    /**
     * 创建默认作用域
     *
     * @return 协程作用域
     */
    private fun createDefaultScope(): CoroutineScope {
        val job = SupervisorJob()
        val dispatcher = Dispatchers.Default
        return CoroutineScope(job + dispatcher)
    }

    /**
     * 获取EDT调度器
     *
     * @return EDT调度器
     */
    private fun getEdtDispatcher(): CoroutineDispatcher {
        // 尝试使用新的EDT调度器
        return try {
            // 使用新的EDT调度器，如果可用
            EDT
        } catch (e: Throwable) {
            // 如果不可用，回退到旧的方式
            val app = ApplicationManager.getApplication()
            app.getCoroutineScope().coroutineContext[CoroutineDispatcher] ?: Dispatchers.Default
        }
    }
}
