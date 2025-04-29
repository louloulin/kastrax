package actor.proto.plugin

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.Receive
import actor.proto.ReceiveMiddleware
import actor.proto.Send
import actor.proto.SenderContext
import actor.proto.SenderMiddleware
import actor.proto.plugin.ActorDecoratorPluginInterface
import actor.proto.plugin.PluginInterface
import actor.proto.plugin.PropsDecoratorPluginInterface
import actor.proto.plugin.ReceiveMiddlewarePluginInterface
import actor.proto.plugin.SenderMiddlewarePluginInterface
import org.pf4j.ExtensionPoint
import org.pf4j.Plugin
import org.pf4j.PluginManager
import org.pf4j.PluginWrapper
import org.pf4j.DefaultPluginManager
import java.nio.file.Path
import java.nio.file.Paths

/**
 * ProtoActor插件基类
 * 所有插件必须继承此类
 */
abstract class ProtoPlugin(wrapper: PluginWrapper) : Plugin(wrapper), PluginInterface {
    /**
     * 获取插件ID
     * @return 插件ID
     */
    override fun id(): String = wrapper.pluginId

    /**
     * 获取插件版本
     * @return 插件版本
     */
    override fun version(): String = wrapper.descriptor.version

    /**
     * 初始化插件
     * @param system Actor系统
     */
    override open fun init(system: ActorSystem) {
        // 默认实现为空
    }

    /**
     * 关闭插件
     */
    override fun shutdown() {
        // 默认实现为空
    }
}

/**
 * 接收中间件扩展点
 * 用于添加接收中间件
 */
interface ReceiveMiddlewareExtension : ExtensionPoint, ReceiveMiddlewarePluginInterface {
    /**
     * 获取接收中间件
     * @return 接收中间件
     */
    fun getReceiveMiddleware(): ReceiveMiddleware

    /**
     * 获取接收中间件
     * @return 接收中间件
     */
    override fun receiveMiddleware(): ReceiveMiddleware = getReceiveMiddleware()
}

/**
 * 发送中间件扩展点
 * 用于添加发送中间件
 */
interface SenderMiddlewareExtension : ExtensionPoint, SenderMiddlewarePluginInterface {
    /**
     * 获取发送中间件
     * @return 发送中间件
     */
    fun getSenderMiddleware(): SenderMiddleware

    /**
     * 获取发送中间件
     * @return 发送中间件
     */
    override fun senderMiddleware(): SenderMiddleware = getSenderMiddleware()
}

/**
 * Actor装饰器扩展点
 * 用于装饰Actor
 */
interface ActorDecoratorExtension : ExtensionPoint, ActorDecoratorPluginInterface {
    /**
     * 装饰Actor
     * @param actor 原始Actor
     * @return 装饰后的Actor
     */
    override fun decorateActor(actor: Actor): Actor
}

/**
 * Props装饰器扩展点
 * 用于装饰Props
 */
interface PropsDecoratorExtension : ExtensionPoint, PropsDecoratorPluginInterface {
    /**
     * 装饰Props
     * @param props 原始Props
     * @return 装饰后的Props
     */
    override fun decorateProps(props: Props): Props
}

/**
 * ProtoActor插件管理器
 * 负责加载和管理插件
 */
class ProtoPluginManager private constructor(pluginsRoot: Path) {
    private val pluginManager: PluginManager = DefaultPluginManager(pluginsRoot)

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
     * 获取所有接收中间件
     * @return 接收中间件列表
     */
    fun getReceiveMiddlewares(): List<ReceiveMiddleware> {
        return pluginManager.getExtensions(ReceiveMiddlewareExtension::class.java)
            .map { it.getReceiveMiddleware() }
    }

    /**
     * 获取所有发送中间件
     * @return 发送中间件列表
     */
    fun getSenderMiddlewares(): List<SenderMiddleware> {
        return pluginManager.getExtensions(SenderMiddlewareExtension::class.java)
            .map { it.getSenderMiddleware() }
    }

    /**
     * 装饰Actor
     * @param actor 原始Actor
     * @return 装饰后的Actor
     */
    fun decorateActor(actor: Actor): Actor {
        var result = actor
        pluginManager.getExtensions(ActorDecoratorExtension::class.java)
            .forEach { result = it.decorateActor(result) }
        return result
    }

    /**
     * 装饰Props
     * @param props 原始Props
     * @return 装饰后的Props
     */
    fun decorateProps(props: Props): Props {
        var result = props
        pluginManager.getExtensions(PropsDecoratorExtension::class.java)
            .forEach { result = it.decorateProps(result) }
        return result
    }

    /**
     * 初始化所有插件
     * @param system Actor系统
     */
    fun initPlugins(system: ActorSystem) {
        pluginManager.plugins.forEach { plugin ->
            (plugin.plugin as? ProtoPlugin)?.init(system)
        }
    }

    companion object {
        private var instance: ProtoPluginManager? = null

        /**
         * 获取插件管理器实例
         * @param pluginsRoot 插件根目录
         * @return 插件管理器实例
         */
        @JvmStatic
        fun getInstance(pluginsRoot: Path = Paths.get("plugins")): ProtoPluginManager {
            if (instance == null) {
                instance = ProtoPluginManager(pluginsRoot)
            }
            return instance!!
        }
    }
}

/**
 * 使用插件系统
 * @param pluginsRoot 插件根目录
 */
fun ActorSystem.usePlugins(pluginsRoot: Path = Paths.get("plugins")) {
    val pluginManager = ProtoPluginManager.getInstance(pluginsRoot)
    pluginManager.loadPlugins()
    pluginManager.startPlugins()
    pluginManager.initPlugins(this)
}

/**
 * 获取插件管理器
 * @return 插件管理器实例
 */
fun ActorSystem.getPluginManager(): ProtoPluginManager {
    return ProtoPluginManager.getInstance()
}
