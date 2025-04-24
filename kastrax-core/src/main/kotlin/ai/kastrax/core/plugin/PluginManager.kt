package ai.kastrax.core.plugin

import ai.kastrax.core.common.KastraXBase
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import mu.KotlinLogging

/**
 * 插件管理器接口，负责插件的注册、加载、启动和停止。
 */
interface PluginManager {
    /**
     * 注册插件。
     *
     * @param plugin 插件实例
     * @return 注册是否成功
     */
    fun registerPlugin(plugin: Plugin): Boolean
    
    /**
     * 加载插件。
     *
     * @param pluginId 插件ID
     * @return 加载是否成功
     */
    fun loadPlugin(pluginId: String): Boolean
    
    /**
     * 启动插件。
     *
     * @param pluginId 插件ID
     * @return 启动是否成功
     */
    fun startPlugin(pluginId: String): Boolean
    
    /**
     * 停止插件。
     *
     * @param pluginId 插件ID
     * @return 停止是否成功
     */
    fun stopPlugin(pluginId: String): Boolean
    
    /**
     * 卸载插件。
     *
     * @param pluginId 插件ID
     * @return 卸载是否成功
     */
    fun unloadPlugin(pluginId: String): Boolean
    
    /**
     * 获取所有插件信息。
     *
     * @return 所有插件信息的列表
     */
    fun getAllPlugins(): List<PluginInfo>
    
    /**
     * 获取插件信息。
     *
     * @param pluginId 插件ID
     * @return 插件信息，如果不存在则返回null
     */
    fun getPluginInfo(pluginId: String): PluginInfo?
    
    /**
     * 获取插件实例。
     *
     * @param pluginId 插件ID
     * @return 插件实例，如果不存在则返回null
     */
    fun getPlugin(pluginId: String): Plugin?
    
    /**
     * 获取指定类型的所有插件。
     *
     * @param type 插件类型
     * @return 指定类型的所有插件信息
     */
    fun getPluginsByType(type: PluginType): List<PluginInfo>
    
    /**
     * 从目录加载插件。
     *
     * @param directory 插件目录
     * @return 加载的插件数量
     */
    fun loadPluginsFromDirectory(directory: File): Int
    
    /**
     * 从JAR文件加载插件。
     *
     * @param jarFile JAR文件
     * @return 加载的插件数量
     */
    fun loadPluginFromJar(jarFile: File): Int
    
    /**
     * 使用Java ServiceLoader加载插件。
     *
     * @return 加载的插件数量
     */
    fun loadPluginsWithServiceLoader(): Int
}

/**
 * 默认插件管理器实现。
 */
class DefaultPluginManager : PluginManager, KastraXBase(component = "PLUGIN_MANAGER", name = "DefaultPluginManager") {
    private val plugins = ConcurrentHashMap<String, Plugin>()
    private val pluginInfos = ConcurrentHashMap<String, PluginInfo>()
    private val pluginContext = DefaultPluginContext(this)
    private val classLoaders = mutableListOf<URLClassLoader>()
    
    override fun registerPlugin(plugin: Plugin): Boolean {
        if (plugins.containsKey(plugin.id)) {
            logger.warn { "插件已存在: ${plugin.id}" }
            return false
        }
        
        plugins[plugin.id] = plugin
        pluginInfos[plugin.id] = PluginInfo(
            metadata = plugin.getMetadata(),
            status = PluginStatus.REGISTERED
        )
        
        logger.info { "注册插件: ${plugin.name} (${plugin.id})" }
        return true
    }
    
    override fun loadPlugin(pluginId: String): Boolean {
        val plugin = plugins[pluginId] ?: return false
        val pluginInfo = pluginInfos[pluginId] ?: return false
        
        try {
            val success = plugin.initialize(pluginContext)
            if (success) {
                pluginInfo.status = PluginStatus.INITIALIZED
                logger.info { "加载插件成功: ${plugin.name} (${plugin.id})" }
            } else {
                pluginInfo.status = PluginStatus.ERROR
                pluginInfo.error = "初始化失败"
                logger.error { "加载插件失败: ${plugin.name} (${plugin.id})" }
            }
            return success
        } catch (e: Exception) {
            pluginInfo.status = PluginStatus.ERROR
            pluginInfo.error = e.message
            logger.error(e) { "加载插件异常: ${plugin.name} (${plugin.id})" }
            return false
        }
    }
    
    override fun startPlugin(pluginId: String): Boolean {
        val plugin = plugins[pluginId] ?: return false
        val pluginInfo = pluginInfos[pluginId] ?: return false
        
        if (pluginInfo.status != PluginStatus.INITIALIZED) {
            logger.warn { "插件未初始化，无法启动: ${plugin.name} (${plugin.id})" }
            return false
        }
        
        try {
            val success = plugin.start(pluginContext)
            if (success) {
                pluginInfo.status = PluginStatus.RUNNING
                logger.info { "启动插件成功: ${plugin.name} (${plugin.id})" }
            } else {
                pluginInfo.status = PluginStatus.ERROR
                pluginInfo.error = "启动失败"
                logger.error { "启动插件失败: ${plugin.name} (${plugin.id})" }
            }
            return success
        } catch (e: Exception) {
            pluginInfo.status = PluginStatus.ERROR
            pluginInfo.error = e.message
            logger.error(e) { "启动插件异常: ${plugin.name} (${plugin.id})" }
            return false
        }
    }
    
