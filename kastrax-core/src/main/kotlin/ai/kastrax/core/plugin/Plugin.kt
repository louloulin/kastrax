package ai.kastrax.core.plugin

// import ai.kastrax.core.common.KastraXBase
import kotlinx.serialization.Serializable
import mu.KotlinLogging

/**
 * 插件接口，定义了KastraX插件系统的基本功能。
 * 所有插件都必须实现此接口。
 */
interface Plugin {
    /**
     * 插件的唯一标识符。
     */
    val id: String

    /**
     * 插件的名称。
     */
    val name: String

    /**
     * 插件的描述。
     */
    val description: String

    /**
     * 插件的版本。
     */
    val version: String

    /**
     * 插件的作者。
     */
    val author: String

    /**
     * 插件的类型。
     */
    val type: PluginType

    /**
     * 插件的配置。
     */
    val config: PluginConfig

    /**
     * 初始化插件。
     * 在插件加载时调用。
     *
     * @param context 插件上下文
     * @return 初始化是否成功
     */
    fun initialize(context: PluginContext): Boolean

    /**
     * 启动插件。
     * 在插件初始化成功后调用。
     *
     * @param context 插件上下文
     * @return 启动是否成功
     */
    fun start(context: PluginContext): Boolean

    /**
     * 停止插件。
     * 在系统关闭或插件被禁用时调用。
     *
     * @param context 插件上下文
     * @return 停止是否成功
     */
    fun stop(context: PluginContext): Boolean

    /**
     * 获取插件的元数据。
     *
     * @return 插件元数据
     */
    fun getMetadata(): PluginMetadata {
        return PluginMetadata(
            id = id,
            name = name,
            description = description,
            version = version,
            author = author,
            type = type
        )
    }
}

/**
 * 插件类型枚举。
 */
enum class PluginType {
    /**
     * 工作流步骤插件。
     */
    WORKFLOW_STEP,

    /**
     * 工具插件。
     */
    TOOL,

    /**
     * 连接器插件。
     */
    CONNECTOR,

    /**
     * 存储插件。
     */
    STORAGE,

    /**
     * UI扩展插件。
     */
    UI_EXTENSION,

    /**
     * 其他类型插件。
     */
    OTHER
}

/**
 * 插件配置接口。
 */
interface PluginConfig {
    /**
     * 获取配置属性。
     *
     * @param key 属性键
     * @return 属性值，如果不存在则返回null
     */
    fun getProperty(key: String): Any?

    /**
     * 设置配置属性。
     *
     * @param key 属性键
     * @param value 属性值
     */
    fun setProperty(key: String, value: Any?)

    /**
     * 获取所有配置属性。
     *
     * @return 所有配置属性的映射
     */
    fun getAllProperties(): Map<String, Any?>
}

/**
 * 默认插件配置实现。
 */
class DefaultPluginConfig : PluginConfig {
    private val properties = mutableMapOf<String, Any?>()

    override fun getProperty(key: String): Any? {
        return properties[key]
    }

    override fun setProperty(key: String, value: Any?) {
        properties[key] = value
    }

    override fun getAllProperties(): Map<String, Any?> {
        return properties.toMap()
    }
}

/**
 * 插件上下文，提供插件与系统交互的接口。
 */
interface PluginContext {
    /**
     * 获取插件管理器。
     *
     * @return 插件管理器
     */
    fun getPluginManager(): PluginManager

    /**
     * 获取插件的配置。
     *
     * @param pluginId 插件ID
     * @return 插件配置
     */
    fun getPluginConfig(pluginId: String): PluginConfig?

    /**
     * 获取系统服务。
     *
     * @param serviceClass 服务类
     * @return 服务实例
     */
    fun <T : Any> getService(serviceClass: Class<T>): T?

    /**
     * 注册服务。
     *
     * @param serviceClass 服务类
     * @param service 服务实例
     */
    fun <T : Any> registerService(serviceClass: Class<T>, service: T)
}

/**
 * 插件元数据，包含插件的基本信息。
 */
@Serializable
data class PluginMetadata(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val type: PluginType
)

/**
 * 插件状态枚举。
 */
enum class PluginStatus {
    /**
     * 插件已注册但未初始化。
     */
    REGISTERED,

    /**
     * 插件已初始化但未启动。
     */
    INITIALIZED,

    /**
     * 插件已启动并正在运行。
     */
    RUNNING,

    /**
     * 插件已停止。
     */
    STOPPED,

    /**
     * 插件发生错误。
     */
    ERROR
}

/**
 * 插件信息，包含插件的元数据和状态。
 */
data class PluginInfo(
    val metadata: PluginMetadata,
    var status: PluginStatus,
    var error: String? = null
)

/**
 * 抽象插件基类，提供了插件接口的基本实现。
 */
abstract class AbstractPlugin(
    override val id: String,
    override val name: String,
    override val description: String,
    override val version: String,
    override val author: String,
    override val type: PluginType,
    override val config: PluginConfig = DefaultPluginConfig()
) : Plugin {

    protected val logger = KotlinLogging.logger("PLUGIN:$id")

    override fun initialize(context: PluginContext): Boolean {
        logger.info { "初始化插件: $name ($id)" }
        return true
    }

    override fun start(context: PluginContext): Boolean {
        logger.info { "启动插件: $name ($id)" }
        return true
    }

    override fun stop(context: PluginContext): Boolean {
        logger.info { "停止插件: $name ($id)" }
        return true
    }
}
