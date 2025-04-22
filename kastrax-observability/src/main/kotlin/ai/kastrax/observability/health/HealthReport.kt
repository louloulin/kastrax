package ai.kastrax.observability.health

import java.time.Instant

/**
 * 健康检查报告类。
 * 包含系统整体健康状态和各个组件的健康检查结果。
 *
 * @property status 整体健康状态
 * @property checks 各个组件的健康检查结果
 * @property timestamp 报告生成时间戳
 */
data class HealthReport(
    val status: HealthStatus,
    val checks: Map<String, HealthResult>,
    val timestamp: Instant = Instant.now()
) {
    /**
     * 检查系统是否可用。
     *
     * @return 如果系统可用则返回 true，否则返回 false
     */
    fun isAvailable(): Boolean {
        return status.isAvailable()
    }

    /**
     * 获取不健康的检查结果。
     *
     * @return 不健康的检查结果映射
     */
    fun getUnhealthyChecks(): Map<String, HealthResult> {
        return checks.filter { (_, result) -> result.status != HealthStatus.UP }
    }

    /**
     * 获取指定类型的检查结果。
     *
     * @param type 健康检查类型
     * @return 指定类型的检查结果映射
     */
    fun getChecksByType(type: HealthCheckType): Map<String, HealthResult> {
        return checks.filter { (name, _) ->
            val healthCheck = HealthCheckSystem.getHealthChecks()[name]
            healthCheck?.getType() == type
        }
    }

    /**
     * 将健康报告转换为 JSON 格式的字符串。
     *
     * @return JSON 格式的健康报告
     */
    fun toJson(): String {
        val checksJson = checks.entries.joinToString(",") { (name, result) ->
            val detailsJson = result.details.entries.joinToString(",") { (key, value) ->
                "\"$key\":\"$value\""
            }
            val errorJson = result.error?.let { "\"error\":\"${it.message?.replace("\"", "\\\"")}\"" } ?: ""
            val separator = if (detailsJson.isNotEmpty() && errorJson.isNotEmpty()) "," else ""
            "\"$name\":{\"status\":\"${result.status}\",${if (detailsJson.isNotEmpty()) "\"details\":{$detailsJson}" else ""}$separator$errorJson}"
        }
        return "{\"status\":\"$status\",\"timestamp\":\"$timestamp\",\"checks\":{$checksJson}}"
    }
}
