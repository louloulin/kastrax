package actor.proto.fixture

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.RestartStatistics
import actor.proto.Supervisor
import actor.proto.SupervisorStrategy

class DoNothingSupervisorStrategy : SupervisorStrategy {
    override fun handleFailure(actorSystem: ActorSystem, supervisor: Supervisor, child: PID, restartStatistics: RestartStatistics, reason: Any, message: Any?) {
    }
}