    override fun stopPlugin(pluginId: String): Boolean {
        val plugin = plugins[pluginId] ?: return false
        val pluginInfo = pluginInfos[pluginId] ?: return false
        
        if (pluginInfo.status != PluginStatus.RUNNING) {
            logger.warn { "插件未运行，无法停止: ${plugin.name} (${plugin.id})" }
            return false
        }
        
        try {
            val success = plugin.stop(pluginContext)
            if (success) {
                pluginInfo.status = PluginStatus.STOPPED
                logger.info { "停止插件成功: ${plugin.name} (${plugin.id})" }
            } else {
                pluginInfo.status = PluginStatus.ERROR
                pluginInfo.error = "停止失败"
                logger.error { "停止插件失败: ${plugin.name} (${plugin.id})" }
            }
            return success
        } catch (e: Exception) {
            pluginInfo.status = PluginStatus.ERROR
            pluginInfo.error = e.message
            logger.error(e) { "停止插件异常: ${plugin.name} (${plugin.id})" }
            return false
        }
    }
    
    override fun unloadPlugin(pluginId: String): Boolean {
        val plugin = plugins[pluginId] ?: return false
        val pluginInfo = pluginInfos[pluginId] ?: return false
        
        if (pluginInfo.status == PluginStatus.RUNNING) {
            stopPlugin(pluginId)
        }
        
        plugins.remove(pluginId)
        pluginInfos.remove(pluginId)
        
        logger.info { "卸载插件: ${plugin.name} (${plugin.id})" }
        return true
    }
    
    override fun getAllPlugins(): List<PluginInfo> {
        return pluginInfos.values.toList()
    }
    
    override fun getPluginInfo(pluginId: String): PluginInfo? {
        return pluginInfos[pluginId]
    }
    
    override fun getPlugin(pluginId: String): Plugin? {
        return plugins[pluginId]
    }
    
    override fun getPluginsByType(type: PluginType): List<PluginInfo> {
        return pluginInfos.values.filter { it.metadata.type == type }
    }
    
    override fun loadPluginsFromDirectory(directory: File): Int {
        if (!directory.exists() || !directory.isDirectory) {
            logger.warn { "插件目录不存在或不是目录: ${directory.absolutePath}" }
            return 0
        }
        
        var count = 0
        directory.listFiles { file -> file.isFile && file.name.endsWith(".jar") }?.forEach { jarFile ->
            count += loadPluginFromJar(jarFile)
        }
        
        logger.info { "从目录加载插件: ${directory.absolutePath}, 加载了 $count 个插件" }
        return count
    }
    
    override fun loadPluginFromJar(jarFile: File): Int {
        if (!jarFile.exists() || !jarFile.isFile || !jarFile.name.endsWith(".jar")) {
            logger.warn { "JAR文件不存在或不是JAR文件: ${jarFile.absolutePath}" }
            return 0
        }
        
        try {
            val url = jarFile.toURI().toURL()
            val classLoader = URLClassLoader(arrayOf(url), this.javaClass.classLoader)
            classLoaders.add(classLoader)
            
            val serviceLoader = ServiceLoader.load(Plugin::class.java, classLoader)
            var count = 0
            
            serviceLoader.forEach { plugin ->
                if (registerPlugin(plugin)) {
                    if (loadPlugin(plugin.id)) {
                        count++
                    }
                }
            }
            
            logger.info { "从JAR加载插件: ${jarFile.absolutePath}, 加载了 $count 个插件" }
            return count
        } catch (e: Exception) {
            logger.error(e) { "加载JAR插件异常: ${jarFile.absolutePath}" }
            return 0
        }
    }
    
    override fun loadPluginsWithServiceLoader(): Int {
        val serviceLoader = ServiceLoader.load(Plugin::class.java)
        var count = 0
        
        serviceLoader.forEach { plugin ->
            if (registerPlugin(plugin)) {
                if (loadPlugin(plugin.id)) {
                    count++
                }
            }
        }
        
        logger.info { "使用ServiceLoader加载插件, 加载了 $count 个插件" }
        return count
    }
    
    /**
     * 关闭插件管理器，停止所有插件。
     */
    fun shutdown() {
        logger.info { "关闭插件管理器，停止所有插件..." }
        
        // 按照依赖关系的反序停止插件
        val runningPlugins = pluginInfos.values
            .filter { it.status == PluginStatus.RUNNING }
            .map { it.metadata.id }
        
        runningPlugins.forEach { pluginId ->
            stopPlugin(pluginId)
        }
        
        // 关闭所有类加载器
        classLoaders.forEach { it.close() }
        classLoaders.clear()
        
        logger.info { "插件管理器已关闭" }
    }
}

/**
 * 默认插件上下文实现。
 */
class DefaultPluginContext(private val pluginManager: PluginManager) : PluginContext {
    private val services = ConcurrentHashMap<Class<*>, Any>()
    
    override fun getPluginManager(): PluginManager {
        return pluginManager
    }
    
    override fun getPluginConfig(pluginId: String): PluginConfig? {
        return pluginManager.getPlugin(pluginId)?.config
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getService(serviceClass: Class<T>): T? {
        return services[serviceClass] as T?
    }
    
    override fun <T : Any> registerService(serviceClass: Class<T>, service: T) {
        services[serviceClass] = service
    }
}
