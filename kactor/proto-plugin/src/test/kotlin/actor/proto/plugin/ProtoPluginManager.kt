package actor.proto.plugin

import actor.proto.ActorSystem
import org.pf4j.DefaultPluginManager
import org.pf4j.PluginManager
import org.pf4j.PluginWrapper
import java.nio.file.Path

/**
 * 测试用的ProtoPluginManager类
 */
class ProtoPluginManager private constructor(pluginsRoot: Path) {
    val pluginManager: PluginManager = DefaultPluginManager(pluginsRoot)

    /**
     * 加载插件
     */
    fun loadPlugins() {
        pluginManager.loadPlugins()
    }

    /**
     * 启动插件
     */
    fun startPlugins() {
        pluginManager.startPlugins()
    }

    /**
     * 停止插件
     */
    fun stopPlugins() {
        pluginManager.stopPlugins()
    }

    /**
     * 初始化所有插件
     * @param system Actor系统
     */
    fun initPlugins(system: ActorSystem) {
        pluginManager.plugins.forEach { plugin ->
            // 使用反射调用init方法
            val plugin = plugin.plugin
            if (plugin is ProtoPlugin) {
                val initMethod = plugin.javaClass.getMethod("init", ActorSystem::class.java)
                initMethod.invoke(plugin, system)
            }
        }
    }

    /**
     * 获取所有插件
     * @return 插件列表
     */
    fun getPlugins(): List<ProtoPlugin> {
        return pluginManager.plugins
            .map { it.plugin }
            .filterIsInstance<ProtoPlugin>()
    }

    companion object {
        private var instance: ProtoPluginManager? = null

        /**
         * 获取插件管理器实例
         * @param pluginsRoot 插件根目录
         * @return 插件管理器实例
         */
        @JvmStatic
        fun getInstance(pluginsRoot: Path): ProtoPluginManager {
            if (instance == null) {
                instance = ProtoPluginManager(pluginsRoot)
            }
            return instance!!
        }
    }
}
