package ai.kastrax.runtime.coroutines.idea

import ai.kastrax.runtime.coroutines.KastraxSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * IntelliJ IDEA共享流实现
 */
class IdeaSharedFlow<T>(private val flow: MutableSharedFlow<T>) : KastraxSharedFlow<T>, KastraxFlow<T> by IdeaFlow(flow) {
    override suspend fun emit(value: T) {
        flow.emit(value)
    }
    
    override fun tryEmit(value: T): Boolean {
        return flow.tryEmit(value)
    }
}
