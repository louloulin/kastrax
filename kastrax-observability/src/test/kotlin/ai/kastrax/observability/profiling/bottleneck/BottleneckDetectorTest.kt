package ai.kastrax.observability.profiling.bottleneck

import ai.kastrax.observability.profiling.ProfilingEvent
import ai.kastrax.observability.profiling.ProfilingResult
import ai.kastrax.observability.profiling.SessionStatus
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottleneckDetectorTest {
    @Test
    fun testDetectLongRunningSession() {
        val detector = BottleneckDetector()
        val config = BottleneckDetectionConfig(longRunningThresholdMs = 500)
        
        val result = createProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            duration = 1000 // 1 second
        )
        
        val detectionResult = detector.detectBottlenecks(result, config)
        
        assertTrue(detectionResult.hasBottlenecks())
        assertEquals(1, detectionResult.bottlenecks.size)
        assertEquals(BottleneckType.LONG_RUNNING_SESSION, detectionResult.bottlenecks[0].type)
    }

    @Test
    fun testDetectMemoryLeak() {
        val detector = BottleneckDetector()
        val config = BottleneckDetectionConfig(memoryGrowthThresholdBytes = 5 * 1024 * 1024) // 5MB
        
        val result = createProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            metrics = mapOf("heap_growth" to (10 * 1024 * 1024L)) // 10MB
        )
        
        val detectionResult = detector.detectBottlenecks(result, config)
        
        assertTrue(detectionResult.hasBottlenecks())
        assertEquals(1, detectionResult.bottlenecks.size)
        assertEquals(BottleneckType.MEMORY_LEAK, detectionResult.bottlenecks[0].type)
    }

    @Test
    fun testDetectHighMemoryUsage() {
        val detector = BottleneckDetector()
        val config = BottleneckDetectionConfig(highMemoryUsageThresholdBytes = 50 * 1024 * 1024) // 50MB
        
        val result = createProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            metrics = mapOf("max_heap_used" to (100 * 1024 * 1024L)) // 100MB
        )
        
        val detectionResult = detector.detectBottlenecks(result, config)
        
        assertTrue(detectionResult.hasBottlenecks())
        assertEquals(1, detectionResult.bottlenecks.size)
        assertEquals(BottleneckType.HIGH_MEMORY_USAGE, detectionResult.bottlenecks[0].type)
    }

    @Test
    fun testDetectSlowMethod() {
        val detector = BottleneckDetector()
        val config = BottleneckDetectionConfig(slowMethodThresholdMs = 50)
        
        val result = createProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            metrics = mapOf(
                "slowest_method" to "com.example.SlowMethod",
                "avg_method_duration_ms" to 100L
            )
        )
        
        val detectionResult = detector.detectBottlenecks(result, config)
        
        assertTrue(detectionResult.hasBottlenecks())
        assertEquals(1, detectionResult.bottlenecks.size)
        assertEquals(BottleneckType.SLOW_METHOD, detectionResult.bottlenecks[0].type)
    }

    @Test
    fun testDetectHighInvocationCount() {
        val detector = BottleneckDetector()
        val config = BottleneckDetectionConfig(highInvocationCountThreshold = 100)
        
        val result = createProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            metrics = mapOf(
                "most_invoked_method" to "com.example.FrequentMethod",
                "total_method_invocations" to 500
            )
        )
        
        val detectionResult = detector.detectBottlenecks(result, config)
        
        assertTrue(detectionResult.hasBottlenecks())
        assertEquals(1, detectionResult.bottlenecks.size)
        assertEquals(BottleneckType.HIGH_INVOCATION_COUNT, detectionResult.bottlenecks[0].type)
    }

    @Test
    fun testDetectLowSuccessRate() {
        val detector = BottleneckDetector()
        val config = BottleneckDetectionConfig(lowSuccessRateThreshold = 0.9)
        
        val result = createProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            metrics = mapOf("success_rate" to 0.7)
        )
        
        val detectionResult = detector.detectBottlenecks(result, config)
        
        assertTrue(detectionResult.hasBottlenecks())
        assertEquals(1, detectionResult.bottlenecks.size)
        assertEquals(BottleneckType.LOW_SUCCESS_RATE, detectionResult.bottlenecks[0].type)
    }

    @Test
    fun testNoBottlenecks() {
        val detector = BottleneckDetector()
        val config = BottleneckDetectionConfig()
        
        val result = createProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            duration = 100, // 100ms
            metrics = mapOf(
                "heap_growth" to (1 * 1024 * 1024L), // 1MB
                "max_heap_used" to (10 * 1024 * 1024L), // 10MB
                "avg_method_duration_ms" to 10L,
                "total_method_invocations" to 50,
                "success_rate" to 0.98
            )
        )
        
        val detectionResult = detector.detectBottlenecks(result, config)
        
        assertFalse(detectionResult.hasBottlenecks())
        assertEquals(0, detectionResult.bottlenecks.size)
    }

    @Test
    fun testMultipleBottlenecks() {
        val detector = BottleneckDetector()
        val config = BottleneckDetectionConfig(
            longRunningThresholdMs = 500,
            memoryGrowthThresholdBytes = 5 * 1024 * 1024, // 5MB
            slowMethodThresholdMs = 50
        )
        
        val result = createProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            duration = 1000, // 1 second
            metrics = mapOf(
                "heap_growth" to (10 * 1024 * 1024L), // 10MB
                "slowest_method" to "com.example.SlowMethod",
                "avg_method_duration_ms" to 100L
            )
        )
        
        val detectionResult = detector.detectBottlenecks(result, config)
        
        assertTrue(detectionResult.hasBottlenecks())
        assertEquals(3, detectionResult.bottlenecks.size)
        
        val bottleneckTypes = detectionResult.bottlenecks.map { it.type }
        assertTrue(bottleneckTypes.contains(BottleneckType.LONG_RUNNING_SESSION))
        assertTrue(bottleneckTypes.contains(BottleneckType.MEMORY_LEAK))
        assertTrue(bottleneckTypes.contains(BottleneckType.SLOW_METHOD))
    }

    @Test
    fun testGetBottlenecksByType() {
        val detector = BottleneckDetector()
        val config = BottleneckDetectionConfig(
            longRunningThresholdMs = 500,
            memoryGrowthThresholdBytes = 5 * 1024 * 1024 // 5MB
        )
        
        val result = createProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            duration = 1000, // 1 second
            metrics = mapOf("heap_growth" to (10 * 1024 * 1024L)) // 10MB
        )
        
        val detectionResult = detector.detectBottlenecks(result, config)
        
        val longRunningSessions = detectionResult.getBottlenecksByType(BottleneckType.LONG_RUNNING_SESSION)
        assertEquals(1, longRunningSessions.size)
        
        val memoryLeaks = detectionResult.getBottlenecksByType(BottleneckType.MEMORY_LEAK)
        assertEquals(1, memoryLeaks.size)
        
        val slowMethods = detectionResult.getBottlenecksByType(BottleneckType.SLOW_METHOD)
        assertEquals(0, slowMethods.size)
    }

    @Test
    fun testGetMostSevereBottleneck() {
        val detector = BottleneckDetector()
        val config = BottleneckDetectionConfig(
            longRunningThresholdMs = 500,
            memoryGrowthThresholdBytes = 5 * 1024 * 1024 // 5MB
        )
        
        val result = createProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            duration = 1000, // 1 second
            metrics = mapOf("heap_growth" to (10 * 1024 * 1024L)) // 10MB
        )
        
        val detectionResult = detector.detectBottlenecks(result, config)
        
        val mostSevere = detectionResult.getMostSevereBottleneck()
        assertEquals(BottleneckType.MEMORY_LEAK, mostSevere?.type)
    }

    @Test
    fun testGenerateReport() {
        val detector = BottleneckDetector()
        val config = BottleneckDetectionConfig(
            longRunningThresholdMs = 500,
            memoryGrowthThresholdBytes = 5 * 1024 * 1024 // 5MB
        )
        
        val result = createProfilingResult(
            sessionId = "test-session",
            name = "Test Session",
            duration = 1000, // 1 second
            metrics = mapOf("heap_growth" to (10 * 1024 * 1024L)) // 10MB
        )
        
        val detectionResult = detector.detectBottlenecks(result, config)
        
        val report = detectionResult.generateReport()
        assertTrue(report.contains("Bottleneck Detection Report"))
        assertTrue(report.contains("Total Bottlenecks Found: 2"))
        assertTrue(report.contains("LONG_RUNNING_SESSION"))
        assertTrue(report.contains("MEMORY_LEAK"))
        assertTrue(report.contains("Most Severe Bottleneck"))
    }

    private fun createProfilingResult(
        sessionId: String,
        name: String,
        startTime: Instant = Instant.now().minusMillis(1000),
        endTime: Instant = Instant.now(),
        duration: Long = 1000,
        status: SessionStatus = SessionStatus.COMPLETED,
        tags: Map<String, String> = emptyMap(),
        metrics: Map<String, Any> = emptyMap(),
        events: List<ProfilingEvent> = emptyList(),
        subSessions: List<ProfilingResult> = emptyList()
    ): ProfilingResult {
        return ProfilingResult(
            sessionId = sessionId,
            name = name,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            status = status,
            tags = tags,
            metrics = metrics,
            events = events,
            subSessions = subSessions
        )
    }
}
