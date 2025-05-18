package ai.kastrax.runtime.coroutines.jvm.kactor

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import actor.proto.mailbox.Dispatcher
import actor.proto.mailbox.Mailbox

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
