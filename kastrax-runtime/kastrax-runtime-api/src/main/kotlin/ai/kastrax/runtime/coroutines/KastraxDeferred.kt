package ai.kastrax.runtime.coroutines

/**
 * kastrax延迟结果抽象
 */
interface KastraxDeferred<T> : KastraxJob {
    /**
     * 等待并获取结果
     */
    suspend fun await(): T
}
