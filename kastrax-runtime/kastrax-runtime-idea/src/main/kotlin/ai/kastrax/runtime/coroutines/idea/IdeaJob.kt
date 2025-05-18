package ai.kastrax.runtime.coroutines.idea

import ai.kastrax.runtime.coroutines.KastraxJob
import kotlinx.coroutines.Job

/**
 * IntelliJ IDEA协程作业实现
 */
class IdeaJob(private val job: Job) : KastraxJob {
    override fun cancel() {
        job.cancel()
    }
    
    override suspend fun join() {
        job.join()
    }
    
    override fun isActive(): Boolean {
        return job.isActive
    }
}
