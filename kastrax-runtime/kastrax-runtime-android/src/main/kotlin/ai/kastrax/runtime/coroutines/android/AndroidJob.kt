package ai.kastrax.runtime.coroutines.android

import ai.kastrax.runtime.coroutines.KastraxJob
import kotlinx.coroutines.Job

/**
 * Android 平台的协程作业实现
 */
class AndroidJob(private val job: Job) : KastraxJob {
    /**
     * 取消作业
     */
    override fun cancel() {
        job.cancel()
    }
    
    /**
     * 等待作业完成
     */
    override suspend fun join() {
        job.join()
    }
    
    /**
     * 检查作业是否活跃
     *
     * @return 作业是否活跃
     */
    override fun isActive(): Boolean {
        return job.isActive
    }
}
