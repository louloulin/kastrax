package actor.proto.mailbox

/**
 * 调度器对象，提供默认调度器和同步调度器
 */
object Dispatchers {
    /**
     * 默认调度器，可以被替换为使用kastrax-runtime的调度器
     */
    var DEFAULT_DISPATCHER: Dispatcher = DefaultDispatcher()

    /**
     * 同步调度器，用于同步执行
     */
    val SYNCHRONOUS_DISPATCHER: Dispatcher = SynchronousDispatcher()
}

