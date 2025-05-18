package actor.proto

import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntime
import ai.kastrax.runtime.coroutines.KastraxCoroutineRuntimeFactory

/**
 * 使用kastrax协程运行时发送消息
 *
 * @param target 目标Actor的PID
 * @param message 消息
 * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
 */
fun RootContext.sendWithKastraxRuntime(
    target: PID,
    message: Any,
    runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()
) {
    when (senderMiddleware) {
        null -> {
            val process = actorSystem.processRegistry().get(target)
            process.sendUserMessage(target, message)
        }
        else -> {
            val envelope = when (message) {
                is MessageEnvelope -> message
                else -> MessageEnvelope(message, null, null)
            }
            runtime.runBlocking { senderMiddleware!!.invoke(this@sendWithKastraxRuntime, target, envelope) }
        }
    }
}

/**
 * 使用kastrax协程运行时发送请求
 *
 * @param target 目标Actor的PID
 * @param message 消息
 * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
 */
fun RootContext.requestWithKastraxRuntime(
    target: PID,
    message: Any,
    runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()
) {
    val envelope = MessageEnvelope(message, self, null)
    sendWithKastraxRuntime(target, envelope, runtime)
}

/**
 * 使用kastrax协程运行时异步请求并等待响应
 *
 * @param target 目标Actor的PID
 * @param message 消息
 * @param timeout 超时时间
 * @param runtime 协程运行时，默认使用KastraxCoroutineRuntimeFactory.getRuntime()
 * @return 响应结果
 */
suspend fun <T> RootContext.requestAwaitWithKastraxRuntime(
    target: PID,
    message: Any,
    timeout: java.time.Duration = java.time.Duration.ofSeconds(5),
    runtime: KastraxCoroutineRuntime = KastraxCoroutineRuntimeFactory.getRuntime()
): T {
    val future = FutureWithKastraxRuntime<T>(actorSystem, timeout, runtime)
    val messageEnvelope = MessageEnvelope(message, future.pid, null)
    sendWithKastraxRuntime(target, messageEnvelope, runtime)
    return future.result()
}
