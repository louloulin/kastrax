package actor.proto.mailbox

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory

/**
 * 调度器对象，提供默认调度器和同步调度器
 */
object Dispatchers {
    /**
     * 默认调度器，默认使用kastrax-runtime
     */
    var DEFAULT_DISPATCHER: Dispatcher = DefaultDispatcher.withKastraxRuntime(KastraxCoroutineRuntimeFactory.getRuntime())

    /**
     * 同步调度器，用于同步执行
     */
    val SYNCHRONOUS_DISPATCHER: Dispatcher = SynchronousDispatcher()
}

