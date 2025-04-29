package actor.proto


import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.*

class ExponentialBackoffStrategy(private val backoffWindow: Duration, private val initialBackoff: Duration) : SupervisorStrategy {
    private val random: Random = Random()

    override fun handleFailure(actorSystem: ActorSystem, supervisor: Supervisor, child: PID, restartStatistics: RestartStatistics, reason: Any, message: Any?) {
        setFailureCount(restartStatistics)
        val backoff: Long = restartStatistics.failureCount * initialBackoff.toNanos()
        val noise: Int = random.nextInt(500)
        val duration: Duration = Duration.ofNanos(backoff + noise)
        GlobalScope.launch {
            delay(duration.toMillis())
            supervisor.restartChildren(child)
        }
    }

    private fun setFailureCount(rs: RestartStatistics) {
        if (rs.isWithinDuration(backoffWindow)) {
            rs.fail()
            return
        }
        rs.reset()
    }
}
