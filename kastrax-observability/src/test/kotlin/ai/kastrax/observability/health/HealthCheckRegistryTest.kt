package ai.kastrax.observability.health

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HealthCheckRegistryTest {
    private lateinit var registry: HealthCheckRegistry

    @BeforeEach
    fun setUp() {
        registry = HealthCheckRegistry()
    }

    @Test
    fun testRegisterAndUnregister() {
        // 创建测试健康检查
        val healthCheck = object : HealthCheck {
            override fun getName(): String = "test"
            override fun check(): HealthResult = HealthResult.up()
        }

        // 注册健康检查
        registry.register(healthCheck)

        // 验证健康检查已注册
        val healthChecks = registry.getHealthChecks()
        assertEquals(1, healthChecks.size)
        assertTrue(healthChecks.containsKey("test"))
        assertEquals(healthCheck, healthChecks["test"])

        // 取消注册健康检查
        registry.unregister("test")

        // 验证健康检查已取消注册
        val updatedHealthChecks = registry.getHealthChecks()
        assertEquals(0, updatedHealthChecks.size)
        assertTrue(updatedHealthChecks.isEmpty())
    }

    @Test
    fun testRunHealthChecks() {
        // 创建测试健康检查
        val upCheck = object : HealthCheck {
            override fun getName(): String = "up"
            override fun check(): HealthResult = HealthResult.up(mapOf("status" to "good"))
        }

        val degradedCheck = object : HealthCheck {
            override fun getName(): String = "degraded"
            override fun check(): HealthResult = HealthResult.degraded(mapOf("status" to "slow"))
        }

        val downCheck = object : HealthCheck {
            override fun getName(): String = "down"
            override fun check(): HealthResult = HealthResult.down(mapOf("status" to "error"))
        }

        val throwingCheck = object : HealthCheck {
            override fun getName(): String = "throwing"
            override fun check(): HealthResult = throw RuntimeException("Test exception")
        }

        // 注册健康检查
        registry.register(upCheck)
            .register(degradedCheck)
            .register(downCheck)
            .register(throwingCheck)

        // 运行所有健康检查
        val results = registry.runHealthChecks()

        // 验证结果
        assertEquals(4, results.size)
        assertEquals(HealthStatus.UP, results["up"]?.status)
        assertEquals(HealthStatus.DEGRADED, results["degraded"]?.status)
        assertEquals(HealthStatus.DOWN, results["down"]?.status)
        assertEquals(HealthStatus.DOWN, results["throwing"]?.status)
        assertNotNull(results["throwing"]?.error)
    }

    @Test
    fun testRunHealthChecksByType() {
        // 创建测试健康检查
        val componentCheck = object : HealthCheck {
            override fun getName(): String = "component"
            override fun check(): HealthResult = HealthResult.up()
            override fun getType(): HealthCheckType = HealthCheckType.COMPONENT
        }

        val dependencyCheck = object : HealthCheck {
            override fun getName(): String = "dependency"
            override fun check(): HealthResult = HealthResult.up()
            override fun getType(): HealthCheckType = HealthCheckType.DEPENDENCY
        }

        // 注册健康检查
        registry.register(componentCheck)
            .register(dependencyCheck)

        // 运行组件健康检查
        val componentResults = registry.runHealthChecks(HealthCheckType.COMPONENT)
        assertEquals(1, componentResults.size)
        assertTrue(componentResults.containsKey("component"))

        // 运行依赖健康检查
        val dependencyResults = registry.runHealthChecks(HealthCheckType.DEPENDENCY)
        assertEquals(1, dependencyResults.size)
        assertTrue(dependencyResults.containsKey("dependency"))
    }

    @Test
    fun testRunHealthCheck() {
        // 创建测试健康检查
        val healthCheck = object : HealthCheck {
            override fun getName(): String = "test"
            override fun check(): HealthResult = HealthResult.up(mapOf("key" to "value"))
        }

        // 注册健康检查
        registry.register(healthCheck)

        // 运行特定健康检查
        val result = registry.runHealthCheck("test")
        assertNotNull(result)
        assertEquals(HealthStatus.UP, result.status)
        assertEquals("value", result.details["key"])

        // 运行不存在的健康检查
        val nonExistentResult = registry.runHealthCheck("non_existent")
        assertNull(nonExistentResult)
    }

    @Test
    fun testGetAggregateStatus() {
        // 空注册表应该返回 UP
        assertEquals(HealthStatus.UP, registry.getAggregateStatus())

        // 只有 UP 状态的检查应该返回 UP
        registry.register(object : HealthCheck {
            override fun getName(): String = "up1"
            override fun check(): HealthResult = HealthResult.up()
        })
        registry.register(object : HealthCheck {
            override fun getName(): String = "up2"
            override fun check(): HealthResult = HealthResult.up()
        })
        assertEquals(HealthStatus.UP, registry.getAggregateStatus())

        // 有 DEGRADED 状态的检查应该返回 DEGRADED
        registry.register(object : HealthCheck {
            override fun getName(): String = "degraded"
            override fun check(): HealthResult = HealthResult.degraded()
        })
        assertEquals(HealthStatus.DEGRADED, registry.getAggregateStatus())

        // 有 DOWN 状态的检查应该返回 DOWN
        registry.register(object : HealthCheck {
            override fun getName(): String = "down"
            override fun check(): HealthResult = HealthResult.down()
        })
        assertEquals(HealthStatus.DOWN, registry.getAggregateStatus())
    }

    @Test
    fun testGetAggregateStatusByType() {
        // 创建测试健康检查
        val upComponentCheck = object : HealthCheck {
            override fun getName(): String = "up_component"
            override fun check(): HealthResult = HealthResult.up()
            override fun getType(): HealthCheckType = HealthCheckType.COMPONENT
        }

        val degradedComponentCheck = object : HealthCheck {
            override fun getName(): String = "degraded_component"
            override fun check(): HealthResult = HealthResult.degraded()
            override fun getType(): HealthCheckType = HealthCheckType.COMPONENT
        }

        val downDependencyCheck = object : HealthCheck {
            override fun getName(): String = "down_dependency"
            override fun check(): HealthResult = HealthResult.down()
            override fun getType(): HealthCheckType = HealthCheckType.DEPENDENCY
        }

        // 注册健康检查
        registry.register(upComponentCheck)
            .register(degradedComponentCheck)
            .register(downDependencyCheck)

        // 验证组件状态
        assertEquals(HealthStatus.DEGRADED, registry.getAggregateStatus(HealthCheckType.COMPONENT))

        // 验证依赖状态
        assertEquals(HealthStatus.DOWN, registry.getAggregateStatus(HealthCheckType.DEPENDENCY))
    }
}
