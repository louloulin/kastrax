package ai.kastrax.datasource.common

/**
 * 所有连接器的基类。
 */
abstract class ConnectorBase {
    /**
     * 连接器的名称。
     */
    abstract val connectorName: String
    
    /**
     * 连接器的类型。
     */
    abstract val connectorType: ConnectorType
}

/**
 * 连接器类型枚举。
 */
enum class ConnectorType {
    API,
    DATABASE,
    FILESYSTEM
}

/**
 * API 连接器基类。
 */
abstract class ApiConnectorBase(
    override val connectorName: String
) : ConnectorBase() {
    override val connectorType: ConnectorType = ConnectorType.API
}

/**
 * REST API 连接器基类。
 */
abstract class RestApiConnectorBase(
    connectorName: String
) : ApiConnectorBase(connectorName)

/**
 * 数据库连接器基类。
 */
abstract class DatabaseConnectorBase(
    override val connectorName: String
) : ConnectorBase() {
    override val connectorType: ConnectorType = ConnectorType.DATABASE
}

/**
 * MySQL 连接器基类。
 */
abstract class MySqlConnectorBase(
    connectorName: String
) : DatabaseConnectorBase(connectorName)

/**
 * 文件系统连接器基类。
 */
abstract class FileSystemConnectorBase(
    override val connectorName: String
) : ConnectorBase() {
    override val connectorType: ConnectorType = ConnectorType.FILESYSTEM
}

/**
 * 本地文件系统连接器基类。
 */
abstract class LocalFileSystemConnectorBase(
    connectorName: String
) : FileSystemConnectorBase(connectorName)
