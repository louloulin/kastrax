package ai.kastrax.observability.health

import java.util.concurrent.ConcurrentHashMap

/**
 * 健康检查注册表。
 * 用于管理和执行所有注册的健康检查。
 */
class HealthCheckRegistry {
    private val healthChecks = ConcurrentHashMap<String, HealthCheck>()

    /**
     * 注册健康检查。
     *
     * @param healthCheck 要注册的健康检查
     * @return 当前注册表实例，用于链式调用
     */
    fun register(healthCheck: HealthCheck): HealthCheckRegistry {
        healthChecks[healthCheck.getName()] = healthCheck
        return this
    }

    /**
     * 取消注册健康检查。
     *
     * @param name 要取消注册的健康检查名称
     * @return 当前注册表实例，用于链式调用
     */
    fun unregister(name: String): HealthCheckRegistry {
        healthChecks.remove(name)
        return this
    }

    /**
     * 获取所有注册的健康检查。
     *
     * @return 所有注册的健康检查的映射
     */
    fun getHealthChecks(): Map<String, HealthCheck> {
        return healthChecks.toMap()
    }

    /**
     * 执行所有健康检查。
     *
     * @return 所有健康检查的结果映射
     */
    fun runHealthChecks(): Map<String, HealthResult> {
        return healthChecks.mapValues { (_, healthCheck) ->
            try {
                healthCheck.check()
            } catch (e: Exception) {
                HealthResult.down(mapOf("error" to e.message.orEmpty()), e)
            }
        }
    }

    /**
     * 执行指定类型的健康检查。
     *
     * @param type 健康检查类型
     * @return 指定类型的健康检查的结果映射
     */
    fun runHealthChecks(type: HealthCheckType): Map<String, HealthResult> {
        return healthChecks
            .filter { (_, healthCheck) -> healthCheck.getType() == type }
            .mapValues { (_, healthCheck) ->
                try {
                    healthCheck.check()
                } catch (e: Exception) {
                    HealthResult.down(mapOf("error" to e.message.orEmpty()), e)
                }
            }
    }

    /**
     * 执行指定名称的健康检查。
     *
     * @param name 健康检查名称
     * @return 健康检查结果，如果找不到指定名称的健康检查则返回 null
     */
    fun runHealthCheck(name: String): HealthResult? {
        val healthCheck = healthChecks[name] ?: return null
        return try {
            healthCheck.check()
        } catch (e: Exception) {
            HealthResult.down(mapOf("error" to e.message.orEmpty()), e)
        }
    }

    /**
     * 聚合所有健康检查的结果。
     *
     * @return 聚合的健康状态
     */
    fun getAggregateStatus(): HealthStatus {
        val results = runHealthChecks()
        if (results.isEmpty()) {
            return HealthStatus.UP
        }

        val hasDown = results.any { (_, result) -> result.status == HealthStatus.DOWN }
        if (hasDown) {
            return HealthStatus.DOWN
        }

        val hasDegraded = results.any { (_, result) -> result.status == HealthStatus.DEGRADED }
        if (hasDegraded) {
            return HealthStatus.DEGRADED
        }

        return HealthStatus.UP
    }

    /**
     * 聚合指定类型的健康检查的结果。
     *
     * @param type 健康检查类型
     * @return 聚合的健康状态
     */
    fun getAggregateStatus(type: HealthCheckType): HealthStatus {
        val results = runHealthChecks(type)
        if (results.isEmpty()) {
            return HealthStatus.UP
        }

        val hasDown = results.any { (_, result) -> result.status == HealthStatus.DOWN }
        if (hasDown) {
            return HealthStatus.DOWN
        }

        val hasDegraded = results.any { (_, result) -> result.status == HealthStatus.DEGRADED }
        if (hasDegraded) {
            return HealthStatus.DEGRADED
        }

        return HealthStatus.UP
    }
}
