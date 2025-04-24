package ai.kastrax.core.plugin

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

/**
 * 连接器插件接口，用于提供与外部系统的集成。
 */
interface ConnectorPlugin : Plugin {
    /**
     * 获取此插件提供的连接器类型列表。
     *
     * @return 连接器类型列表
     */
    fun getConnectorTypes(): List<ConnectorType>
    
    /**
     * 创建连接器实例。
     *
     * @param connectorType 连接器类型
     * @param config 连接器配置
     * @return 连接器实例
     */
    fun createConnector(connectorType: String, config: Map<String, Any?>): Connector?
}

/**
 * 连接器接口，定义了与外部系统交互的方法。
 */
interface Connector {
    /**
     * 连接器的唯一标识符。
     */
    val id: String
    
    /**
     * 连接器的名称。
     */
    val name: String
    
    /**
     * 连接器的描述。
     */
    val description: String
    
    /**
     * 连接器的类型。
     */
    val type: String
    
    /**
     * 连接器的配置。
     */
    val config: Map<String, Any?>
    
    /**
     * 连接器的状态。
     */
    val status: ConnectorStatus
    
    /**
     * 连接到外部系统。
     *
     * @return 连接是否成功
     */
    suspend fun connect(): Boolean
    
    /**
     * 断开与外部系统的连接。
     *
     * @return 断开连接是否成功
     */
    suspend fun disconnect(): Boolean
    
    /**
     * 测试与外部系统的连接。
     *
     * @return 测试结果
     */
    suspend fun testConnection(): ConnectorTestResult
    
    /**
     * 执行操作。
     *
     * @param operation 操作名称
     * @param parameters 操作参数
     * @return 操作结果
     */
    suspend fun execute(operation: String, parameters: Map<String, Any?>): JsonElement
    
    /**
     * 执行流式操作。
     *
     * @param operation 操作名称
     * @param parameters 操作参数
     * @return 操作结果流
     */
    suspend fun executeStream(operation: String, parameters: Map<String, Any?>): Flow<JsonElement>
    
    /**
     * 获取连接器支持的操作列表。
     *
     * @return 操作列表
     */
    fun getOperations(): List<ConnectorOperation>
    
    /**
     * 获取连接器的元数据。
     *
     * @return 连接器元数据
     */
    fun getMetadata(): ConnectorMetadata
}

/**
 * 连接器类型，描述了一种外部系统连接器的类型。
 */
data class ConnectorType(
    /**
     * 连接器类型的唯一标识符。
     */
    val id: String,
    
    /**
     * 连接器类型的名称。
     */
    val name: String,
    
    /**
     * 连接器类型的描述。
     */
    val description: String,
    
    /**
     * 连接器类型的图标（可选）。
     */
    val icon: String? = null,
    
    /**
     * 连接器类型的配置模式，描述了连接器配置的结构。
     */
    val configSchema: Map<String, ConfigField> = emptyMap(),
    
    /**
     * 连接器类型支持的操作列表。
     */
    val operations: List<ConnectorOperation> = emptyList(),
    
    /**
     * 连接器类型的分类。
     */
    val category: String = "其他",
    
    /**
     * 连接器类型的标签。
     */
    val tags: List<String> = emptyList()
)

/**
 * 连接器操作，描述了连接器支持的一种操作。
 */
data class ConnectorOperation(
    /**
     * 操作的唯一标识符。
     */
    val id: String,
    
    /**
     * 操作的名称。
     */
    val name: String,
    
    /**
     * 操作的描述。
     */
    val description: String,
    
    /**
     * 操作的参数模式，描述了操作参数的结构。
     */
    val parameterSchema: Map<String, ConfigField> = emptyMap(),
    
    /**
     * 操作的结果模式，描述了操作结果的结构。
     */
    val resultSchema: Map<String, ConfigField> = emptyMap(),
    
    /**
     * 操作是否支持流式执行。
     */
    val supportsStreaming: Boolean = false
)

/**
 * 连接器状态枚举。
 */
enum class ConnectorStatus {
    /**
     * 连接器已创建但未连接。
     */
    CREATED,
    
    /**
     * 连接器已连接。
     */
    CONNECTED,
    
    /**
     * 连接器已断开连接。
     */
    DISCONNECTED,
    
    /**
     * 连接器发生错误。
     */
    ERROR
}

/**
 * 连接器测试结果。
 */
data class ConnectorTestResult(
    /**
     * 测试是否成功。
     */
    val success: Boolean,
    
    /**
     * 测试消息。
     */
    val message: String,
    
    /**
     * 测试详情。
     */
    val details: Map<String, Any?> = emptyMap()
)

/**
 * 连接器元数据，包含连接器的基本信息。
 */
data class ConnectorMetadata(
    /**
     * 连接器的唯一标识符。
     */
    val id: String,
    
    /**
     * 连接器的名称。
     */
    val name: String,
    
    /**
     * 连接器的描述。
     */
    val description: String,
    
    /**
     * 连接器的类型。
     */
    val type: String,
    
    /**
     * 连接器的状态。
     */
    val status: ConnectorStatus,
    
    /**
     * 连接器的创建时间。
     */
    val createdAt: Long,
    
    /**
     * 连接器的最后连接时间。
     */
    val lastConnectedAt: Long? = null,
    
    /**
     * 连接器的配置（敏感信息已移除）。
     */
    val config: Map<String, Any?>,
    
    /**
     * 连接器支持的操作列表。
     */
    val operations: List<ConnectorOperation>
)

/**
 * 抽象连接器插件基类，提供了ConnectorPlugin接口的基本实现。
 */
abstract class AbstractConnectorPlugin(
    id: String,
    name: String,
    description: String,
    version: String,
    author: String,
    config: PluginConfig = DefaultPluginConfig()
) : AbstractPlugin(
    id = id,
    name = name,
    description = description,
    version = version,
    author = author,
    type = PluginType.CONNECTOR,
    config = config
), ConnectorPlugin
