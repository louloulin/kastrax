package actor.proto

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Scheduler for scheduling tasks to run after a delay.
 */
class Scheduler {
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    /**
     * Schedule a task to run once after a delay.
     * @param delayMillis The delay in milliseconds
     * @param task The task to run
     */
    fun scheduleOnce(delayMillis: Long, task: () -> Unit) {
        executor.schedule(task, delayMillis, TimeUnit.MILLISECONDS)
    }

    /**
     * Schedule a task to run repeatedly with a fixed delay.
     * @param initialDelayMillis The initial delay in milliseconds
     * @param intervalMillis The interval in milliseconds
     * @param task The task to run
     */
    fun scheduleRepeatedly(initialDelayMillis: Long, intervalMillis: Long, task: () -> Unit) {
        executor.scheduleAtFixedRate(task, initialDelayMillis, intervalMillis, TimeUnit.MILLISECONDS)
    }

    /**
     * Shutdown the scheduler.
     */
    fun shutdown() {
        executor.shutdown()
    }
}
