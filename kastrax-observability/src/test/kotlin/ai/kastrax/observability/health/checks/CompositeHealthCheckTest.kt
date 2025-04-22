package ai.kastrax.observability.health.checks

import ai.kastrax.observability.health.HealthCheck
import ai.kastrax.observability.health.HealthCheckType
import ai.kastrax.observability.health.HealthResult
import ai.kastrax.observability.health.HealthStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompositeHealthCheckTest {
    @Test
    fun testCompositeHealthCheckWithNoChecks() {
        // 创建没有子检查的组合健康检查
        val healthCheck = CompositeHealthCheck("empty", emptyList())

        // 执行健康检查
        val result = healthCheck.check()

        // 验证结果
        assertEquals(HealthStatus.UP, result.status)
        assertEquals("No health checks configured", result.details["message"])
    }

    @Test
    fun testCompositeHealthCheckWithAllUp() {
        // 创建测试健康检查
        val check1 = object : HealthCheck {
            override fun getName(): String = "check1"
            override fun check(): HealthResult = HealthResult.up()
        }

        val check2 = object : HealthCheck {
            override fun getName(): String = "check2"
            override fun check(): HealthResult = HealthResult.up()
        }

        // 创建组合健康检查
        val healthCheck = CompositeHealthCheck("all_up", listOf(check1, check2))

        // 执行健康检查
        val result = healthCheck.check()

        // 验证结果
        assertEquals(HealthStatus.UP, result.status)
        assertEquals("All health checks are up", result.details["message"])
    }

    @Test
    fun testCompositeHealthCheckWithDegraded() {
        // 创建测试健康检查
        val upCheck = object : HealthCheck {
            override fun getName(): String = "up"
            override fun check(): HealthResult = HealthResult.up()
        }

        val degradedCheck = object : HealthCheck {
            override fun getName(): String = "degraded"
            override fun check(): HealthResult = HealthResult.degraded()
        }

        // 创建组合健康检查
        val healthCheck = CompositeHealthCheck("with_degraded", listOf(upCheck, degradedCheck))

        // 执行健康检查
        val result = healthCheck.check()

        // 验证结果
        assertEquals(HealthStatus.DEGRADED, result.status)
        assertEquals("Some health checks are degraded", result.details["message"])
        assertEquals("degraded", result.details["degraded_checks"])
    }

    @Test
    fun testCompositeHealthCheckWithDown() {
        // 创建测试健康检查
        val upCheck = object : HealthCheck {
            override fun getName(): String = "up"
            override fun check(): HealthResult = HealthResult.up()
        }

        val degradedCheck = object : HealthCheck {
            override fun getName(): String = "degraded"
            override fun check(): HealthResult = HealthResult.degraded()
        }

        val downCheck = object : HealthCheck {
            override fun getName(): String = "down"
            override fun check(): HealthResult = HealthResult.down()
        }

        // 创建组合健康检查
        val healthCheck = CompositeHealthCheck("with_down", listOf(upCheck, degradedCheck, downCheck))

        // 执行健康检查
        val result = healthCheck.check()

        // 验证结果
        assertEquals(HealthStatus.DOWN, result.status)
        assertEquals("Some health checks are down", result.details["message"])
        assertEquals("down", result.details["down_checks"])
    }

    @Test
    fun testCompositeHealthCheckWithException() {
        // 创建测试健康检查
        val upCheck = object : HealthCheck {
            override fun getName(): String = "up"
            override fun check(): HealthResult = HealthResult.up()
        }

        val throwingCheck = object : HealthCheck {
            override fun getName(): String = "throwing"
            override fun check(): HealthResult = throw RuntimeException("Test exception")
        }

        // 创建组合健康检查
        val healthCheck = CompositeHealthCheck("with_exception", listOf(upCheck, throwingCheck))

        // 执行健康检查
        val result = healthCheck.check()

        // 验证结果
        assertEquals(HealthStatus.DOWN, result.status)
        assertEquals("Some health checks are down", result.details["message"])
        assertTrue((result.details["down_checks"] as String).contains("throwing"))
    }

    @Test
    fun testGetName() {
        val healthCheck = CompositeHealthCheck("test", emptyList())
        assertEquals("test", healthCheck.getName())
    }

    @Test
    fun testGetType() {
        // 默认类型应该是 COMPONENT
        val defaultHealthCheck = CompositeHealthCheck("default", emptyList())
        assertEquals(HealthCheckType.COMPONENT, defaultHealthCheck.getType())

        // 指定类型应该被保留
        val dependencyHealthCheck = CompositeHealthCheck(
            "dependency",
            emptyList(),
            HealthCheckType.DEPENDENCY
        )
        assertEquals(HealthCheckType.DEPENDENCY, dependencyHealthCheck.getType())
    }
}
