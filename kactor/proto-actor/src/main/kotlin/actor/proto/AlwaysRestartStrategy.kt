package actor.proto

class AlwaysRestartStrategy() : SupervisorStrategy {
    override fun handleFailure(
        actorSystem: ActorSystem,
        supervisor: Supervisor,
        child: PID,
        restartStatistics: RestartStatistics,
        reason: Any,
        message: Any?
    ) {
        supervisor.restartChildren(child)
    }
}
