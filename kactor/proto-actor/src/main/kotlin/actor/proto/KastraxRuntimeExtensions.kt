package actor.proto

import actor.proto.mailbox.KastraxActorDispatcher
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime

/**
 * 在ActorSystem中使用kastrax协程运行时
 * 
 * @param runtime kastrax协程运行时
 */
fun ActorSystem.useKastraxRuntime(runtime: KastraxCoroutineRuntime) {
    val dispatcher = KastraxActorDispatcher(runtime)
    this.config.dispatcher = dispatcher
}
