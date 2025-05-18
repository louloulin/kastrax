package actor.proto.mailbox

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime

/**
 * 使用kastrax协程运行时的kactor调度器实现
 */
class KastraxActorDispatcher(private val runtime: KastraxCoroutineRuntime) : Dispatcher {
    override var throughput: Int = 300
    
    override fun schedule(mailbox: Mailbox) {
        val scope = runtime.getScope(this)
        scope.launch {
            mailbox.run()
        }
    }
}
