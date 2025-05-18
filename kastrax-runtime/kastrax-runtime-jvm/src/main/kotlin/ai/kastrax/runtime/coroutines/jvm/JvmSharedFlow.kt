package ai.kastrax.runtime.coroutines.jvm

import ai.kastrax.runtime.coroutines.KastraxSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * JVM共享流实现
 */
class JvmSharedFlow<T>(private val flow: MutableSharedFlow<T>) : KastraxSharedFlow<T>, KastraxFlow<T> by JvmFlow(flow) {
    override suspend fun emit(value: T) {
        flow.emit(value)
    }
    
    override fun tryEmit(value: T): Boolean {
        return flow.tryEmit(value)
    }
}
