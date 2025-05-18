package ai.kastrax.runtime.coroutines.compat

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory
import kotlinx.coroutines.*

/**
 * 兼容性工具类
 * 提供与kotlinx.coroutines API兼容的方法
 */
object CoroutineCompat {
    private val runtime = KastraxCoroutineRuntimeFactory.getRuntime()
    
    /**
     * 兼容性launch方法
     */
    fun CoroutineScope.launchCompat(block: suspend CoroutineScope.() -> Unit): Job {
        val kastraxScope = runtime.getScope(this)
        return kastraxScope.launch { block(this@launchCompat) } as Job
    }
    
    /**
     * 兼容性withContext方法
     */
    suspend fun <T> withContextCompat(
        dispatcher: CoroutineDispatcher,
        block: suspend CoroutineScope.() -> T
    ): T {
        val kastraxDispatcher = when (dispatcher) {
            Dispatchers.IO -> runtime.ioDispatcher()
            Dispatchers.Default -> runtime.computeDispatcher()
            Dispatchers.Main -> runtime.uiDispatcher()
            else -> runtime.computeDispatcher()
        }
        
        return kastraxDispatcher.withContext { block(CoroutineScope(coroutineContext)) }
    }
    
    /**
     * 兼容性async方法
     */
    fun <T> CoroutineScope.asyncCompat(block: suspend CoroutineScope.() -> T): Deferred<T> {
        val kastraxScope = runtime.getScope(this)
        return kastraxScope.async { block(this@asyncCompat) } as Deferred<T>
    }
    
    /**
     * 兼容性runBlocking方法
     */
    fun <T> runBlockingCompat(block: suspend CoroutineScope.() -> T): T {
        return runtime.runBlocking {
            block(CoroutineScope(coroutineContext))
        }
    }
}
