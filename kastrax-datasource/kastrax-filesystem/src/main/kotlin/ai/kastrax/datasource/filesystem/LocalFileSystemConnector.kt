package ai.kastrax.datasource.filesystem

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 本地文件系统连接器，提供了与本地文件系统交互的功能。
 */
class LocalFileSystemConnector(
    name: String,
    override val rootPath: String
) : FileSystemConnectorBase(name) {
    
    private var isConnected = false
    
    override suspend fun doConnect(): Boolean {
        return try {
            val root = Paths.get(rootPath)
            
            if (!Files.exists(root)) {
                Files.createDirectories(root)
            }
            
            if (!Files.isDirectory(root)) {
                throw IllegalArgumentException("Root path is not a directory: $rootPath")
            }
            
            if (!Files.isReadable(root) || !Files.isWritable(root)) {
                throw IllegalArgumentException("Root path is not readable or writable: $rootPath")
            }
            
            isConnected = true
            true
        } catch (e: Exception) {
            logger.error(e) { "Error connecting to local file system: $rootPath" }
            false
        }
    }
    
    override suspend fun doDisconnect(): Boolean {
        isConnected = false
        return true
    }
}
