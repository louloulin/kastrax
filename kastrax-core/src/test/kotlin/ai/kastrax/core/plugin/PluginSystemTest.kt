package ai.kastrax.core.plugin

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 插件系统测试。
 */
class PluginSystemTest {
    private lateinit var pluginManager: DefaultPluginManager
    
    @BeforeEach
    fun setUp() {
        pluginManager = DefaultPluginManager()
    }
    
    @AfterEach
    fun tearDown() {
        pluginManager.shutdown()
    }
    
    @Test
    fun `test plugin lifecycle`() {
        // 创建测试插件
        val plugin = TestPlugin()
        
        // 注册插件
        val registered = pluginManager.registerPlugin(plugin)
        assertTrue(registered)
        
        // 获取插件信息
        val pluginInfo = pluginManager.getPluginInfo(plugin.id)
        assertNotNull(pluginInfo)
        assertEquals(PluginStatus.REGISTERED, pluginInfo!!.status)
        
        // 加载插件
        val loaded = pluginManager.loadPlugin(plugin.id)
        assertTrue(loaded)
        assertEquals(PluginStatus.INITIALIZED, pluginManager.getPluginInfo(plugin.id)!!.status)
        
        // 启动插件
        val started = pluginManager.startPlugin(plugin.id)
        assertTrue(started)
        assertEquals(PluginStatus.RUNNING, pluginManager.getPluginInfo(plugin.id)!!.status)
        
        // 停止插件
        val stopped = pluginManager.stopPlugin(plugin.id)
        assertTrue(stopped)
        assertEquals(PluginStatus.STOPPED, pluginManager.getPluginInfo(plugin.id)!!.status)
        
        // 卸载插件
        val unloaded = pluginManager.unloadPlugin(plugin.id)
        assertTrue(unloaded)
        assertFalse(pluginManager.getAllPlugins().any { it.metadata.id == plugin.id })
    }
    
    @Test
    fun `test plugin manager operations`() {
        // 创建测试插件
        val plugin1 = TestPlugin("test-plugin-1", "Test Plugin 1", PluginType.TOOL)
        val plugin2 = TestPlugin("test-plugin-2", "Test Plugin 2", PluginType.WORKFLOW_STEP)
        val plugin3 = TestPlugin("test-plugin-3", "Test Plugin 3", PluginType.CONNECTOR)
        
        // 注册插件
        pluginManager.registerPlugin(plugin1)
        pluginManager.registerPlugin(plugin2)
        pluginManager.registerPlugin(plugin3)
        
        // 获取所有插件
        val allPlugins = pluginManager.getAllPlugins()
        assertEquals(3, allPlugins.size)
        
        // 获取指定类型的插件
        val toolPlugins = pluginManager.getPluginsByType(PluginType.TOOL)
        assertEquals(1, toolPlugins.size)
        assertEquals(plugin1.id, toolPlugins[0].metadata.id)
        
        val stepPlugins = pluginManager.getPluginsByType(PluginType.WORKFLOW_STEP)
        assertEquals(1, stepPlugins.size)
        assertEquals(plugin2.id, stepPlugins[0].metadata.id)
        
        val connectorPlugins = pluginManager.getPluginsByType(PluginType.CONNECTOR)
        assertEquals(1, connectorPlugins.size)
        assertEquals(plugin3.id, connectorPlugins[0].metadata.id)
        
        // 获取插件实例
        val plugin = pluginManager.getPlugin(plugin1.id)
        assertNotNull(plugin)
        assertEquals(plugin1.id, plugin!!.id)
    }
    
    @Test
    fun `test plugin context`() {
        // 创建测试插件
        val plugin = TestPlugin()
        
        // 注册插件
        pluginManager.registerPlugin(plugin)
        
        // 加载插件
        pluginManager.loadPlugin(plugin.id)
        
        // 启动插件
        pluginManager.startPlugin(plugin.id)
        
        // 验证插件上下文
        val context = plugin.getContext()
        assertNotNull(context)
        
        // 验证插件管理器
        val manager = context!!.getPluginManager()
        assertNotNull(manager)
        
        // 验证服务注册和获取
        val service = TestService()
        context.registerService(TestService::class.java, service)
        val retrievedService = context.getService(TestService::class.java)
        assertNotNull(retrievedService)
        assertEquals(service, retrievedService)
    }
    
    @Test
    fun `test plugin configuration`() {
        // 创建测试插件
        val config = DefaultPluginConfig()
        config.setProperty("key1", "value1")
        config.setProperty("key2", 42)
        config.setProperty("key3", true)
        
        val plugin = TestPlugin(config = config)
        
        // 注册插件
        pluginManager.registerPlugin(plugin)
        
        // 获取插件配置
        val pluginConfig = plugin.config
        assertNotNull(pluginConfig)
        
        // 验证配置属性
        assertEquals("value1", pluginConfig.getProperty("key1"))
        assertEquals(42, pluginConfig.getProperty("key2"))
        assertEquals(true, pluginConfig.getProperty("key3"))
        
        // 验证所有属性
        val allProperties = pluginConfig.getAllProperties()
        assertEquals(3, allProperties.size)
        assertEquals("value1", allProperties["key1"])
        assertEquals(42, allProperties["key2"])
        assertEquals(true, allProperties["key3"])
    }
}

/**
 * 测试插件，用于测试插件系统。
 */
class TestPlugin(
    id: String = "test-plugin",
    name: String = "Test Plugin",
    type: PluginType = PluginType.OTHER,
    config: PluginConfig = DefaultPluginConfig()
) : AbstractPlugin(
    id = id,
    name = name,
    description = "A test plugin for testing the plugin system",
    version = "1.0.0",
    author = "KastraX Team",
    type = type,
    config = config
) {
    private var context: PluginContext? = null
    
    override fun initialize(context: PluginContext): Boolean {
        this.context = context
        return true
    }
    
    override fun start(context: PluginContext): Boolean {
        return true
    }
    
    override fun stop(context: PluginContext): Boolean {
        return true
    }
    
    /**
     * 获取插件上下文。
     */
    fun getContext(): PluginContext? {
        return context
    }
}

/**
 * 测试服务，用于测试插件上下文。
 */
class TestService
