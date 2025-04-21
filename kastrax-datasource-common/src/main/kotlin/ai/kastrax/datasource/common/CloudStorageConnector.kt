package ai.kastrax.datasource.common

/**
 * 云存储连接器接口，定义了云存储连接器的通用操作。
 */
interface CloudStorageConnector : FileSystemConnector {
    /**
     * 获取对象的公共 URL。
     *
     * @param path 对象路径。
     * @param expirationSeconds URL 的有效期（秒），默认为 3600 秒（1 小时）。
     * @return 公共 URL。
     */
    suspend fun getPublicUrl(path: String, expirationSeconds: Int = 3600): String
    
    /**
     * 获取对象的元数据。
     *
     * @param path 对象路径。
     * @return 对象元数据，包含内容类型、大小、最后修改时间等信息。
     */
    suspend fun getObjectMetadata(path: String): Map<String, String>
    
    /**
     * 设置对象的元数据。
     *
     * @param path 对象路径。
     * @param metadata 对象元数据。
     * @return 如果设置成功，则返回 true；否则返回 false。
     */
    suspend fun setObjectMetadata(path: String, metadata: Map<String, String>): Boolean
    
    /**
     * 获取存储桶名称。
     *
     * @return 存储桶名称。
     */
    fun getBucketName(): String
    
    /**
     * 获取区域。
     *
     * @return 区域。
     */
    fun getRegion(): String
}
