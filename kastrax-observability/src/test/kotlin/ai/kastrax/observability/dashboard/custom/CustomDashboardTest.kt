package ai.kastrax.observability.dashboard.custom

import ai.kastrax.observability.dashboard.DashboardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomDashboardTest {
    @Test
    fun testCustomDashboard() {
        // 创建测试小部件
        val widget = DashboardWidget(
            id = "test-widget",
            title = "Test Widget",
            type = WidgetType.CHART,
            x = 0,
            y = 0,
            width = 6,
            height = 4,
            dataSource = "test-source",
            config = mapOf(
                "metric" to "test.metric",
                "chartType" to "line"
            )
        )

        // 创建仪表板配置
        val config = CustomDashboardConfig(
            title = "Test Dashboard",
            id = "test-dashboard",
            theme = "dark",
            refreshInterval = 15,
            widgets = listOf(widget)
        )

        // 创建仪表板
        val dashboard = CustomDashboard(
            name = "test_dashboard",
            description = "Test Dashboard Description",
            url = "/dashboards/test-dashboard",
            id = "test-dashboard",
            config = config
        )

        // 验证仪表板属性
        assertEquals("test_dashboard", dashboard.getName())
        assertEquals("Test Dashboard Description", dashboard.getDescription())
        assertEquals("/dashboards/test-dashboard", dashboard.getUrl())
        assertEquals(DashboardType.CUSTOM, dashboard.getType())
        assertEquals("test-dashboard", dashboard.getId())
        assertEquals(config, dashboard.getConfig())

        // 验证导出配置
        val exportedConfig = dashboard.exportConfig()
        
        // 验证关键字段存在
        assertTrue(exportedConfig.contains("title"), "Config should contain title field")
        assertTrue(exportedConfig.contains("Test Dashboard"), "Config should contain dashboard title")
        assertTrue(exportedConfig.contains("id"), "Config should contain id field")
        assertTrue(exportedConfig.contains("test-dashboard"), "Config should contain dashboard id")
        assertTrue(exportedConfig.contains("theme"), "Config should contain theme field")
        assertTrue(exportedConfig.contains("dark"), "Config should contain theme value")
        assertTrue(exportedConfig.contains("refreshInterval"), "Config should contain refreshInterval field")
        assertTrue(exportedConfig.contains("15"), "Config should contain refreshInterval value")
        assertTrue(exportedConfig.contains("widgets"), "Config should contain widgets field")
        
        // 验证小部件字段
        assertTrue(exportedConfig.contains("test-widget"), "Config should contain widget id")
        assertTrue(exportedConfig.contains("Test Widget"), "Config should contain widget title")
        assertTrue(exportedConfig.contains("chart"), "Config should contain widget type")
        assertTrue(exportedConfig.contains("test-source"), "Config should contain data source")
        assertTrue(exportedConfig.contains("test.metric"), "Config should contain metric")
        assertTrue(exportedConfig.contains("line"), "Config should contain chart type")
    }

    @Test
    fun testWidgetToJson() {
        // 创建测试小部件
        val widget = DashboardWidget(
            id = "test-widget",
            title = "Test Widget",
            type = WidgetType.CHART,
            x = 0,
            y = 0,
            width = 6,
            height = 4,
            dataSource = "test-source",
            config = mapOf(
                "metric" to "test.metric",
                "chartType" to "line",
                "enabled" to true,
                "limit" to 100
            )
        )

        // 验证JSON转换
        val json = widget.toJson()
        
        // 验证关键字段存在
        assertTrue(json.contains("id"), "JSON should contain id field")
        assertTrue(json.contains("test-widget"), "JSON should contain widget id")
        assertTrue(json.contains("title"), "JSON should contain title field")
        assertTrue(json.contains("Test Widget"), "JSON should contain widget title")
        assertTrue(json.contains("type"), "JSON should contain type field")
        assertTrue(json.contains("chart"), "JSON should contain chart type")
        assertTrue(json.contains("x"), "JSON should contain x field")
        assertTrue(json.contains("y"), "JSON should contain y field")
        assertTrue(json.contains("width"), "JSON should contain width field")
        assertTrue(json.contains("height"), "JSON should contain height field")
        assertTrue(json.contains("dataSource"), "JSON should contain dataSource field")
        assertTrue(json.contains("test-source"), "JSON should contain data source")
        assertTrue(json.contains("config"), "JSON should contain config field")
        assertTrue(json.contains("metric"), "JSON should contain metric field")
        assertTrue(json.contains("test.metric"), "JSON should contain metric value")
        assertTrue(json.contains("chartType"), "JSON should contain chartType field")
        assertTrue(json.contains("line"), "JSON should contain chart type value")
        assertTrue(json.contains("enabled"), "JSON should contain enabled field")
        assertTrue(json.contains("true"), "JSON should contain enabled value")
        assertTrue(json.contains("limit"), "JSON should contain limit field")
        assertTrue(json.contains("100"), "JSON should contain limit value")
    }
}
