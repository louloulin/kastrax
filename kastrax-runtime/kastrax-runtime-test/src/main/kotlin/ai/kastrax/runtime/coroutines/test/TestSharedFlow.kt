package ai.kastrax.runtime.coroutines.test

import ai.kastrax.runtime.coroutines.KastraxSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 测试共享流实现
 */
class TestSharedFlow<T>(private val flow: MutableSharedFlow<T>) : KastraxSharedFlow<T>, KastraxFlow<T> by TestFlow(flow) {
    override suspend fun emit(value: T) {
        flow.emit(value)
    }
    
    override fun tryEmit(value: T): Boolean {
        return flow.tryEmit(value)
    }
}
