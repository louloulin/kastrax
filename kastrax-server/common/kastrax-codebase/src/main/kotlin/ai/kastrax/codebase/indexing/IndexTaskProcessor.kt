package ai.kastrax.codebase.indexing

/**
 * 索引任务处理器接口
 *
 * 定义索引任务处理器的通用接口
 */
interface IndexTaskProcessor {
    /**
     * 处理索引任务
     *
     * @param task 索引任务
     * @return 处理结果
     */
    suspend fun process(task: IndexTask): IndexTaskResult
}
