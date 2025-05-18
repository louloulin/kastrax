package ai.kastrax.runtime.coroutines

/**
 * kastrax流收集器抽象
 */
interface FlowCollector<in T> {
    /**
     * 收集值
     */
    suspend fun emit(value: T)
}
