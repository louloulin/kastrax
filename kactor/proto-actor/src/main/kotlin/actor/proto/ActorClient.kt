package actor.proto

import actor.proto.logging.DefaultLoggerFactory
import actor.proto.logging.Logger
import kotlinx.coroutines.runBlocking
import java.time.Duration

class ActorClient(messageHeader: MessageHeader = MessageHeader.EMPTY, senderMiddleware: List<SenderMiddleware> = listOf()) : SenderContext {

    /**
     * Logger for the actor client
     */
    override val logger: Logger = DefaultLoggerFactory.getLogger("ActorClient")

    private val senderMiddleware: Send? = when {
        senderMiddleware.isEmpty() -> null
        else -> senderMiddleware
                .reversed()
                .fold({ ctx, targetPid, envelope -> ContextHelper.defaultSender(ctx, targetPid, envelope) },
                        { inner, outer -> outer(inner!!) })
    }

    override val message: Any?
        get() = null

    override val headers: MessageHeader? = messageHeader

    override val sender: PID?
        get() = null

    override val self: PID
        get() = ActorSystem.default().deadLetter()

    override fun send(target: PID, message: Any) {
        return when (senderMiddleware) {
            null -> {
                val process: Process = target.cachedProcess() ?: ProcessRegistry.get(target)
                process.sendUserMessage(target, message)
            }
            else -> {
                val c = this
                when (message) {
                    is MessageEnvelope -> runBlocking { senderMiddleware.invoke(c, target, message) }
                    else -> runBlocking { senderMiddleware.invoke(c, target, MessageEnvelope(message, null, null)) }
                }
            }
        }
    }

    override fun request(target: PID, message: Any) {
        val envelope = MessageEnvelope(message, self, null)
        send(target, envelope)
    }

    fun request(target: PID, message: Any, sender: PID) {
        val envelope = MessageEnvelope(message, sender, null)
        send(target, envelope)
    }

    suspend fun <T> requestAwait(target: PID, message: Any, timeout: Duration): T {
        val deferredProcess = DeferredProcess<T>(timeout)
        request(target, message, deferredProcess.pid)
        return deferredProcess.await()
    }
}

fun send(target: PID, message: Any) = DefaultActorClient.send(target, message)
fun request(target: PID, message: Any, sender: PID) = DefaultActorClient.request(target, message, sender)
suspend fun <T> requestAwait(target: PID, message: Any, timeout: Duration): T = DefaultActorClient.requestAwait(target, message, timeout)

val DefaultActorClient = ActorClient()
