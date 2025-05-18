package actor.proto

import actor.proto.mailbox.DefaultDispatcher
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory

/**
 * 使用kastrax协程运行时
 *
 * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
 * @return 当前ActorSystem实例
 */
fun ActorSystem.useKastraxRuntime(
    runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()
): ActorSystem {
    // 设置调度器为使用kastrax-runtime的DefaultDispatcher
    this.config.dispatcher = DefaultDispatcher.withKastraxRuntime(runtime)
    return this
}

/**
 * 创建一个使用kastrax协程运行时的Actor
 *
 * @param props Actor属性
 * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
 * @return 新Actor的PID
 */
fun ActorSystem.actorOfWithKastraxRuntime(
    props: Props,
    runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()
): PID {
    // 使用kastrax协程运行时
    val dispatcher = DefaultDispatcher.withKastraxRuntime(runtime)
    val newProps = props.withDispatcher(dispatcher)
    return this.actorOf(newProps)
}

/**
 * 创建一个使用kastrax协程运行时的命名Actor
 *
 * @param props Actor属性
 * @param name Actor名称
 * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
 * @return 新Actor的PID
 */
fun ActorSystem.actorOfWithKastraxRuntime(
    props: Props,
    name: String,
    runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()
): PID {
    // 使用kastrax协程运行时
    val dispatcher = DefaultDispatcher.withKastraxRuntime(runtime)
    val newProps = props.withDispatcher(dispatcher)
    return this.actorOf(newProps, name)
}
