package actor.proto.tests

import actor.proto.ActorSystem
import actor.proto.ExponentialBackoffStrategy
import actor.proto.PID
import actor.proto.RestartStatistics
import actor.proto.Supervisor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class SupervisionTests_ExponentialBackoff {
    @Test
    fun `a failure outside window should zero the count`() {
        val lastFailureIsOlderThanWindow = System.currentTimeMillis() - Duration.ofSeconds(11).toMillis()
        val rs: RestartStatistics = RestartStatistics(10, lastFailureIsOlderThanWindow)
        val strategy: ExponentialBackoffStrategy = ExponentialBackoffStrategy(Duration.ofSeconds(10), Duration.ofSeconds(1))

        strategy.handleFailure(ActorSystem.default(), DummySupervisor(), dummyPID(), rs, Exception(), null)

        assertEquals(0, rs.failureCount)
    }


    @Test
    fun `a failure inside window should increment count`() {
        val lastFailureIsNewerThanWindow = System.currentTimeMillis() - Duration.ofSeconds(9).toMillis()
        val rs: RestartStatistics = RestartStatistics(10, lastFailureIsNewerThanWindow)
        val strategy: ExponentialBackoffStrategy = ExponentialBackoffStrategy(Duration.ofSeconds(10), Duration.ofSeconds(1))

        strategy.handleFailure(ActorSystem.default(), DummySupervisor(), dummyPID(), rs, Exception(), null)

        assertEquals(11, rs.failureCount)
    }
}

class DummySupervisor : Supervisor {
    override fun children(): Set<PID> = emptySet()

    override fun escalateFailure(reason: Any, message: Any?) {}

    override fun restartChildren(vararg pids: PID) {}

    override fun stopChildren(vararg pids: PID) {}

    override fun resumeChildren(vararg pids: PID) {}
}

fun dummyPID(): PID {
    return PID("", "")
}
