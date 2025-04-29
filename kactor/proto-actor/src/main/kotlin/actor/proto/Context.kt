package actor.proto

import actor.proto.extensions.ContextExtension
import actor.proto.extensions.ContextExtensionID
import actor.proto.logging.Logger
import java.time.Duration

/**
 * Context 是 Actor 的上下文接口
 * 提供了 Actor 与其环境交互所需的方法
 */

interface Context : SpawnerContext, ExtensionContext {
    /**
     * Get the logger for this context
     * @return The logger
     */
    val logger: Logger
    val parent: PID?
    override val self: PID
    val sender: PID?
    override val actor: Actor
    val children: Set<PID>
    val message: Any
    val headers: MessageHeader?

    fun stash()
    fun spawnChild(props: Props): PID
    fun spawnPrefixChild(props: Props, prefix: String): PID
    fun spawnNamedChild(props: Props, name: String): PID
    fun watch(pid: PID)
    fun unwatch(pid: PID)
    fun setReceiveTimeout(duration: Duration)
    fun getReceiveTimeout(): Duration
    fun cancelReceiveTimeout()
    fun send(target: PID, message: Any)
    fun request(target: PID, message: Any)
    fun respond(message: Any)

    suspend fun <T> requestAwait(target: PID, message: Any, timeout: Duration): T
    suspend fun <T> requestAwait(target: PID, message: Any): T

    /**
     * Request a value from a target PID and return a Future.
     * @param target The target PID
     * @param message The message to send
     * @param timeout The timeout duration
     * @return A Future that will complete with the response
     */
    fun <T> requestFuture(target: PID, message: Any, timeout: Duration): Future<T>

    /**
     * Reenter the actor after the future completes.
     * This allows an actor to pause processing while waiting for a future to complete,
     * and then resume processing with the result of the future.
     * @param future The future to wait for
     * @param continuation The function to call when the future completes
     */
    fun <T> reenterAfter(future: Future<T>, continuation: (T?, Throwable?) -> Unit)
}

/**
 * ExtensionContext 是用于扩展上下文的接口
 * 提供了获取和设置上下文扩展的方法
 */
interface ExtensionContext {
    /**
     * 获取指定ID的上下文扩展
     * @param id 扩展ID
     * @return 扩展实例，如果不存在则返回null
     */
    fun get(id: ContextExtensionID): ContextExtension?

    /**
     * 设置上下文扩展
     * @param extension 扩展实例
     */
    fun set(extension: ContextExtension)
}

