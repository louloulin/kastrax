package ai.kastrax.datasource.common

/**
 * 文件系统连接器接口，定义了文件系统连接器的通用操作。
 */
interface FileSystemConnector : DataSource {
    /**
     * 读取文件内容。
     *
     * @param path 文件路径。
     * @return 文件内容。
     */
    suspend fun readFile(path: String): ByteArray
    
    /**
     * 读取文本文件内容。
     *
     * @param path 文件路径。
     * @param charset 字符集，默认为 UTF-8。
     * @return 文件内容。
     */
    suspend fun readTextFile(path: String, charset: String = "UTF-8"): String
    
    /**
     * 写入文件内容。
     *
     * @param path 文件路径。
     * @param content 文件内容。
     * @param overwrite 是否覆盖已存在的文件，默认为 false。
     * @return 如果写入成功，则返回 true；否则返回 false。
     */
    suspend fun writeFile(path: String, content: ByteArray, overwrite: Boolean = false): Boolean
    
    /**
     * 写入文本文件内容。
     *
     * @param path 文件路径。
     * @param content 文件内容。
     * @param charset 字符集，默认为 UTF-8。
     * @param overwrite 是否覆盖已存在的文件，默认为 false。
     * @return 如果写入成功，则返回 true；否则返回 false。
     */
    suspend fun writeTextFile(path: String, content: String, charset: String = "UTF-8", overwrite: Boolean = false): Boolean
    
    /**
     * 创建目录。
     *
     * @param path 目录路径。
     * @param createParents 是否创建父目录，默认为 true。
     * @return 如果创建成功，则返回 true；否则返回 false。
     */
    suspend fun createDirectory(path: String, createParents: Boolean = true): Boolean
    
    /**
     * 删除文件或目录。
     *
     * @param path 文件或目录路径。
     * @param recursive 如果是目录，是否递归删除，默认为 false。
     * @return 如果删除成功，则返回 true；否则返回 false。
     */
    suspend fun delete(path: String, recursive: Boolean = false): Boolean
    
    /**
     * 复制文件或目录。
     *
     * @param source 源文件或目录路径。
     * @param destination 目标文件或目录路径。
     * @param overwrite 是否覆盖已存在的文件，默认为 false。
     * @return 如果复制成功，则返回 true；否则返回 false。
     */
    suspend fun copy(source: String, destination: String, overwrite: Boolean = false): Boolean
    
    /**
     * 移动文件或目录。
     *
     * @param source 源文件或目录路径。
     * @param destination 目标文件或目录路径。
     * @param overwrite 是否覆盖已存在的文件，默认为 false。
     * @return 如果移动成功，则返回 true；否则返回 false。
     */
    suspend fun move(source: String, destination: String, overwrite: Boolean = false): Boolean
    
    /**
     * 列出目录内容。
     *
     * @param path 目录路径。
     * @return 目录内容列表。
     */
    suspend fun listDirectory(path: String): List<FileInfo>
    
    /**
     * 检查文件或目录是否存在。
     *
     * @param path 文件或目录路径。
     * @return 如果存在，则返回 true；否则返回 false。
     */
    suspend fun exists(path: String): Boolean
    
    /**
     * 获取文件或目录信息。
     *
     * @param path 文件或目录路径。
     * @return 文件或目录信息。
     */
    suspend fun getInfo(path: String): FileInfo
}

/**
 * 文件信息类，包含文件或目录的基本信息。
 */
data class FileInfo(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val creationTime: Long,
    val lastAccessTime: Long
)
