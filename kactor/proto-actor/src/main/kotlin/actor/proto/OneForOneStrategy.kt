package actor.proto

import mu.KotlinLogging
import java.time.Duration
private val logger = KotlinLogging.logger {}
class OneForOneStrategy(private val decider: (PID, Exception) -> SupervisorDirective, private val maxNrOfRetries: Int, private val withinTimeSpan: Duration? = null) : SupervisorStrategy {
    override fun handleFailure(
        actorSystem: ActorSystem,
        supervisor: Supervisor,
        child: PID,
        restartStatistics: RestartStatistics,
        reason: Any,
        message: Any?
    ) {
        val directive: SupervisorDirective = decider(child, reason as Exception)
        when (directive) {
            SupervisorDirective.Resume -> {
                logger.debug("Resuming ${child.toShortString()} Reason $reason")
                supervisor.resumeChildren(child)
            }
            SupervisorDirective.Restart -> {
                if (requestRestartPermission(restartStatistics)) {
                    logger.debug("Restarting ${child.toShortString()} Reason $reason")
                    supervisor.restartChildren(child)
                } else {
                    logger.debug("Stopping ${child.toShortString()} Reason $reason")
                    supervisor.stopChildren(child)
                }
            }
            SupervisorDirective.Stop -> {
                logger.debug("Stopping ${child.toShortString()} Reason $reason")
                supervisor.stopChildren(child)
            }
            SupervisorDirective.Escalate -> supervisor.escalateFailure(reason, message)
        }
    }

    private fun requestRestartPermission(rs: RestartStatistics): Boolean {
        return when (maxNrOfRetries) {
            0 -> false
            else -> {
                rs.fail()
                when {
                    withinTimeSpan == null || rs.isWithinDuration(withinTimeSpan) -> rs.failureCount <= maxNrOfRetries
                    else -> {
                        rs.reset()
                        true
                    }
                }
            }
        }
    }
}
