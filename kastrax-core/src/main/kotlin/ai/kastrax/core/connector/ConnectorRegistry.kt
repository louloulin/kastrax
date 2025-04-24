package ai.kastrax.core.connector

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.plugin.Connector
import ai.kastrax.core.plugin.ConnectorPlugin
import ai.kastrax.core.plugin.ConnectorType
import java.util.concurrent.ConcurrentHashMap

/**
 * 连接器注册表，用于管理外部系统连接器。
 */
class ConnectorRegistry : KastraXBase(component = "CONNECTOR_REGISTRY", name = "ConnectorRegistry") {
    private val connectorTypes = ConcurrentHashMap<String, ConnectorType>()
    private val connectorPlugins = ConcurrentHashMap<String, ConnectorPlugin>()
    private val connectors = ConcurrentHashMap<String, Connector>()
    
    /**
     * 注册连接器类型。
     *
     * @param connectorType 连接器类型
     * @param plugin 提供此连接器类型的插件
     * @return 注册是否成功
     */
    fun registerConnectorType(connectorType: ConnectorType, plugin: ConnectorPlugin): Boolean {
        if (connectorTypes.containsKey(connectorType.id)) {
            logger.warn { "连接器类型已存在: ${connectorType.id}" }
            return false
        }
        
        connectorTypes[connectorType.id] = connectorType
        connectorPlugins[connectorType.id] = plugin
        
        logger.info { "注册连接器类型: ${connectorType.name} (${connectorType.id})" }
        return true
    }
    
    /**
     * 注销连接器类型。
     *
     * @param connectorTypeId 连接器类型ID
     * @return 注销是否成功
     */
    fun unregisterConnectorType(connectorTypeId: String): Boolean {
        val connectorType = connectorTypes.remove(connectorTypeId)
        connectorPlugins.remove(connectorTypeId)
        
        if (connectorType != null) {
            logger.info { "注销连接器类型: ${connectorType.name} (${connectorType.id})" }
            return true
        }
        
        return false
    }
    
    /**
     * 获取所有连接器类型。
     *
     * @return 所有连接器类型
     */
    fun getAllConnectorTypes(): List<ConnectorType> {
        return connectorTypes.values.toList()
    }
    
    /**
     * 获取连接器类型。
     *
     * @param connectorTypeId 连接器类型ID
     * @return 连接器类型，如果不存在则返回null
     */
    fun getConnectorType(connectorTypeId: String): ConnectorType? {
        return connectorTypes[connectorTypeId]
    }
    
    /**
     * 获取指定分类的连接器类型。
     *
     * @param category 分类
     * @return 指定分类的连接器类型
     */
    fun getConnectorTypesByCategory(category: String): List<ConnectorType> {
        return connectorTypes.values.filter { it.category == category }
    }
    
    /**
     * 获取具有指定标签的连接器类型。
     *
     * @param tag 标签
     * @return 具有指定标签的连接器类型
     */
    fun getConnectorTypesByTag(tag: String): List<ConnectorType> {
        return connectorTypes.values.filter { it.tags.contains(tag) }
    }
    
    /**
     * 创建连接器实例。
     *
     * @param connectorTypeId 连接器类型ID
     * @param config 连接器配置
     * @return 连接器实例，如果连接器类型不存在则返回null
     */
    fun createConnector(connectorTypeId: String, config: Map<String, Any?>): Connector? {
        val plugin = connectorPlugins[connectorTypeId] ?: return null
        return plugin.createConnector(connectorTypeId, config)
    }
    
    /**
     * 注册连接器实例。
     *
     * @param connector 连接器实例
     * @return 注册是否成功
     */
    fun registerConnector(connector: Connector): Boolean {
        if (connectors.containsKey(connector.id)) {
            logger.warn { "连接器已存在: ${connector.id}" }
            return false
        }
        
        connectors[connector.id] = connector
        
        logger.info { "注册连接器: ${connector.name} (${connector.id})" }
        return true
    }
    
    /**
     * 注销连接器实例。
     *
     * @param connectorId 连接器ID
     * @return 注销是否成功
     */
    fun unregisterConnector(connectorId: String): Boolean {
        val connector = connectors.remove(connectorId)
        
        if (connector != null) {
            logger.info { "注销连接器: ${connector.name} (${connector.id})" }
            return true
        }
        
        return false
    }
    
    /**
     * 获取所有连接器实例。
     *
     * @return 所有连接器实例
     */
    fun getAllConnectors(): List<Connector> {
        return connectors.values.toList()
    }
    
    /**
     * 获取连接器实例。
     *
     * @param connectorId 连接器ID
     * @return 连接器实例，如果不存在则返回null
     */
    fun getConnector(connectorId: String): Connector? {
        return connectors[connectorId]
    }
    
    /**
     * 获取指定类型的连接器实例。
     *
     * @param connectorTypeId 连接器类型ID
     * @return 指定类型的连接器实例
     */
    fun getConnectorsByType(connectorTypeId: String): List<Connector> {
        return connectors.values.filter { it.type == connectorTypeId }
    }
    
    /**
     * 连接所有连接器。
     *
     * @return 连接成功的连接器数量
     */
    suspend fun connectAll(): Int {
        var count = 0
        
        for (connector in connectors.values) {
            try {
                if (connector.connect()) {
                    count++
                }
            } catch (e: Exception) {
                logger.error(e) { "连接连接器异常: ${connector.name} (${connector.id})" }
            }
        }
        
        logger.info { "连接所有连接器: 成功 $count 个，总共 ${connectors.size} 个" }
        return count
    }
    
    /**
     * 断开所有连接器。
     *
     * @return 断开连接成功的连接器数量
     */
    suspend fun disconnectAll(): Int {
        var count = 0
        
        for (connector in connectors.values) {
            try {
                if (connector.disconnect()) {
                    count++
                }
            } catch (e: Exception) {
                logger.error(e) { "断开连接器异常: ${connector.name} (${connector.id})" }
            }
        }
        
        logger.info { "断开所有连接器: 成功 $count 个，总共 ${connectors.size} 个" }
        return count
    }
}
