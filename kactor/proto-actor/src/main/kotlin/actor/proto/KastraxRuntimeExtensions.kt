package actor.proto

import actor.proto.mailbox.DefaultDispatcher
import actor.proto.mailbox.Dispatchers
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory

/**
 * 在ActorSystem中使用kastrax协程运行时
 *
 * @param runtime kastrax协程运行时
 * @return 当前ActorSystem实例
 */
fun ActorSystem.useKastraxRuntime(
    runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()
): ActorSystem {
    // 设置默认调度器为使用kastrax-runtime的DefaultDispatcher
    Dispatchers.DEFAULT_DISPATCHER = DefaultDispatcher.withKastraxRuntime(runtime)
    return this
}
