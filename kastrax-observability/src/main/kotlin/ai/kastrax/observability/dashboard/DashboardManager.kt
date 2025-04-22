package ai.kastrax.observability.dashboard

import java.util.concurrent.ConcurrentHashMap

/**
 * 仪表板管理器。
 * 用于管理和访问仪表板。
 */
class DashboardManager {
    private val dashboards = ConcurrentHashMap<String, Dashboard>()

    /**
     * 注册仪表板。
     *
     * @param dashboard 要注册的仪表板
     * @return 当前管理器实例，用于链式调用
     */
    fun register(dashboard: Dashboard): DashboardManager {
        dashboards[dashboard.getName()] = dashboard
        return this
    }

    /**
     * 取消注册仪表板。
     *
     * @param name 要取消注册的仪表板名称
     * @return 当前管理器实例，用于链式调用
     */
    fun unregister(name: String): DashboardManager {
        dashboards.remove(name)
        return this
    }

    /**
     * 获取所有仪表板。
     *
     * @return 所有仪表板的映射
     */
    fun getDashboards(): Map<String, Dashboard> {
        return dashboards.toMap()
    }

    /**
     * 获取指定名称的仪表板。
     *
     * @param name 仪表板名称
     * @return 仪表板，如果找不到则返回null
     */
    fun getDashboard(name: String): Dashboard? {
        return dashboards[name]
    }

    /**
     * 获取指定类型的仪表板。
     *
     * @param type 仪表板类型
     * @return 指定类型的仪表板列表
     */
    fun getDashboardsByType(type: DashboardType): List<Dashboard> {
        return dashboards.values.filter { it.getType() == type }
    }

    /**
     * 导出所有仪表板配置。
     *
     * @return 所有仪表板配置的JSON字符串
     */
    fun exportAllConfigs(): String {
        val configs = dashboards.values.associate { it.getName() to it.exportConfig() }
        return buildJsonObject(configs)
    }

    /**
     * 导出指定类型的仪表板配置。
     *
     * @param type 仪表板类型
     * @return 指定类型的仪表板配置的JSON字符串
     */
    fun exportConfigsByType(type: DashboardType): String {
        val configs = getDashboardsByType(type).associate { it.getName() to it.exportConfig() }
        return buildJsonObject(configs)
    }

    private fun buildJsonObject(map: Map<String, String>): String {
        val entries = map.entries.joinToString(",") { (key, value) ->
            "\"$key\":$value"
        }
        return "{$entries}"
    }
}
