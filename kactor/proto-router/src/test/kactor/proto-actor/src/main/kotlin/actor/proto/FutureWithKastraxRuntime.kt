package actor.proto

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * 使用kastrax协程运行时的Future实现
 *
 * @param system Actor系统
 * @param timeout 超时时间
 * @param runtime 协程运行时
 */
class FutureWithKastraxRuntime<T>(
    system: ActorSystem,
    timeout: Duration = Duration.ofSeconds(5),
    private val runtime: KastraxCoroutineRuntime
) : Future<T>(system, timeout) {
    
    /**
     * 获取结果
     *
     * @return 结果
     */
    override suspend fun result(): T {
        return runtime.runBlocking {
            val completableFuture = CompletableFuture<T>()
            
            // 设置完成回调
            this@FutureWithKastraxRuntime.completionCallback = { result, error ->
                if (error != null) {
                    completableFuture.completeExceptionally(error)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    completableFuture.complete(result as T)
                }
            }
            
            // 等待结果或超时
            try {
                completableFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                // 超时或其他异常，取消Future
                this@FutureWithKastraxRuntime.cancel()
                throw e
            }
        }
    }
    
    /**
     * 继续执行
     *
     * @param action 执行函数
     */
    override fun continueWith(action: (T?, Throwable?) -> Unit) {
        val scope = runtime.getScope(this)
        scope.launch {
            try {
                val result = result()
                action(result, null)
            } catch (e: Throwable) {
                action(null, e)
            }
        }
    }
}
