package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.KastraxJob
import kotlinx.coroutines.Job

/**
 * JVM协程作业实现
 */
class JvmJob(private val job: Job) : KastraxJob {
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
