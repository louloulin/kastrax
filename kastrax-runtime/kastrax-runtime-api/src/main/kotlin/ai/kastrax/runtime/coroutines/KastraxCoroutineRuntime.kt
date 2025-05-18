package ai.kastrax.runtime.coroutines

/**
 * kastrax协程运行时抽象
 * 提供跨平台的协程支持
 */
interface KastraxCoroutineRuntime {
    /**
     * 获取适合当前平台的协程作用域
     */
    fun getScope(owner: Any): KastraxCoroutineScope
    
    /**
     * 获取IO调度器
     */
    fun ioDispatcher(): KastraxDispatcher
    
    /**
     * 获取计算调度器
     */
    fun computeDispatcher(): KastraxDispatcher
    
    /**
     * 获取UI调度器
     */
    fun uiDispatcher(): KastraxDispatcher
    
    /**
     * 执行阻塞操作
     */
    fun <T> runBlocking(block: suspend () -> T): T
    
    /**
     * 创建可取消的作用域
     */
    fun createCancellableScope(owner: Any): KastraxCoroutineScope
    
    /**
     * 创建流
     */
    fun <T> flow(block: suspend FlowCollector<T>.() -> Unit): KastraxFlow<T>
    
    /**
     * 创建共享流
     */
    fun <T> sharedFlow(replay: Int = 0, extraBufferCapacity: Int = 0): KastraxSharedFlow<T>
}
