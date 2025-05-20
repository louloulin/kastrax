package actor.proto.mailbox

import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal

/**
 * 同步调度器，使用 KastraxCoroutineGlobal.runBlocking 而不是 kotlinx.coroutines.runBlocking
 */
class SynchronousDispatcher(override var throughput: Int = 300) : Dispatcher {
    override fun schedule(mailbox:Mailbox) = KastraxCoroutineGlobal.runBlocking { mailbox.run() }
}
