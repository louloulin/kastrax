package ai.kastrax.runtime.coroutines.android

import ai.kastrax.runtime.coroutines.KastraxDeferred
import kotlinx.coroutines.Deferred

/**
 * Android 平台的延迟结果实现
 */
class AndroidDeferred<T>(private val deferred: Deferred<T>) : KastraxDeferred<T> {
    /**
     * 取消延迟结果
     */
    override fun cancel() {
        deferred.cancel()
    }
    
    /**
     * 等待延迟结果完成
     */
    override suspend fun join() {
        deferred.join()
    }
    
    /**
     * 检查延迟结果是否活跃
     *
     * @return 延迟结果是否活跃
     */
    override fun isActive(): Boolean {
        return deferred.isActive
    }
    
    /**
     * 等待并获取结果
     *
     * @return 结果
     */
    override suspend fun await(): T {
        return deferred.await()
    }
}
