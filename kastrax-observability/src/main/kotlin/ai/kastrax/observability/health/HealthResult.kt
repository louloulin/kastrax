package ai.kastrax.observability.health

/**
 * 健康检查结果类。
 *
 * @property status 健康状态
 * @property details 健康检查详情
 * @property error 错误信息，如果有的话
 */
data class HealthResult(
    val status: HealthStatus,
    val details: Map<String, Any> = emptyMap(),
    val error: Throwable? = null
) {
    companion object {
        /**
         * 创建一个表示健康的结果。
         *
         * @param details 健康检查详情
         * @return 健康检查结果
         */
        fun up(details: Map<String, Any> = emptyMap()): HealthResult {
            return HealthResult(HealthStatus.UP, details)
        }

        /**
         * 创建一个表示性能下降的结果。
         *
         * @param details 健康检查详情
         * @param error 错误信息，如果有的话
         * @return 健康检查结果
         */
        fun degraded(details: Map<String, Any> = emptyMap(), error: Throwable? = null): HealthResult {
            return HealthResult(HealthStatus.DEGRADED, details, error)
        }

        /**
         * 创建一个表示不可用的结果。
         *
         * @param details 健康检查详情
         * @param error 错误信息，如果有的话
         * @return 健康检查结果
         */
        fun down(details: Map<String, Any> = emptyMap(), error: Throwable? = null): HealthResult {
            return HealthResult(HealthStatus.DOWN, details, error)
        }
    }
}
