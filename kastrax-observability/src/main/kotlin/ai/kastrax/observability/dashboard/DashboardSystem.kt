package ai.kastrax.observability.dashboard

/**
 * 仪表板系统。
 * 提供集中管理仪表板的功能。
 */
object DashboardSystem {
    private val manager = DashboardManager()

    /**
     * 注册仪表板。
     *
     * @param dashboard 要注册的仪表板
     */
    fun registerDashboard(dashboard: Dashboard) {
        manager.register(dashboard)
    }

    /**
     * 取消注册仪表板。
     *
     * @param name 要取消注册的仪表板名称
     */
    fun unregisterDashboard(name: String) {
        manager.unregister(name)
    }

    /**
     * 获取所有仪表板。
     *
     * @return 所有仪表板的映射
     */
    fun getDashboards(): Map<String, Dashboard> {
        return manager.getDashboards()
    }

    /**
     * 获取指定名称的仪表板。
     *
     * @param name 仪表板名称
     * @return 仪表板，如果找不到则返回null
     */
    fun getDashboard(name: String): Dashboard? {
        return manager.getDashboard(name)
    }

    /**
     * 获取指定类型的仪表板。
     *
     * @param type 仪表板类型
     * @return 指定类型的仪表板列表
     */
    fun getDashboardsByType(type: DashboardType): List<Dashboard> {
        return manager.getDashboardsByType(type)
    }

    /**
     * 导出所有仪表板配置。
     *
     * @return 所有仪表板配置的JSON字符串
     */
    fun exportAllConfigs(): String {
        return manager.exportAllConfigs()
    }

    /**
     * 导出指定类型的仪表板配置。
     *
     * @param type 仪表板类型
     * @return 指定类型的仪表板配置的JSON字符串
     */
    fun exportConfigsByType(type: DashboardType): String {
        return manager.exportConfigsByType(type)
    }
}
