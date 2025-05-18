package ai.kastrax.runtime.coroutines.idea

import ai.kastrax.runtime.coroutines.*
import kotlinx.coroutines.*

/**
 * IntelliJ IDEA协程作用域实现
 */
class IdeaCoroutineScope(private val scope: CoroutineScope) : KastraxCoroutineScope {
    override fun launch(block: suspend () -> Unit): KastraxJob {
        return IdeaJob(scope.launch { block() })
    }
    
    override fun <T> async(block: suspend () -> T): KastraxDeferred<T> {
        return IdeaDeferred(scope.async { block() })
    }
    
    override fun cancel() {
        scope.cancel()
    }
    
    override fun isActive(): Boolean {
        return scope.isActive
    }
}
