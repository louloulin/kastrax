package ai.kastrax.runtime.coroutines

/**
 * kastrax流抽象
 */
interface KastraxFlow<T> {
    /**
     * 收集流
     */
    suspend fun collect(collector: suspend (T) -> Unit)
    
    /**
     * 映射流
     */
    fun <R> map(transform: suspend (T) -> R): KastraxFlow<R>
    
    /**
     * 过滤流
     */
    fun filter(predicate: suspend (T) -> Boolean): KastraxFlow<T>
    
    /**
     * 捕获异常
     */
    fun catch(action: suspend (Throwable) -> Unit): KastraxFlow<T>
}
