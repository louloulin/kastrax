package ai.kastrax.datasource

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.datasource.common.DataSource
import ai.kastrax.datasource.common.DataSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 数据源管理器，用于管理所有数据源。
 */
class DataSourceManager : KastraXBase(component = "DataSourceManager", name = "DataSourceManager") {

    private val dataSources = ConcurrentHashMap<String, DataSource>()

    /**
     * 注册数据源。
     *
     * @param dataSource 数据源。
     * @return 如果注册成功，则返回 true；否则返回 false。
     */
    fun registerDataSource(dataSource: DataSource): Boolean {
        val name = dataSource.sourceName

        if (dataSources.containsKey(name)) {
            logger.warn { "Data source with name '$name' already exists" }
            return false
        }

        dataSources[name] = dataSource
        logger.info { "Registered data source: $name (${dataSource.type})" }
        return true
    }

    /**
     * 注销数据源。
     *
     * @param name 数据源名称。
     * @return 如果注销成功，则返回 true；否则返回 false。
     */
    suspend fun unregisterDataSource(name: String): Boolean {
        val dataSource = dataSources[name] ?: return false

        if (dataSource.isConnected()) {
            dataSource.disconnect()
        }

        dataSources.remove(name)
        logger.info { "Unregistered data source: $name" }
        return true
    }

    /**
     * 获取数据源。
     *
     * @param name 数据源名称。
     * @return 数据源，如果不存在则返回 null。
     */
    fun getDataSource(name: String): DataSource? {
        return dataSources[name]
    }

    /**
     * 获取所有数据源。
     *
     * @return 所有数据源的映射，键是数据源名称，值是数据源。
     */
    fun getAllDataSources(): Map<String, DataSource> {
        return dataSources.toMap()
    }

    /**
     * 获取指定类型的所有数据源。
     *
     * @param type 数据源类型。
     * @return 指定类型的所有数据源的映射，键是数据源名称，值是数据源。
     */
    fun getDataSourcesByType(type: DataSourceType): Map<String, DataSource> {
        return dataSources.filterValues { it.type == type }
    }

    /**
     * 连接所有数据源。
     *
     * @return 连接成功的数据源数量。
     */
    suspend fun connectAll(): Int {
        var successCount = 0

        withContext(Dispatchers.IO) {
            dataSources.values.forEach { dataSource ->
                try {
                    if (dataSource.connect()) {
                        successCount++
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error connecting to data source: ${dataSource.sourceName}" }
                }
            }
        }

        logger.info { "Connected to $successCount/${dataSources.size} data sources" }
        return successCount
    }

    /**
     * 断开所有数据源的连接。
     *
     * @return 断开连接成功的数据源数量。
     */
    suspend fun disconnectAll(): Int {
        var successCount = 0

        withContext(Dispatchers.IO) {
            dataSources.values.forEach { dataSource ->
                try {
                    if (dataSource.disconnect()) {
                        successCount++
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error disconnecting from data source: ${dataSource.sourceName}" }
                }
            }
        }

        logger.info { "Disconnected from $successCount/${dataSources.size} data sources" }
        return successCount
    }

    companion object {
        private val instance = DataSourceManager()

        /**
         * 获取数据源管理器实例。
         *
         * @return 数据源管理器实例。
         */
        fun getInstance(): DataSourceManager {
            return instance
        }
    }
}
