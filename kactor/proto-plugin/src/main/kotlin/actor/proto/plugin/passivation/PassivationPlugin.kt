package actor.proto.plugin.passivation

import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.Receive
import actor.proto.ReceiveMiddleware
import actor.proto.plugin.ProtoPlugin
import actor.proto.plugin.PropsDecoratorExtension
import actor.proto.plugin.ReceiveMiddlewareExtension
import actor.proto.plugin.PluginInterface
import actor.proto.plugin.ReceiveMiddlewarePluginInterface
import actor.proto.plugin.PropsDecoratorPluginInterface
import org.pf4j.Extension
import org.pf4j.PluginWrapper
import java.time.Duration
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap

/**
 * 被动化消息，用于通知Actor停止活动
 */
object Passivate

/**
 * 被动化插件
 * 用于在Actor空闲一段时间后自动停止
 */
class PassivationPlugin(wrapper: PluginWrapper) : ProtoPlugin(wrapper) {
    lateinit var passivationManager: PassivationManager

    override fun start() {
        passivationManager = PassivationManager()
        log.info("PassivationPlugin started")
    }

    override fun stop() {
        passivationManager.shutdown()
        log.info("PassivationPlugin stopped")
    }

    override fun init(system: ActorSystem) {
        passivationManager.init(system)
        log.info("PassivationPlugin initialized with ActorSystem")
    }

    /**
     * 被动化管理器
     * 负责跟踪Actor的最后活动时间并触发被动化
     */
    class PassivationManager {
        private val lastUsedTimes = ConcurrentHashMap<Any, Long>()
        private var timer: Timer? = null
        private var system: ActorSystem? = null
        private var duration: Duration = Duration.ofMinutes(5) // 默认5分钟

        /**
         * 初始化
         * @param actorSystem Actor系统
         * @param timeout 超时时间
         */
        fun init(actorSystem: ActorSystem, timeout: Duration = Duration.ofMinutes(5)) {
            system = actorSystem
            duration = timeout

            timer = Timer("PassivationTimer", true)
            timer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    checkForPassivation()
                }
            }, duration.toMillis(), duration.toMillis())
        }

        /**
         * 更新Actor的最后活动时间
         * @param pid Actor的PID
         */
        fun updateLastUsed(pid: Any) {
            lastUsedTimes[pid] = System.currentTimeMillis()
        }

        /**
         * 移除Actor的记录
         * @param pid Actor的PID
         */
        fun remove(pid: Any) {
            lastUsedTimes.remove(pid)
        }

        /**
         * 检查需要被动化的Actor
         */
        private fun checkForPassivation() {
            val now = System.currentTimeMillis()
            val timeout = duration.toMillis()
            val sys = system ?: return

            lastUsedTimes.forEach { (pid, lastUsed) ->
                if (now - lastUsed > timeout) {
                    // 使用反射调用send方法
                    val rootField = sys.javaClass.getDeclaredField("root")
                    rootField.isAccessible = true
                    val root = rootField.get(sys)
                    val sendMethod = root.javaClass.getMethod("send", Any::class.java, Any::class.java)
                    sendMethod.invoke(root, pid, Passivate)
                }
            }
        }

        /**
         * 关闭
         */
        fun shutdown() {
            timer?.cancel()
            timer = null
            lastUsedTimes.clear()
            system = null
        }
    }

    /**
     * 被动化接收中间件扩展
     */
    @Extension
    class PassivationReceiveMiddleware : ReceiveMiddlewareExtension {
        override fun id(): String = "passivation-receive-middleware"

        override fun version(): String = "1.0.0"

        override fun init(system: ActorSystem) {
            // 默认实现为空
        }

        override fun shutdown() {
            // 默认实现为空
        }
        override fun getReceiveMiddleware(): ReceiveMiddleware = { next ->
            { ctx ->
                // 获取插件管理器
                val plugin = getPlugin(ctx, PassivationPlugin::class.java)
                val manager = (plugin as? PassivationPlugin)?.passivationManager

                // 更新最后使用时间
                manager?.updateLastUsed(ctx.self)

                // 处理Passivate消息
                if (ctx.message == Passivate) {
                    // 使用反射调用stop方法
                    val stopMethod = ctx.javaClass.getMethod("stop", ctx.self.javaClass)
                    stopMethod.invoke(ctx, ctx.self)
                } else {
                    next(ctx)
                }
            }
        }
    }

    /**
     * 被动化Props装饰器扩展
     */
    @Extension
    class PassivationPropsDecorator : PropsDecoratorExtension {
        override fun id(): String = "passivation-props-decorator"

        override fun version(): String = "1.0.0"

        override fun init(system: ActorSystem) {
            // 默认实现为空
        }

        override fun shutdown() {
            // 默认实现为空
        }
        override fun decorateProps(props: Props): Props {
            // 添加停止钩子，在Actor停止时清理记录
            // 使用反射调用withOnStop方法
            val withOnStopMethod = props.javaClass.getMethod("withOnStop", Function1::class.java)

            // 创建回调函数
            val callback = object : Function1<Context, Unit> {
                override fun invoke(ctx: Context) {
                    // 获取插件管理器
                    val plugin = getPlugin(ctx, PassivationPlugin::class.java)
                    val manager = (plugin as? PassivationPlugin)?.passivationManager

                    // 移除记录
                    manager?.remove(ctx.self)
                }
            }

            return withOnStopMethod.invoke(props, callback) as Props
        }
    }
}

/**
 * 获取插件
 * @param pluginClass 插件类
 * @return 插件实例
 */
fun ActorSystem.getPlugin(id: String): Any? {
    return this.getPlugin(id)
}

fun ActorSystem.getPlugin(pluginClass: Class<*>): Any? {
    val plugins = this.javaClass.getDeclaredField("plugins")
    plugins.isAccessible = true
    val pluginsMap = plugins.get(this) as Map<String, Any>
    return pluginsMap.values.firstOrNull { pluginClass.isInstance(it) }
}

fun getPlugin(ctx: Context, pluginClass: Class<*>): Any? {
    val systemField = ctx.javaClass.getDeclaredField("system")
    systemField.isAccessible = true
    val system = systemField.get(ctx) as ActorSystem
    return system.getPlugin(pluginClass)
}

/**
 * 配置被动化超时
 * @param timeout 超时时间
 */
fun ActorSystem.configurePassivation(timeout: Duration) {
    val plugin = getPlugin(PassivationPlugin::class.java) as? PassivationPlugin
    plugin?.passivationManager?.init(this, timeout)
}
