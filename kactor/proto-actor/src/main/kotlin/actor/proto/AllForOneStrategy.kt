package actor.proto

import mu.KotlinLogging
import java.time.Duration
private val logger = KotlinLogging.logger {}
class AllForOneStrategy(private val decider: (PID, Exception) -> SupervisorDirective, private val maxNrOfRetries: Int, private val withinTimeSpan: Duration?) : SupervisorStrategy {
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
                when {
                    requestRestartPermission(restartStatistics) -> {
                        logger.debug("Restarting ${child.toShortString()} Reason $reason")
                        supervisor.restartChildren(*supervisor.children().toTypedArray())
                    }
                    else -> {
                        logger.debug("Stopping ${child.toShortString()} Reason $reason")
                        supervisor.stopChildren(*supervisor.children().toTypedArray())
                    }
                }
            }
            SupervisorDirective.Stop -> {
                logger.debug("Stopping ${child.toShortString()} Reason $reason")
                supervisor.stopChildren(*supervisor.children().toTypedArray())
            }
            SupervisorDirective.Escalate -> {
                supervisor.escalateFailure(reason, message)
            }
        }
    }

    private fun requestRestartPermission(rs: RestartStatistics): Boolean {
        if (maxNrOfRetries == 0) {
            return false
        }
        rs.fail()
        if (withinTimeSpan == null || rs.isWithinDuration(withinTimeSpan)) {
            return rs.failureCount <= maxNrOfRetries
        }
        rs.reset()
        return true
    }
}