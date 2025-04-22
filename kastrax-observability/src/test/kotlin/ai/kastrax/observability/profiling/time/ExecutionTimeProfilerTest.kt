package ai.kastrax.observability.profiling.time

import ai.kastrax.observability.profiling.SessionStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExecutionTimeProfilerTest {
    @Test
    fun testStartSession() {
        val profiler = ExecutionTimeProfiler()
        val session = profiler.startSession("test-session")
        
        assertEquals("test-session", session.getName())
        assertEquals(SessionStatus.ACTIVE, session.getStatus())
        assertNotNull(session.getStartTime())
        assertEquals(null, session.getEndTime())
    }

    @Test
    fun testWithSession() {
        val profiler = ExecutionTimeProfiler()
        val result = profiler.withSession("test-session") { session ->
            session.addTag("test", "true")
            "test-result"
        }
        
        assertEquals("test-result", result)
        assertEquals(1, profiler.getCompletedSessions().size)
        
        val completedSession = profiler.getCompletedSessions()[0]
        assertEquals("test-session", completedSession.getName())
        assertEquals(SessionStatus.COMPLETED, completedSession.getStatus())
        assertNotNull(completedSession.getStartTime())
        assertNotNull(completedSession.getEndTime())
        assertEquals("true", completedSession.getTags()["test"])
    }

    @Test
    fun testWithSessionException() {
        val profiler = ExecutionTimeProfiler()
        
        try {
            profiler.withSession("test-session") { session ->
                session.addTag("test", "true")
                throw RuntimeException("Test exception")
            }
        } catch (e: RuntimeException) {
            assertEquals("Test exception", e.message)
        }
        
        assertEquals(1, profiler.getCompletedSessions().size)
        
        val completedSession = profiler.getCompletedSessions()[0]
        assertEquals("test-session", completedSession.getName())
        assertEquals(SessionStatus.COMPLETED, completedSession.getStatus())
        assertEquals("true", completedSession.getTags()["test"])
        assertEquals("true", completedSession.getTags()["error"])
    }

    @Test
    fun testSubSessions() {
        val profiler = ExecutionTimeProfiler()
        val result = profiler.withSession("parent-session") { session ->
            session.withSubSession("child-session-1") { child1 ->
                child1.addTag("child", "1")
                "child-1-result"
            }
            
            session.withSubSession("child-session-2") { child2 ->
                child2.addTag("child", "2")
                "child-2-result"
            }
            
            "parent-result"
        }
        
        assertEquals("parent-result", result)
        assertEquals(1, profiler.getCompletedSessions().size)
        
        val parentSession = profiler.getCompletedSessions()[0]
        assertEquals("parent-session", parentSession.getName())
        assertEquals(2, parentSession.getSubSessions().size)
        
        val childSession1 = parentSession.getSubSessions()[0]
        assertEquals("child-session-1", childSession1.getName())
        assertEquals("1", childSession1.getTags()["child"])
        
        val childSession2 = parentSession.getSubSessions()[1]
        assertEquals("child-session-2", childSession2.getName())
        assertEquals("2", childSession2.getTags()["child"])
    }

    @Test
    fun testSessionMetrics() {
        val profiler = ExecutionTimeProfiler()
        val result = profiler.withSession("test-session") { session ->
            session.addMetric("custom_metric", 42)
            Thread.sleep(100) // 确保会话持续一段时间
            "test-result"
        }
        
        val completedSession = profiler.getCompletedSessions()[0]
        val metrics = completedSession.getMetrics()
        
        assertEquals(42, metrics["custom_metric"])
        assertTrue(metrics.containsKey("duration_ms"))
        assertTrue((metrics["duration_ms"] as Long) >= 100)
    }

    @Test
    fun testSessionEvents() {
        val profiler = ExecutionTimeProfiler()
        val result = profiler.withSession("test-session") { session ->
            session.recordEvent("test-event-1", mapOf("key1" to "value1"))
            session.recordEvent("test-event-2", mapOf("key2" to "value2"))
            "test-result"
        }
        
        val completedSession = profiler.getCompletedSessions()[0]
        val events = completedSession.getEvents()
        
        assertEquals(2, events.size)
        assertEquals("test-event-1", events[0].name)
        assertEquals("value1", events[0].attributes["key1"])
        assertEquals("test-event-2", events[1].name)
        assertEquals("value2", events[1].attributes["key2"])
    }

    @Test
    fun testCancelSession() {
        val profiler = ExecutionTimeProfiler()
        val session = profiler.startSession("test-session")
        
        session.cancel("Test cancellation")
        
        assertEquals(SessionStatus.CANCELLED, session.getStatus())
        assertNotNull(session.getEndTime())
        
        assertEquals(1, profiler.getCompletedSessions().size)
        val completedSession = profiler.getCompletedSessions()[0]
        assertEquals("Test cancellation", completedSession.getMetrics()["cancel_reason"])
    }

    @Test
    fun testClearCompletedSessions() {
        val profiler = ExecutionTimeProfiler()
        
        profiler.withSession("session-1") { "result-1" }
        profiler.withSession("session-2") { "result-2" }
        
        assertEquals(2, profiler.getCompletedSessions().size)
        
        profiler.clearCompletedSessions()
        
        assertEquals(0, profiler.getCompletedSessions().size)
    }
}
