package actor.proto.plugin.polyglot

import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Receive
import actor.proto.ReceiveMiddleware
import actor.proto.Send
import actor.proto.SenderMiddleware
import actor.proto.plugin.ProtoPlugin
import actor.proto.plugin.ReceiveMiddlewareExtension
import actor.proto.plugin.SenderMiddlewareExtension
import actor.proto.plugin.PluginInterface
import actor.proto.plugin.ReceiveMiddlewarePluginInterface
import actor.proto.plugin.SenderMiddlewarePluginInterface
import org.graalvm.polyglot.Context as GraalContext
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.pf4j.Extension
import org.pf4j.PluginWrapper
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * GraalVM多语言插件
 * 支持使用JavaScript、Python等语言编写插件
 */
class GraalPlugin(wrapper: PluginWrapper) : ProtoPlugin(wrapper) {
    private var graalContext: GraalContext? = null
    private val scriptEngines = mutableMapOf<String, ScriptEngine>()

    override fun start() {
        initGraalContext()
        log.info("GraalPlugin started")
    }

    override fun stop() {
        graalContext?.close()
        graalContext = null
        scriptEngines.clear()
        log.info("GraalPlugin stopped")
    }

    override fun init(system: ActorSystem) {
        log.info("GraalPlugin initialized with ActorSystem")
    }

    /**
     * 初始化GraalVM上下文
     */
    private fun initGraalContext() {
        try {
            graalContext = GraalContext.newBuilder()
                .allowAllAccess(true)
                .build()
        } catch (e: Exception) {
            log.error("Failed to initialize GraalVM context", e)
        }
    }

    /**
     * 加载脚本
     * @param language 语言ID
     * @param scriptPath 脚本路径
     * @return 脚本引擎
     */
    fun loadScript(language: String, scriptPath: String): ScriptEngine? {
        val context = graalContext ?: return null

        try {
            val scriptFile = File(scriptPath)
            if (!scriptFile.exists()) {
                log.error("Script file not found: $scriptPath")
                return null
            }

            val source = Source.newBuilder(language, scriptFile).build()
            context.eval(source)

            val engine = ScriptEngine(language, scriptPath, context)
            scriptEngines["$language:$scriptPath"] = engine
            return engine
        } catch (e: Exception) {
            log.error("Failed to load script: $scriptPath", e)
            return null
        }
    }

    /**
     * 获取脚本引擎
     * @param language 语言ID
     * @param scriptPath 脚本路径
     * @return 脚本引擎
     */
    fun getScriptEngine(language: String, scriptPath: String): ScriptEngine? {
        return scriptEngines["$language:$scriptPath"]
    }

    /**
     * 脚本引擎
     * 封装GraalVM脚本执行环境
     * @param language 语言ID
     * @param scriptPath 脚本路径
     * @param context GraalVM上下文
     */
    class ScriptEngine(
        val language: String,
        val scriptPath: String,
        private val context: GraalContext
    ) {
        /**
         * 调用脚本中的函数
         * @param functionName 函数名
         * @param args 参数
         * @return 函数返回值
         */
        fun callFunction(functionName: String, vararg args: Any): Value? {
            try {
                val bindings = context.getBindings(language)
                val function = bindings.getMember(functionName)

                if (function == null || !function.canExecute()) {
                    return null
                }

                return function.execute(*args)
            } catch (e: Exception) {
                return null
            }
        }

        /**
         * 设置全局变量
         * @param name 变量名
         * @param value 变量值
         */
        fun setGlobal(name: String, value: Any) {
            context.getBindings(language).putMember(name, value)
        }
    }

    /**
     * JavaScript接收中间件扩展
     */
    @Extension
    class JavaScriptReceiveMiddleware : ReceiveMiddlewareExtension {
        override fun id(): String = "javascript-receive-middleware"

        override fun version(): String = "1.0.0"

        override fun init(system: ActorSystem) {
            // 默认实现为空
        }

