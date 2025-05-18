package ai.kastrax.runtime.coroutines

/**
 * kastrax协程作用域抽象
 */
interface KastraxCoroutineScope {
    /**
     * 启动协程
     */
    fun launch(block: suspend () -> Unit): KastraxJob
    
    /**
     * 异步执行并返回结果
     */
    fun <T> async(block: suspend () -> T): KastraxDeferred<T>
    
    /**
     * 取消作用域中的所有协程
     */
    fun cancel()
    
    /**
     * 检查作用域是否活跃
     */
    fun isActive(): Boolean
}
