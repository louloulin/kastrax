package ai.kastrax.observability.dashboard

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DashboardManagerTest {
    private lateinit var manager: DashboardManager

    @BeforeEach
    fun setUp() {
        manager = DashboardManager()
    }

    @Test
    fun testRegisterAndUnregister() {
        // 创建测试仪表板
        val dashboard = object : Dashboard {
            override fun getName(): String = "test"
            override fun getDescription(): String = "Test Dashboard"
            override fun getUrl(): String = "http://example.com/test"
            override fun getType(): DashboardType = DashboardType.CUSTOM
            override fun exportConfig(): String = "{}"
        }

        // 注册仪表板
        manager.register(dashboard)

        // 验证仪表板已注册
        val dashboards = manager.getDashboards()
        assertEquals(1, dashboards.size)
        assertTrue(dashboards.containsKey("test"))
        assertEquals(dashboard, dashboards["test"])

        // 取消注册仪表板
        manager.unregister("test")

        // 验证仪表板已取消注册
        val updatedDashboards = manager.getDashboards()
        assertEquals(0, updatedDashboards.size)
        assertTrue(updatedDashboards.isEmpty())
    }

    @Test
    fun testGetDashboard() {
        // 创建测试仪表板
        val dashboard = object : Dashboard {
            override fun getName(): String = "test"
            override fun getDescription(): String = "Test Dashboard"
            override fun getUrl(): String = "http://example.com/test"
            override fun getType(): DashboardType = DashboardType.CUSTOM
            override fun exportConfig(): String = "{}"
        }

        // 注册仪表板
        manager.register(dashboard)

        // 获取仪表板
        val result = manager.getDashboard("test")
        assertNotNull(result)
        assertEquals("test", result.getName())
        assertEquals("Test Dashboard", result.getDescription())
        assertEquals("http://example.com/test", result.getUrl())
        assertEquals(DashboardType.CUSTOM, result.getType())

        // 获取不存在的仪表板
        val nonExistent = manager.getDashboard("non_existent")
        assertNull(nonExistent)
    }

    @Test
    fun testGetDashboardsByType() {
        // 创建测试仪表板
        val customDashboard = object : Dashboard {
            override fun getName(): String = "custom"
            override fun getDescription(): String = "Custom Dashboard"
            override fun getUrl(): String = "http://example.com/custom"
            override fun getType(): DashboardType = DashboardType.CUSTOM
            override fun exportConfig(): String = "{}"
        }

        val grafanaDashboard = object : Dashboard {
            override fun getName(): String = "grafana"
            override fun getDescription(): String = "Grafana Dashboard"
            override fun getUrl(): String = "http://example.com/grafana"
            override fun getType(): DashboardType = DashboardType.GRAFANA
            override fun exportConfig(): String = "{}"
        }

        // 注册仪表板
        manager.register(customDashboard)
        manager.register(grafanaDashboard)

        // 获取自定义仪表板
        val customDashboards = manager.getDashboardsByType(DashboardType.CUSTOM)
        assertEquals(1, customDashboards.size)
        assertEquals("custom", customDashboards[0].getName())

        // 获取Grafana仪表板
        val grafanaDashboards = manager.getDashboardsByType(DashboardType.GRAFANA)
        assertEquals(1, grafanaDashboards.size)
        assertEquals("grafana", grafanaDashboards[0].getName())
    }

    @Test
    fun testExportAllConfigs() {
        // 创建测试仪表板
        val dashboard1 = object : Dashboard {
            override fun getName(): String = "dashboard1"
            override fun getDescription(): String = "Dashboard 1"
            override fun getUrl(): String = "http://example.com/dashboard1"
            override fun getType(): DashboardType = DashboardType.CUSTOM
            override fun exportConfig(): String = """{"name":"dashboard1"}"""
        }

        val dashboard2 = object : Dashboard {
            override fun getName(): String = "dashboard2"
            override fun getDescription(): String = "Dashboard 2"
            override fun getUrl(): String = "http://example.com/dashboard2"
            override fun getType(): DashboardType = DashboardType.GRAFANA
            override fun exportConfig(): String = """{"name":"dashboard2"}"""
        }

        // 注册仪表板
        manager.register(dashboard1)
        manager.register(dashboard2)

        // 导出所有配置
        val configs = manager.exportAllConfigs()
        assertTrue(configs.contains(""""dashboard1":{"name":"dashboard1"}"""))
        assertTrue(configs.contains(""""dashboard2":{"name":"dashboard2"}"""))
    }

    @Test
    fun testExportConfigsByType() {
        // 创建测试仪表板
        val customDashboard = object : Dashboard {
            override fun getName(): String = "custom"
            override fun getDescription(): String = "Custom Dashboard"
            override fun getUrl(): String = "http://example.com/custom"
            override fun getType(): DashboardType = DashboardType.CUSTOM
            override fun exportConfig(): String = """{"name":"custom"}"""
        }

        val grafanaDashboard = object : Dashboard {
            override fun getName(): String = "grafana"
            override fun getDescription(): String = "Grafana Dashboard"
            override fun getUrl(): String = "http://example.com/grafana"
            override fun getType(): DashboardType = DashboardType.GRAFANA
            override fun exportConfig(): String = """{"name":"grafana"}"""
        }

        // 注册仪表板
        manager.register(customDashboard)
        manager.register(grafanaDashboard)

        // 导出自定义仪表板配置
        val customConfigs = manager.exportConfigsByType(DashboardType.CUSTOM)
        assertTrue(customConfigs.contains(""""custom":{"name":"custom"}"""))
        assertTrue(!customConfigs.contains("grafana"))

        // 导出Grafana仪表板配置
        val grafanaConfigs = manager.exportConfigsByType(DashboardType.GRAFANA)
        assertTrue(grafanaConfigs.contains(""""grafana":{"name":"grafana"}"""))
        assertTrue(!grafanaConfigs.contains("custom"))
    }
}
