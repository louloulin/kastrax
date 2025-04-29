package actor.proto

/**
 * 测试用的ActorSystem类
 */
class ActorSystem private constructor(val name: String) {
    val root = RootContext()

    // 存储所有中间件
    private val receiveMiddlewares = mutableListOf<ReceiveMiddleware>()

    companion object {
        /**
         * 创建ActorSystem实例
         * @return ActorSystem实例
         */
        fun create(): ActorSystem {
            return ActorSystem("test")
        }
    }

    /**
     * 注册插件
     * @param plugin 插件实例
     */
    fun registerPlugin(plugin: Any) {
        // 获取插件ID
        try {
            val idMethod = plugin.javaClass.getMethod("id")
            val id = idMethod.invoke(plugin) as String

            // 存储插件
            plugins[id] = plugin

            // 调用插件的init方法
            val initMethod = plugin.javaClass.getMethod("init", ActorSystem::class.java)
            initMethod.invoke(plugin, this)

            // 检查插件是否实现了ReceiveMiddlewarePluginInterface接口
            if (plugin.javaClass.interfaces.any { it.name.endsWith("ReceiveMiddlewarePluginInterface") }) {
                // 获取中间件
                val getMiddlewareMethod = plugin.javaClass.getMethod("receiveMiddleware")
                val middleware = getMiddlewareMethod.invoke(plugin) as ReceiveMiddleware

                // 添加中间件
                receiveMiddlewares.add(middleware)

                // 将中间件添加到RootContext
                root.addMiddleware(middleware)

                println("Added receive middleware from plugin: ${plugin.javaClass.simpleName}")
            }

            println("Plugin registered and initialized: ${plugin.javaClass.simpleName} (id: $id)")
        } catch (e: Exception) {
            println("Failed to initialize plugin: ${e.message}")
            e.printStackTrace()
        }
    }

    // 存储所有插件
    private val plugins = mutableMapOf<String, Any>()

    /**
     * 获取插件
     * @param id 插件ID
     * @return 插件实例
     */
    fun getPlugin(id: String): Any? {
        return plugins[id]
    }

    /**
     * 关闭ActorSystem
     */
    fun shutdown() {
        // 测试用的空实现
    }

    /**
     * 发送消息
     * @param pid 目标PID
     * @param message 消息
     */
    fun send(pid: TestPID, message: Any) {
        // 测试用的空实现
    }

    /**
     * 异步请求
     * @param pid 目标PID
     * @param message 消息
     * @param timeout 超时时间
     * @return 响应
     */
    fun <T> requestAsync(pid: TestPID, message: Any, timeout: java.time.Duration): T {
        // 测试用的空实现
        @Suppress("UNCHECKED_CAST")
        return "Response: $message" as T
    }

    /**
     * 创建Actor
     * @param props Actor属性
     * @return Actor的PID
     */
    fun actorOf(props: Props): TestPID {
        // 测试用的空实现
        return createPID("test", "test")
    }
}
