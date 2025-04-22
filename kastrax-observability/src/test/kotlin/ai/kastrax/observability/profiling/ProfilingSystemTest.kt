package ai.kastrax.observability.profiling

import ai.kastrax.observability.profiling.time.ExecutionTimeProfiler
import ai.kastrax.observability.profiling.memory.MemoryProfiler
import ai.kastrax.observability.profiling.method.MethodInvocationProfiler
import ai.kastrax.observability.profiling.bottleneck.BottleneckType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProfilingSystemTest {
    @Test
    fun testGetProfilers() {
        val profilers = ProfilingSystem.getProfilers()

        // 验证默认分析器已注册
        assertTrue(profilers.containsKey("execution-time-profiler"))
        assertTrue(profilers.containsKey("memory-profiler"))
        assertTrue(profilers.containsKey("method-invocation-profiler"))
        assertTrue(profilers.containsKey("composite-profiler"))
    }

    @Test
    fun testRegisterAndUnregisterProfiler() {
        val customProfiler = ExecutionTimeProfiler("custom-profiler")

        // 注册自定义分析器
        ProfilingSystem.registerProfiler(customProfiler)

        // 验证自定义分析器已注册
        val profilers = ProfilingSystem.getProfilers()
        assertTrue(profilers.containsKey("custom-profiler"))

        // 取消注册自定义分析器
        ProfilingSystem.unregisterProfiler("custom-profiler")

        // 验证自定义分析器已取消注册
        val updatedProfilers = ProfilingSystem.getProfilers()
        assertTrue(!updatedProfilers.containsKey("custom-profiler"))
    }

    @Test
    fun testGetProfilersByType() {
        val executionTimeProfilers = ProfilingSystem.getProfilersByType(ProfilerType.EXECUTION_TIME)
        assertEquals(1, executionTimeProfilers.size)
        assertEquals("execution-time-profiler", executionTimeProfilers[0].getName())

        val memoryProfilers = ProfilingSystem.getProfilersByType(ProfilerType.MEMORY_USAGE)
        assertEquals(1, memoryProfilers.size)
        assertEquals("memory-profiler", memoryProfilers[0].getName())

        val methodInvocationProfilers = ProfilingSystem.getProfilersByType(ProfilerType.METHOD_INVOCATION)
        assertEquals(1, methodInvocationProfilers.size)
        assertEquals("method-invocation-profiler", methodInvocationProfilers[0].getName())

        val compositeProfilers = ProfilingSystem.getProfilersByType(ProfilerType.COMPOSITE)
        assertEquals(1, compositeProfilers.size)
        assertEquals("composite-profiler", compositeProfilers[0].getName())
    }

    @Test
    fun testGetDefaultProfiler() {
        val defaultProfiler = ProfilingSystem.getDefaultProfiler()
        assertEquals(ProfilerType.COMPOSITE, defaultProfiler.getType())
    }

    @Test
    fun testSetDefaultProfiler() {
        val originalDefault = ProfilingSystem.getDefaultProfiler()

        // 设置新的默认分析器
        val executionTimeProfiler = ProfilingSystem.getProfiler("execution-time-profiler")
        assertNotNull(executionTimeProfiler)
        ProfilingSystem.setDefaultProfiler(executionTimeProfiler)

        // 验证默认分析器已更改
        val newDefault = ProfilingSystem.getDefaultProfiler()
        assertEquals("execution-time-profiler", newDefault.getName())

        // 恢复原始默认分析器
        ProfilingSystem.setDefaultProfiler(originalDefault)
    }

    @Test
    fun testStartSession() {
        val session = ProfilingSystem.startSession("test-session")

        assertEquals("test-session", session.getName())
        assertEquals(SessionStatus.ACTIVE, session.getStatus())

        // 清理
        session.end()
    }

    @Test
    fun testWithSession() {
        val result = ProfilingSystem.withSession("test-session") { session ->
            session.addTag("test", "true")
            "test-result"
        }

        assertEquals("test-result", result)
    }

    @Test
    fun testWithSessionSpecificProfiler() {
        val result = ProfilingSystem.withSession("test-session", "execution-time-profiler") { session ->
            session.addTag("test", "true")
            "test-result"
        }

        assertEquals("test-result", result)
    }

    @Test
    fun testDetectBottlenecks() {
        val result = ProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            startTime = java.time.Instant.now().minusMillis(1000),
            endTime = java.time.Instant.now(),
            duration = 1000,
            status = SessionStatus.COMPLETED,
            tags = emptyMap(),
            metrics = mapOf("heap_growth" to (10 * 1024 * 1024L)), // 10MB
            events = emptyList(),
            subSessions = emptyList()
        )

        // Create a config with a lower memory growth threshold to ensure detection
        val config = ai.kastrax.observability.profiling.bottleneck.BottleneckDetectionConfig(
            memoryGrowthThresholdBytes = 5 * 1024 * 1024 // 5MB
        )

        val detectionResult = ProfilingSystem.detectBottlenecks(result, config)

        assertTrue(detectionResult.hasBottlenecks())
        assertEquals(1, detectionResult.bottlenecks.size)
        assertEquals(BottleneckType.MEMORY_LEAK, detectionResult.bottlenecks[0].type)
    }
}
