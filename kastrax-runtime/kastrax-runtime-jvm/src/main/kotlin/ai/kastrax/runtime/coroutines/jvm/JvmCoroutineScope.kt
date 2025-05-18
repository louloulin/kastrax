package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.*
import kotlinx.coroutines.*

/**
 * JVM协程作用域实现
 */
class JvmCoroutineScope(private val scope: CoroutineScope) : KastraxCoroutineScope {
    override fun launch(block: suspend () -> Unit): KastraxJob {
        return JvmJob(scope.launch { block() })
    }
    
    override fun <T> async(block: suspend () -> T): KastraxDeferred<T> {
        return JvmDeferred(scope.async { block() })
    }
    
    override fun cancel() {
        scope.cancel()
    }
    
    override fun isActive(): Boolean {
        return scope.isActive
    }
}
