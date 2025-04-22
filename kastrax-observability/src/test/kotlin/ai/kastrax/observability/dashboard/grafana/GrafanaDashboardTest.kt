package ai.kastrax.observability.dashboard.grafana

import ai.kastrax.observability.dashboard.DashboardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GrafanaDashboardTest {
    @Test
    fun testGrafanaDashboard() {
        // 创建测试面板
        val target = GrafanaTarget(
            refId = "A",
            expr = "test_metric",
            legendFormat = "Test Metric"
        )

        val panel = GrafanaPanel(
            id = 1,
            title = "Test Panel",
            type = "graph",
            datasource = "prometheus",
            targets = listOf(target)
        )

        // 创建测试变量
        val variable = GrafanaVariable(
            name = "test_var",
            label = "Test Variable",
            query = "label_values(test_metric, instance)"
        )

        // 创建仪表板配置
        val config = GrafanaDashboardConfig(
            title = "Test Dashboard",
            uid = "test-dashboard",
            tags = listOf("test", "example"),
            timezone = "utc",
            panels = listOf(panel),
            variables = listOf(variable)
        )

        // 创建仪表板
        val dashboard = GrafanaDashboard(
            name = "test_dashboard",
            description = "Test Dashboard Description",
            url = "http://grafana.example.com/d/test-dashboard",
            uid = "test-dashboard",
            config = config
        )

        // 验证仪表板属性
        assertEquals("test_dashboard", dashboard.getName())
        assertEquals("Test Dashboard Description", dashboard.getDescription())
        assertEquals("http://grafana.example.com/d/test-dashboard", dashboard.getUrl())
        assertEquals(DashboardType.GRAFANA, dashboard.getType())
        assertEquals("test-dashboard", dashboard.getUid())
        assertEquals(config, dashboard.getConfig())

        // 验证导出配置
        val exportedConfig = dashboard.exportConfig()
        
        // 验证关键字段存在
        assertTrue(exportedConfig.contains("title"), "Config should contain title field")
        assertTrue(exportedConfig.contains("Test Dashboard"), "Config should contain dashboard title")
        assertTrue(exportedConfig.contains("uid"), "Config should contain uid field")
        assertTrue(exportedConfig.contains("test-dashboard"), "Config should contain dashboard uid")
        assertTrue(exportedConfig.contains("tags"), "Config should contain tags field")
        assertTrue(exportedConfig.contains("test"), "Config should contain tag value")
        assertTrue(exportedConfig.contains("example"), "Config should contain tag value")
        assertTrue(exportedConfig.contains("timezone"), "Config should contain timezone field")
        assertTrue(exportedConfig.contains("utc"), "Config should contain timezone value")
        assertTrue(exportedConfig.contains("panels"), "Config should contain panels field")
        
        // 验证面板字段
        assertTrue(exportedConfig.contains("id"), "Config should contain panel id field")
        assertTrue(exportedConfig.contains("Test Panel"), "Config should contain panel title")
        assertTrue(exportedConfig.contains("graph"), "Config should contain panel type")
        assertTrue(exportedConfig.contains("prometheus"), "Config should contain datasource")
        assertTrue(exportedConfig.contains("targets"), "Config should contain targets field")
        
        // 验证目标字段
        assertTrue(exportedConfig.contains("refId"), "Config should contain refId field")
        assertTrue(exportedConfig.contains("A"), "Config should contain refId value")
        assertTrue(exportedConfig.contains("expr"), "Config should contain expr field")
        assertTrue(exportedConfig.contains("test_metric"), "Config should contain expr value")
        assertTrue(exportedConfig.contains("legendFormat"), "Config should contain legendFormat field")
        assertTrue(exportedConfig.contains("Test Metric"), "Config should contain legendFormat value")
        
        // 验证变量字段
        assertTrue(exportedConfig.contains("templating"), "Config should contain templating field")
        assertTrue(exportedConfig.contains("list"), "Config should contain list field")
        assertTrue(exportedConfig.contains("test_var"), "Config should contain variable name")
        assertTrue(exportedConfig.contains("Test Variable"), "Config should contain variable label")
        assertTrue(exportedConfig.contains("label_values"), "Config should contain query value")
    }

    @Test
    fun testPanelToJson() {
        // 创建测试目标
        val target = GrafanaTarget(
            refId = "A",
            expr = "test_metric",
            legendFormat = "Test Metric"
        )

        // 创建测试面板
        val panel = GrafanaPanel(
            id = 1,
            title = "Test Panel",
            type = "graph",
            datasource = "prometheus",
            targets = listOf(target)
        )

        // 验证JSON转换
        val json = panel.toJson()
        
        // 验证关键字段存在
        assertTrue(json.contains("id"), "JSON should contain id field")
        assertTrue(json.contains("title"), "JSON should contain title field")
        assertTrue(json.contains("Test Panel"), "JSON should contain panel title")
        assertTrue(json.contains("type"), "JSON should contain type field")
        assertTrue(json.contains("graph"), "JSON should contain panel type")
        assertTrue(json.contains("datasource"), "JSON should contain datasource field")
        assertTrue(json.contains("prometheus"), "JSON should contain datasource value")
        assertTrue(json.contains("targets"), "JSON should contain targets field")
        assertTrue(json.contains("refId"), "JSON should contain refId field")
        assertTrue(json.contains("A"), "JSON should contain refId value")
        assertTrue(json.contains("expr"), "JSON should contain expr field")
        assertTrue(json.contains("test_metric"), "JSON should contain expr value")
        assertTrue(json.contains("legendFormat"), "JSON should contain legendFormat field")
        assertTrue(json.contains("Test Metric"), "JSON should contain legendFormat value")
    }

    @Test
    fun testTargetToJson() {
        // 创建测试目标
        val target = GrafanaTarget(
            refId = "A",
            expr = "test_metric",
            legendFormat = "Test Metric"
        )

        // 验证JSON转换
        val json = target.toJson()
        
        // 验证关键字段存在
        assertTrue(json.contains("refId"), "JSON should contain refId field")
        assertTrue(json.contains("A"), "JSON should contain refId value")
        assertTrue(json.contains("expr"), "JSON should contain expr field")
        assertTrue(json.contains("test_metric"), "JSON should contain expr value")
        assertTrue(json.contains("legendFormat"), "JSON should contain legendFormat field")
        assertTrue(json.contains("Test Metric"), "JSON should contain legendFormat value")
    }

    @Test
    fun testVariableToJson() {
        // 创建测试变量
        val variable = GrafanaVariable(
            name = "test_var",
            label = "Test Variable",
            query = "label_values(test_metric, instance)"
        )

        // 验证JSON转换
        val json = variable.toJson()
        
        // 验证关键字段存在
        assertTrue(json.contains("name"), "JSON should contain name field")
        assertTrue(json.contains("test_var"), "JSON should contain name value")
        assertTrue(json.contains("label"), "JSON should contain label field")
        assertTrue(json.contains("Test Variable"), "JSON should contain label value")
        assertTrue(json.contains("query"), "JSON should contain query field")
        assertTrue(json.contains("label_values"), "JSON should contain query value")
        assertTrue(json.contains("type"), "JSON should contain type field")
        assertTrue(json.contains("query"), "JSON should contain type value")
    }
}