        override fun shutdown() {
            // 默认实现为空
        }
        override fun getReceiveMiddleware(): ReceiveMiddleware = { next ->
            { ctx ->
                // 获取插件
                val systemField = ctx.javaClass.getDeclaredField("system")
                systemField.isAccessible = true
                val system = systemField.get(ctx) as ActorSystem
                val plugin = system.getPlugin("graal") as? GraalPlugin

                // 获取所有JavaScript脚本引擎
                val engines = plugin?.scriptEngines?.filterKeys { it.startsWith("js:") }?.values

                // 调用所有脚本的receiveMiddleware函数
                var shouldContinue = true
                engines?.forEach { engine ->
                    val result = engine.callFunction("receiveMiddleware", ctx)
                    if (result?.isBoolean == true && !result.asBoolean()) {
                        shouldContinue = false
                    }
                }

                if (shouldContinue) {
                    next(ctx)
                }
            }
        }
    }

    /**
     * JavaScript发送中间件扩展
     */
    @Extension
    class JavaScriptSenderMiddleware : SenderMiddlewareExtension {
        override fun id(): String = "javascript-sender-middleware"

        override fun version(): String = "1.0.0"

        override fun init(system: ActorSystem) {
            // 默认实现为空
        }

        override fun shutdown() {
            // 默认实现为空
        }
        override fun getSenderMiddleware(): SenderMiddleware = { next ->
            { ctx, pid, envelope ->
                // 获取插件
                val systemField = ctx.javaClass.getDeclaredField("system")
                systemField.isAccessible = true
                val system = systemField.get(ctx) as ActorSystem
                val plugin = system.getPlugin("graal") as? GraalPlugin

                // 获取所有JavaScript脚本引擎
                val engines = plugin?.scriptEngines?.filterKeys { it.startsWith("js:") }?.values

                // 调用所有脚本的senderMiddleware函数
                var shouldContinue = true
                engines?.forEach { engine ->
                    val result = engine.callFunction("senderMiddleware", ctx, pid, envelope)
                    if (result?.isBoolean == true && !result.asBoolean()) {
                        shouldContinue = false
                    }
                }

                if (shouldContinue) {
                    next(ctx, pid, envelope)
                }
            }
        }
    }

    /**
     * Python接收中间件扩展
     */
    @Extension
    class PythonReceiveMiddleware : ReceiveMiddlewareExtension {
        override fun id(): String = "python-receive-middleware"

        override fun version(): String = "1.0.0"

        override fun init(system: ActorSystem) {
            // 默认实现为空
        }

        override fun shutdown() {
            // 默认实现为空
        }
        override fun getReceiveMiddleware(): ReceiveMiddleware = { next ->
            { ctx ->
                // 获取插件
                val systemField = ctx.javaClass.getDeclaredField("system")
                systemField.isAccessible = true
                val system = systemField.get(ctx) as ActorSystem
                val plugin = system.getPlugin("graal") as? GraalPlugin

                // 获取所有Python脚本引擎
                val engines = plugin?.scriptEngines?.filterKeys { it.startsWith("python:") }?.values

                // 调用所有脚本的receive_middleware函数
                var shouldContinue = true
                engines?.forEach { engine ->
                    val result = engine.callFunction("receive_middleware", ctx)
                    if (result?.isBoolean == true && !result.asBoolean()) {
                        shouldContinue = false
                    }
                }

                if (shouldContinue) {
                    next(ctx)
                }
            }
        }
    }
}

/**
 * 加载JavaScript插件
 * @param scriptPath 脚本路径
 */
fun ActorSystem.loadJavaScriptPlugin(scriptPath: String) {
    val plugin = getPlugin("graal") as? GraalPlugin
    plugin?.loadScript("js", scriptPath)
}

/**
 * 加载Python插件
 * @param scriptPath 脚本路径
 */
fun ActorSystem.loadPythonPlugin(scriptPath: String) {
    val plugin = getPlugin("graal") as? GraalPlugin
    plugin?.loadScript("python", scriptPath)
}

/**
 * 从目录加载所有脚本插件
 * @param directory 插件目录
 */
fun ActorSystem.loadScriptPlugins(directory: Path) {
    val plugin = getPlugin("graal") as? GraalPlugin ?: return

    // 加载JavaScript插件
    File(directory.toString()).walk()
        .filter { it.isFile && it.extension == "js" }
        .forEach { file ->
            plugin.loadScript("js", file.absolutePath)
        }

    // 加载Python插件
    File(directory.toString()).walk()
        .filter { it.isFile && it.extension == "py" }
        .forEach { file ->
            plugin.loadScript("python", file.absolutePath)
        }
}
