package actor.proto

import actor.proto.mailbox.SystemMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeoutException

/**
 * Error used when a future times out before receiving a result.
 */
class FutureTimeoutException : TimeoutException("Future: timeout")

/**
 * Error used when a request is sent to an unreachable PID.
 */
class DeadLetterException : Exception("Future: dead letter")

/**
 * Future represents a value that will be available in the future.
 * It is used for request-response patterns and reentrancy.
 */
class Future<T>(private val system: ActorSystem, timeout: Duration? = null) {
    private val deferred = CompletableDeferred<T>()
    private val pipes = ConcurrentLinkedQueue<PID>()
    private val completions = ConcurrentLinkedQueue<(T?, Throwable?) -> Unit>()
    val pid: PID

    init {
        val process = FutureProcess(this)
        val id = ProcessRegistry.nextId()
        pid = ProcessRegistry.put("future$id", process)

        if (timeout != null && !timeout.isNegative && !timeout.isZero) {
            val timeoutMillis = timeout.toMillis()
            if (timeoutMillis > 0) {
                system.scheduler.scheduleOnce(timeoutMillis) {
                    if (!deferred.isCompleted) {
                        val error = FutureTimeoutException()
                        deferred.completeExceptionally(error)
                        process.stop(pid)
                    }
                }
            }
        }
    }

    /**
     * Get the PID of the future process.
     */
    fun getPID(): PID = pid

    /**
     * Pipe the result of this future to the specified PIDs.
     */
    fun pipeTo(vararg pids: PID) {
        pids.forEach { pipes.add(it) }
        if (deferred.isCompleted) {
            sendToPipes()
        }
    }

    private fun sendToPipes() {
        if (pipes.isEmpty()) return

        val result: Any = try {
            deferred.getCompleted() as Any
        } catch (e: Exception) {
            e
        }

        while (pipes.isNotEmpty()) {
            val pid = pipes.poll()
            system.send(pid, result)
        }
    }

    /**
     * Wait for the future to complete and return the result.
     */
    suspend fun result(): T = deferred.await()

    /**
     * Complete the future with a result.
     */
    fun complete(result: T) {
        deferred.complete(result)
        sendToPipes()
        runCompletions(result, null)
    }

    /**
     * Complete the future with an error.
     */
    fun completeExceptionally(error: Throwable) {
        deferred.completeExceptionally(error)
        sendToPipes()
        runCompletions(null, error)
    }

    /**
     * Register a continuation to be called when the future completes.
     */
    fun continueWith(continuation: (T?, Throwable?) -> Unit) {
        if (deferred.isCompleted) {
            try {
                val result = deferred.getCompleted()
                continuation(result, null)
            } catch (e: Exception) {
                continuation(null, e)
            }
        } else {
            completions.add(continuation)
        }
    }

    private fun runCompletions(result: T?, error: Throwable?) {
        while (completions.isNotEmpty()) {
            val completion = completions.poll()
            completion(result, error)
        }
    }

    /**
     * Get the underlying deferred object.
     */
    fun getDeferred(): Deferred<T> = deferred
}

/**
 * Process implementation for futures.
 */
class FutureProcess<T>(private val future: Future<T>) : Process() {
    override fun sendUserMessage(pid: PID, message: Any) {
        when (message) {
            is DeadLetterEvent -> future.completeExceptionally(DeadLetterException())
            else -> {
                @Suppress("UNCHECKED_CAST")
                future.complete(message as T)
            }
        }
        stop(pid)
    }

    override fun sendSystemMessage(pid: PID, message: SystemMessage) {
        @Suppress("UNCHECKED_CAST")
        future.complete(message as T)
        stop(pid)
    }

    override fun stop(pid: PID) {
        ProcessRegistry.remove(pid)
    }
}
