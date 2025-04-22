package ai.kastrax.observability.health

/**
 * 健康检查接口。
 * 用于检查组件或服务的健康状态。
 */
interface HealthCheck {
    /**
     * 获取健康检查的名称。
     *
     * @return 健康检查的名称
     */
    fun getName(): String

    /**
     * 执行健康检查。
     *
     * @return 健康检查结果
     */
    fun check(): HealthResult

    /**
     * 获取健康检查的类型。
     *
     * @return 健康检查的类型
     */
    fun getType(): HealthCheckType = HealthCheckType.COMPONENT
}

/**
 * 健康检查类型枚举。
 */
enum class HealthCheckType {
    /**
     * 组件健康检查。
     */
    COMPONENT,

    /**
     * 依赖服务健康检查。
     */
    DEPENDENCY
}
