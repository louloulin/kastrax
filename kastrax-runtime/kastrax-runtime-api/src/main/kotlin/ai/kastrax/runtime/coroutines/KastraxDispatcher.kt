package ai.kastrax.runtime.coroutines

/**
 * kastrax调度器抽象
 */
interface KastraxDispatcher {
    /**
     * 在此调度器上执行代码块
     */
    suspend fun <T> withContext(block: suspend () -> T): T
}
