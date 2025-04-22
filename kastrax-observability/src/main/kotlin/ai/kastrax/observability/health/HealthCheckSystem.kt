package ai.kastrax.observability.health

/**
 * 健康检查系统。
 * 提供集中管理健康检查的功能。
 */
object HealthCheckSystem {
    private val registry = HealthCheckRegistry()

    /**
     * 注册健康检查。
     *
     * @param healthCheck 要注册的健康检查
     */
    fun registerHealthCheck(healthCheck: HealthCheck) {
        registry.register(healthCheck)
    }

    /**
     * 取消注册健康检查。
     *
     * @param name 要取消注册的健康检查名称
     */
    fun unregisterHealthCheck(name: String) {
        registry.unregister(name)
    }

    /**
     * 获取所有注册的健康检查。
     *
     * @return 所有注册的健康检查的映射
     */
    fun getHealthChecks(): Map<String, HealthCheck> {
        return registry.getHealthChecks()
    }

    /**
     * 执行所有健康检查。
     *
     * @return 所有健康检查的结果映射
     */
    fun runHealthChecks(): Map<String, HealthResult> {
        return registry.runHealthChecks()
    }

    /**
     * 执行指定类型的健康检查。
     *
     * @param type 健康检查类型
     * @return 指定类型的健康检查的结果映射
     */
    fun runHealthChecks(type: HealthCheckType): Map<String, HealthResult> {
        return registry.runHealthChecks(type)
    }

    /**
     * 执行指定名称的健康检查。
     *
     * @param name 健康检查名称
     * @return 健康检查结果，如果找不到指定名称的健康检查则返回 null
     */
    fun runHealthCheck(name: String): HealthResult? {
        return registry.runHealthCheck(name)
    }

    /**
     * 获取系统的整体健康状态。
     *
     * @return 聚合的健康状态
     */
    fun getStatus(): HealthStatus {
        return registry.getAggregateStatus()
    }

    /**
     * 获取指定类型的健康状态。
     *
     * @param type 健康检查类型
     * @return 聚合的健康状态
     */
    fun getStatus(type: HealthCheckType): HealthStatus {
        return registry.getAggregateStatus(type)
    }

    /**
     * 获取健康检查报告。
     *
     * @return 健康检查报告
     */
    fun getHealthReport(): HealthReport {
        val results = registry.runHealthChecks()
        val status = registry.getAggregateStatus()
        return HealthReport(status, results)
    }
}
