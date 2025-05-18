package ai.kastrax.runtime.coroutines

/**
 * kastrax共享流抽象
 */
interface KastraxSharedFlow<T> : KastraxFlow<T> {
    /**
     * 发射值
     */
    suspend fun emit(value: T)
    
    /**
     * 尝试发射值
     */
    fun tryEmit(value: T): Boolean
}
