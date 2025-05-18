package ai.kastrax.runtime.coroutines.jvm.kactor

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import actor.proto.ActorSystem

/**
 * 在ActorSystem中使用kastrax协程运行时
 */
fun ActorSystem.useKastraxRuntime(runtime: KastraxCoroutineRuntime) {
    val dispatcher = KastraxActorDispatcher(runtime)
    this.config.dispatcher = dispatcher
}
