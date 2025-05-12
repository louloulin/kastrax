package ai.kastrax.codebase.indexing

import java.nio.file.Path

/**
 * 索引处理器接口
 */
interface IndexProcessor {
    /**
     * 处理索引任务
     *
     * @param task 索引任务
     */
    suspend fun processTask(task: IndexTask)
}
