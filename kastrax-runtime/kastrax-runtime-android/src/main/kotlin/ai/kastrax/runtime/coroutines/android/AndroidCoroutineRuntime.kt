package ai.kastrax.runtime.coroutines.android

import ai.kastrax.runtime.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

/**
 * Android 平台的协程运行时实现
 *
 * 专门为 Android 平台设计，充分利用 Android 平台的协程支持。
 */
class AndroidCoroutineRuntime : KastraxCoroutineRuntime {
    /**
     * 获取适合当前平台的协程作用域
     *
     * @param owner 作用域拥有者
     * @return 协程作用域
     */
    override fun getScope(owner: Any): KastraxCoroutineScope {
        val scope = when (owner) {
            // 如果 owner 是 LifecycleOwner，使用 lifecycleScope
            is androidx.lifecycle.LifecycleOwner -> {
                androidx.lifecycle.lifecycleScope(owner)
            }
            // 如果 owner 是 ViewModel，使用 viewModelScope
            is androidx.lifecycle.ViewModel -> {
                androidx.lifecycle.viewModelScope(owner)
            }
            // 如果 owner 是 Lifecycle，创建与生命周期关联的作用域
            is androidx.lifecycle.Lifecycle -> {
                createLifecycleScope(owner)
            }
            // 如果 owner 是可释放的，创建可释放的作用域
            is androidx.lifecycle.DefaultLifecycleObserver -> {
                createLifecycleObserverScope(owner)
            }
            // 其他情况，创建默认作用域
            else -> {
                createDefaultScope()
            }
        }
        
        return AndroidCoroutineScope(scope)
    }
    
    /**
     * 获取适合当前平台的协程作用域，使用指定的协程上下文
     *
     * @param context 协程上下文
     * @return 协程作用域
     */
    override fun getScope(context: CoroutineContext): KastraxCoroutineScope {
        // 确保上下文中有 SupervisorJob，以防止异常传播
        val enhancedContext = context + SupervisorJob(context[Job])
        val scope = CoroutineScope(enhancedContext)
        return AndroidCoroutineScope(scope)
    }
    
    /**
     * 获取 IO 调度器
     *
     * @return IO 调度器
     */
    override fun ioDispatcher(): KastraxDispatcher {
        return AndroidDispatcher(Dispatchers.IO)
    }
    
    /**
     * 获取计算调度器
     *
     * @return 计算调度器
     */
    override fun computeDispatcher(): KastraxDispatcher {
        return AndroidDispatcher(Dispatchers.Default)
    }
    
    /**
     * 获取 UI 调度器
     *
     * @return UI 调度器
     */
    override fun uiDispatcher(): KastraxDispatcher {
        return AndroidDispatcher(Dispatchers.Main)
    }
    
    /**
     * 执行阻塞操作
     *
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    override fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
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
        return AndroidFlow(kotlinx.coroutines.flow.flow { 
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
        return AndroidSharedFlow(flow)
    }
    
    /**
     * 创建与生命周期关联的作用域
     *
     * @param lifecycle 生命周期
     * @return 协程作用域
     */
    private fun createLifecycleScope(lifecycle: androidx.lifecycle.Lifecycle): CoroutineScope {
        val job = SupervisorJob()
        val dispatcher = Dispatchers.Main.immediate
        val scope = CoroutineScope(job + dispatcher)
        
        // 在生命周期结束时取消协程
        lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                job.cancel()
                lifecycle.removeObserver(this)
            }
        })
        
        return scope
    }
    
    /**
     * 创建与生命周期观察者关联的作用域
     *
     * @param observer 生命周期观察者
     * @return 协程作用域
     */
    private fun createLifecycleObserverScope(observer: androidx.lifecycle.DefaultLifecycleObserver): CoroutineScope {
        val job = SupervisorJob()
        val dispatcher = Dispatchers.Main.immediate
        return CoroutineScope(job + dispatcher)
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
}
