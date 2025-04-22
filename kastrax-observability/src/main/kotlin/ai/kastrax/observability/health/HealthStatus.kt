package ai.kastrax.observability.health

/**
 * 健康状态枚举。
 * 表示组件或服务的健康状态。
 */
enum class HealthStatus {
    /**
     * 组件或服务运行正常。
     */
    UP,

    /**
     * 组件或服务运行不正常，但仍然可用。
     */
    DEGRADED,

    /**
     * 组件或服务不可用。
     */
    DOWN;

    /**
     * 检查健康状态是否表示服务可用。
     *
     * @return 如果状态为 UP 或 DEGRADED 则返回 true，否则返回 false
     */
    fun isAvailable(): Boolean {
        return this == UP || this == DEGRADED
    }
}
