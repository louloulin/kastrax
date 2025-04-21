package ai.kastrax.datasource.common

import ai.kastrax.core.common.KastraXBase
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 数据源接口，定义了所有数据源的通用操作。
 */
interface DataSource {
    /**
     * 数据源的名称。
     */
    val sourceName: String

    /**
     * 数据源的类型。
     */
    val type: DataSourceType

    /**
     * 连接到数据源。
     *
     * @return 如果连接成功，则返回 true；否则返回 false。
     */
    suspend fun connect(): Boolean

    /**
     * 断开与数据源的连接。
     *
     * @return 如果断开连接成功，则返回 true；否则返回 false。
     */
    suspend fun disconnect(): Boolean

    /**
     * 检查数据源是否已连接。
     *
     * @return 如果数据源已连接，则返回 true；否则返回 false。
     */
    suspend fun isConnected(): Boolean
}

/**
 * 数据源类型枚举。
 */
enum class DataSourceType {
    DATABASE,
    API,
    FILESYSTEM
}

/**
 * 数据源基类，提供了通用的实现。
 */
abstract class DataSourceBase(
    override val sourceName: String,
    override val type: DataSourceType
) : DataSource, KastraXBase(component = "DataSource", name = sourceName) {

    private var connected = false

    override suspend fun connect(): Boolean {
        return try {
            if (!connected) {
                connected = doConnect()
                if (connected) {
                    logger.info { "Connected to data source: $sourceName" }
                } else {
                    logger.error { "Failed to connect to data source: $sourceName" }
                }
            }
            connected
        } catch (e: Exception) {
            logger.error(e) { "Error connecting to data source: $sourceName" }
            false
        }
    }

    override suspend fun disconnect(): Boolean {
        return try {
            if (connected) {
                connected = !doDisconnect()
                if (!connected) {
                    logger.info { "Disconnected from data source: $sourceName" }
                } else {
                    logger.error { "Failed to disconnect from data source: $sourceName" }
                }
            }
            !connected
        } catch (e: Exception) {
            logger.error(e) { "Error disconnecting from data source: $sourceName" }
            false
        }
    }

    override suspend fun isConnected(): Boolean {
        return connected
    }

    /**
     * 执行实际的连接操作。
     *
     * @return 如果连接成功，则返回 true；否则返回 false。
     */
    protected abstract suspend fun doConnect(): Boolean

    /**
     * 执行实际的断开连接操作。
     *
     * @return 如果断开连接成功，则返回 true；否则返回 false。
     */
    protected abstract suspend fun doDisconnect(): Boolean
}
