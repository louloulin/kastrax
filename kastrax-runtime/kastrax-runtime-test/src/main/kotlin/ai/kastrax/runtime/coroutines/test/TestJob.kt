package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.KastraxJob
import kotlinx.coroutines.Job

/**
 * 测试协程作业实现
 */
class TestJob(private val job: kotlinx.coroutines.Job) : KastraxJob {
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
