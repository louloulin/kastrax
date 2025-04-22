package ai.kastrax.observability.health.checks

import ai.kastrax.observability.health.HealthCheck
import ai.kastrax.observability.health.HealthCheckType
import ai.kastrax.observability.health.HealthResult
import ai.kastrax.observability.health.HealthStatus

/**
 * 组合健康检查。
 * 将多个健康检查组合成一个。
 *
 * @property name 健康检查名称
 * @property healthChecks 要组合的健康检查列表
 * @property type 健康检查类型
 */
class CompositeHealthCheck(
    private val name: String,
    private val healthChecks: List<HealthCheck>,
    private val type: HealthCheckType = HealthCheckType.COMPONENT
) : HealthCheck {
    override fun getName(): String = name

    override fun getType(): HealthCheckType = type

    override fun check(): HealthResult {
        if (healthChecks.isEmpty()) {
            return HealthResult.up(mapOf("message" to "No health checks configured"))
        }

        val results = healthChecks.associateBy { it.getName() }
            .mapValues { (_, healthCheck) ->
                try {
                    healthCheck.check()
                } catch (e: Exception) {
                    HealthResult.down(mapOf("error" to e.message.orEmpty()), e)
                }
            }

        val hasDown = results.any { (_, result) -> result.status == HealthStatus.DOWN }
        if (hasDown) {
            val downChecks = results.filter { (_, result) -> result.status == HealthStatus.DOWN }
                .map { (name, _) -> name }
            return HealthResult.down(
                mapOf(
                    "message" to "Some health checks are down",
                    "down_checks" to downChecks.joinToString(", ")
                )
            )
        }

        val hasDegraded = results.any { (_, result) -> result.status == HealthStatus.DEGRADED }
        if (hasDegraded) {
            val degradedChecks = results.filter { (_, result) -> result.status == HealthStatus.DEGRADED }
                .map { (name, _) -> name }
            return HealthResult.degraded(
                mapOf(
                    "message" to "Some health checks are degraded",
                    "degraded_checks" to degradedChecks.joinToString(", ")
                )
            )
        }

        return HealthResult.up(mapOf("message" to "All health checks are up"))
    }
}
