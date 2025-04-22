package ai.kastrax.observability.dashboard

/**
 * 仪表板接口。
 * 定义了仪表板的基本操作。
 */
interface Dashboard {
    /**
     * 获取仪表板名称。
     *
     * @return 仪表板名称
     */
    fun getName(): String

    /**
     * 获取仪表板描述。
     *
     * @return 仪表板描述
     */
    fun getDescription(): String

    /**
     * 获取仪表板URL。
     *
     * @return 仪表板URL
     */
    fun getUrl(): String

    /**
     * 获取仪表板类型。
     *
     * @return 仪表板类型
     */
    fun getType(): DashboardType

    /**
     * 导出仪表板配置。
     *
     * @return 仪表板配置的JSON字符串
     */
    fun exportConfig(): String
}

/**
 * 仪表板类型枚举。
 */
enum class DashboardType {
    /**
     * Grafana仪表板。
     */
    GRAFANA,

    /**
     * 自定义仪表板。
     */
    CUSTOM
}
